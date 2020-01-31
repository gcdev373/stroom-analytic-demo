from pyspark.sql.session import SparkSession
from pyspark.sql.types import *
from pyspark.sql.functions import *
from pyspark.ml.feature import OneHotEncoderEstimator,VectorAssembler,StringIndexer
from pyspark.ml.classification import LogisticRegression,LogisticRegressionModel
from pyspark.ml import Pipeline,PipelineModel
from pathlib import Path

import datetime
import json

outputdir = "../../tmp/alerts"

Path(outputdir).mkdir(parents=True, exist_ok=True)

spark = SparkSession \
    .builder \
    .appName("DetectUnexpectedAuthFailRate") \
    .getOrCreate()

spark.sparkContext.setLogLevel('WARN')

with open('../../state-monitor/src/test/resources/event-logging-v3.2.3.json', "r") as schemaFile:
    schemaString = schemaFile.read()

json_schema = StructType.fromJson(json.loads(schemaString))

json_schema
#includeHeaders isn't supported until Spark Version > 3.0

df = spark \
    .readStream \
    .format("kafka") \
    .option("kafka.bootstrap.servers", "localhost:9092") \
    .option("subscribe", "ANALYTIC-DEMO-UEBA") \
    .option("startingoffsets", "latest") \
    .option("includeHeaders", "true") \
    .load()

# df = spark.read.format('stroom.spark.datasource.StroomDataSource').load(
#         token=os.environ['STROOM_API_KEY'],host='localhost',protocol='http',
#         uri='api/stroom-index/v2',traceLevel="0",
#         index='5b41ebbf-b53e-41e6-a4e5-e5a220d8fd69',pipeline='13143179-b494-4146-ac4b-9a6010cada89',
#         maxResults='300000').filter((col('idxEventTime') > '2019-12-01T00:00:00.000Z')
#             & (col('idxEventTime') < '2020-01-01T00:00:00.000Z')
#             & (col('idxDescription') == 'Authentication Failure'))

def timestamp_now (dummy):
    return datetime.datetime.utcnow().isoformat()[:-3]+'Z'
now_udf = udf(timestamp_now, StringType())

def event_ref (streamId, eventId):
    return streamId + ':' + eventId
eventref_udf = udf(event_ref, StringType())

def description (hour, count, prediction):
    return 'Unexpectedly High Number of Authentication Failures Rate, during hour starting ' + hour \
           + '.  Expected count:' + str(int(prediction)) + '. Actual count:' + str(count)

description_udf = udf(description, StringType())

#     withColumn('timestamp', date_format(current_timestamp(),"dd-MM-yyyy'T'HH:mm:ss.SSS").cast('string')).\

def process_batch(df, epoch_id):
    df.withColumn ('count',size('eventref')).filter((col('count') > col('prediction') * 2) &
                                                    (col('count') > 10)). \
        withColumn('hour',col('window.start').cast('string')). \
        withColumn('timestamp', now_udf('hour')). \
        withColumn('title',lit('Unexpectedly High Authentication Failures Rate')). \
        withColumn('description',description_udf('hour','count','prediction')). \
        withColumn ('hourLit',lit('hour')). \
        withColumn ('countLit',lit('count')). \
        withColumn ('predLit',lit('prediction')). \
        withColumn ('eventRefLit', lit('eventref')). \
        select(['timestamp','title','description','eventRefLit','eventref','hourLit','hour',
                'countLit','count','predLit','prediction']). \
        toPandas().to_csv(outputdir + "/hits-" + str(epoch_id) + ".csv", header=False, index=False)

#col('headers') should be available on the DataFrame (Spark > 3.0)
#Code will be something similar to the line below to be added to the DataFrame definition
#withColumn('headerMap', col('headers').cast('MapType(String,String)')).\
wideDf = df.withColumn('json',col('value').cast('string')). \
    withColumn('evt', from_json(col('json'), json_schema)). \
    withColumn ('timestamp', to_timestamp(col('evt.EventTime.TimeCreated')).cast("timestamp")). \
    withColumn('operation', col('evt.EventDetail.TypeId')). \
    filter(col('operation') == 'Authentication Failure' ). \
    withColumn('streamid', col('evt.StreamId')). \
    withColumn('eventid', col('evt.EventId')). \
    dropDuplicates(["eventid", "streamid"]). \
    groupBy(window ("timestamp", "1 hour"),
            date_format('timestamp', 'EEEE').alias("day"),
            hour("timestamp").alias("hour")).agg(collect_set(eventref_udf('streamid','eventid'))
                                                 .alias('eventref')).alias("wide")

# print("Wide")
# wideDf.show()


pipelineModel = PipelineModel.load("jupyter/singleEventComplexAnalysis/models/inputVecPipelineModel")

featuresDf = pipelineModel.transform(wideDf)

# print("Features")
# featuresDf.show()

vectorAssembler = VectorAssembler(inputCols = ['hourVec','dayVec'], outputCol = 'features')

fullDf = vectorAssembler.transform(featuresDf)
#.select('window','features',size('eventref'))

# print("Full")
# fullDf.show()

lrModel = LogisticRegressionModel.load("jupyter/singleEventComplexAnalysis/models/logisticRegressionAuthFailuresModel")

lrDf = lrModel.transform (fullDf).alias("transformed")

# print("LR")
# lrDf.show()

# #outputMode can be append, complete or update
#join(wideDf, col("wide.window") == col('transformed.window'), 'inner').writeStream. \
query = lrDf.writeStream. \
    outputMode("update"). \
    foreachBatch (process_batch). \
    start()

print ("Starting...")

query.awaitTermination()