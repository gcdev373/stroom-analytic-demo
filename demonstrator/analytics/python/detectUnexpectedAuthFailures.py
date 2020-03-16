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
    .option("failOnDataLoss", "true") \
    .load()

def timestamp_now (dummy):
    return datetime.datetime.utcnow().isoformat()[:-3]+'Z'
now_udf = udf(timestamp_now, StringType())

def event_ref (streamId, eventId):
    return streamId + ':' + eventId
eventref_udf = udf(event_ref, StringType())

def detail (hour, count, prediction):
    return 'Unexpectedly High Number of Authentication Failures during hour starting ' + hour \
           + '.  Expected count:' + str(int(prediction)) + '. Actual count:' + str(count)

detail_udf = udf(detail, StringType())

def description (hour):
    return 'Unexpectedly High Number of Authentication Failures during hour starting ' + hour + '.'

description_udf = udf(description, StringType())

#     withColumn('timestamp', date_format(current_timestamp(),"dd-MM-yyyy'T'HH:mm:ss.SSS").cast('string')).\

def process_batch(df, epoch_id):
    df.withColumn ('count',size('eventref')).filter((col('count') > col('prediction') * 2) &
                                                    (col('count') > 10)). \
        withColumn('hour',col('window.start').cast('string')). \
        withColumn('timestamp', now_udf('hour')). \
        withColumn('title',lit('Unexpectedly High Authentication Failures Rate')). \
        withColumn('description',description_udf('hour')). \
        withColumn('detail',description_udf('hour','count','prediction')). \
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


pipelineModel = PipelineModel.load("jupyter/singleEventComplexAnalysis/models/inputVecPipelineModel")

featuresDf = pipelineModel.transform(wideDf)

vectorAssembler = VectorAssembler(inputCols = ['hourVec','dayVec'], outputCol = 'features')

fullDf = vectorAssembler.transform(featuresDf)

lrModel = LogisticRegressionModel.load("jupyter/singleEventComplexAnalysis/models/logisticRegressionAuthFailuresModel")

lrDf = lrModel.transform (fullDf).alias("transformed")

query = lrDf.writeStream. \
    outputMode("update"). \
    foreachBatch (process_batch). \
    start()

print ("Starting...")

query.awaitTermination()