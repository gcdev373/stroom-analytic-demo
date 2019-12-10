import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.functions.to_timestamp
import org.apache.spark.sql.functions.date_format
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions.window
import org.apache.spark.sql.functions.hour
import org.apache.spark.sql.streaming
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object StateMonitor{

  case class RowDetails(user:String, timestamp:java.sql.Timestamp, operation:String)

   class RangedTimestamp (val timestamp: Option [java.sql.Timestamp], val definite : Boolean = true) {
    def this(eventTime: java.sql.Timestamp) = {
      this(Option(eventTime))
    }

     def this(eventTime: java.sql.Timestamp, definite : Boolean) = {
       this(Option(eventTime), definite)
     }

     def isEmpty() : Boolean = timestamp.isEmpty

     def get(): java.sql.Timestamp = timestamp.get
  }

  //A single session of an activity by a single user.  Start or end might not have been determined
  class Session(var start:RangedTimestamp, var end:RangedTimestamp) {
    def this (eventTime: java.sql.Timestamp, isStart : Boolean) =
    {
        this(new RangedTimestamp(eventTime), new RangedTimestamp(eventTime))
        isStart match {
          case true => end = new RangedTimestamp(None)
          case false => start = new RangedTimestamp(None)
      }

    }
    def inside (timestamp : java.sql.Timestamp) : Boolean = {
      if (start.isEmpty && timestamp.before(end.get))
        true
      else if (end.isEmpty && timestamp.after (start.get))
        true
      else if (timestamp.after(start.get) && timestamp.before(end.get))
        true
      else
        false
    }
  }

  //A collection of sessions that relate to the same activity for a single user
  //It is possible for sessions to split, if data is processed out of order
  class SessionList(var sessions: List[Session] = List()) {
    def addStartTime (timestamp: java.sql.Timestamp) {
      val currentSessions = sessions.filter (_.inside(timestamp))
      if (currentSessions.isEmpty) {
        //Immutable list is recreated with element added.  Potentially expensive, consider ListBuffer
        sessions = new Session (timestamp, true) :: sessions
      }
      else {
        if (currentSessions.size > 1)
          throw new IllegalStateException("More than one open state at " + timestamp) // Deal with multiple opens

        //There should only be one or zero states open at a point in time
        val session = currentSessions.head
        if (session.start.isEmpty) {
          //Can modify the existing session
          session.start = new RangedTimestamp(timestamp)
        } else{
          //Need to add a session
          val additionalSession: Session = null
          if (session.end.isEmpty()){
            session.end = new RangedTimestamp(timestamp, false)
            additionalSession
          } else {

          }
        }

      }

  }

  //All the sessions for all activities for a single user
  class UserState (val user:String, val sessionsForActivity: mutable.HashMap[String, SessionList] = new mutable.HashMap)

  // JSON Schema (expressed in JSON format) Derived using static query via stroom-spark-datasource
  //
  // [python]
  // spark.read.json(schemaDf.rdd.map(lambda row: row.json)).schema.json()
  //
  val schema = "{\"fields\":[{\"metadata\":{},\"name\":\"EventDetail\",\"nullable\":true," +
                "\"type\":{\"fields\":[{\"metadata\":{},\"name\":\"Authenticate\",\"nullable\":true," +
                "\"type\":{\"fields\":[{\"metadata\":{},\"name\":\"Action\",\"nullable\":true,\"type\":\"string\"}," +
                "{\"metadata\":{},\"name\":\"Outcome\",\"nullable\":true,\"type\":{\"fields\":[{\"metadata\":{}," +
                "\"name\":\"Permitted\",\"nullable\":true,\"type\":\"string\"},{\"metadata\":{},\"name\":\"Reason\"," +
                "\"nullable\":true,\"type\":\"string\"},{\"metadata\":{},\"name\":\"Success\",\"nullable\":true,\"type\":" +
                "\"string\"}],\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"User\",\"nullable\":true,\"type\":{\"fields" +
                "\":[{\"metadata\":{},\"name\":\"Id\",\"nullable\":true,\"type\":\"string\"}],\"type\":\"struct\"}}],\"type" +
                "\":\"struct\"}},{\"metadata\":{},\"name\":\"Process\",\"nullable\":true,\"type\":{\"fields\":[{\"metadata" +
                "\":{},\"name\":\"Action\",\"nullable\":true,\"type\":\"string\"},{\"metadata\":{},\"name\":\"Command\"," +
                "\"nullable\":true,\"type\":\"string\"},{\"metadata\":{},\"name\":\"Type\",\"nullable\":true,\"type\":" +
                "\"string\"}],\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"TypeId\",\"nullable\":true,\"type\":\"string" +
                "\"}],\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"EventId\",\"nullable\":true,\"type\":\"string\"},{" +
                "\"metadata\":{},\"name\":\"EventSource\",\"nullable\":true,\"type\":{\"fields\":[{\"metadata\":{},\"name\":" +
                "\"Device\",\"nullable\":true,\"type\":{\"fields\":[{\"metadata\":{},\"name\":\"HostName\",\"nullable\":true," +
                "\"type\":\"string\"}],\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"Generator\",\"nullable\":true,\"type\":" +
                "\"string\"},{\"metadata\":{},\"name\":\"System\",\"nullable\":true,\"type\":{\"fields\":[{\"metadata\":{},\"name" +
                "\":\"Environment\",\"nullable\":true,\"type\":\"string\"},{\"metadata\":{},\"name\":\"Name\",\"nullable\":true," +
                "\"type\":\"string\"}],\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"User\",\"nullable\":true,\"type\":{" +
                "\"fields\":[{\"metadata\":{},\"name\":\"Id\",\"nullable\":true,\"type\":\"string\"}],\"type\":\"struct\"}}]," +
                "\"type\":\"struct\"}},{\"metadata\":{},\"name\":\"EventTime\",\"nullable\":true,\"type\":{\"fields\":[{" +
                "\"metadata\":{},\"name\":\"TimeCreated\",\"nullable\":true,\"type\":\"string\"}],\"type\":\"struct\"}},{" +
                "\"metadata\":{},\"name\":\"StreamId\",\"nullable\":true,\"type\":\"string\"}],\"type\":\"struct\"}"

  def updateSessionsForActivityWithSingleRow (activity: String, sessions)

  def updateState (user: String, events: Iterator[RowDetails], state: GroupState[UserState]): UserState = {
    if (state.hasTimedOut) {                // If called when timing out, remove the state
      printf("State timed out", state)
      state.remove()
    } else if (state.exists) {              // If state exists, use it for processing
      val existingState = state.get         // Get the existing state
        var sessions = existingState.sessionsForActivity.getOrElseUpdate(List())

      } else {
        val newState = ...
        state.update(newState)              // Set the new state
        state.setTimeoutDuration("1 hour")  // Set the timeout
      }

    } else {
      val initialState = ...
      state.update(initialState)            // Set the initial state
      state.setTimeoutDuration("1 hour")    // Set the timeout
    }

    state
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("State Monitor").getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    //Enables line as[InputRow]
    import spark.implicits._

    val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "ANALYTIC-DEMO-UEBA")
    .option("startingoffsets", "earliest")
    .option("includeHeaders", "true")
    .load()

    val jsonSchema = DataType.fromJson(schema);

    val wideDf = df.withColumn("json",col("value").cast("string")).
      withColumn("evt", from_json(col("json"), jsonSchema)).
      withColumn ("timestamp", to_timestamp(col("evt.EventTime.TimeCreated")).cast("timestamp")).
      withColumn ("user", col("evt.EventSource.User.Id")).
      withColumn("operation", col("evt.EventDetail.TypeId")).
      selectExpr ("user, timestamp, operation").
      as[RowDetails].groupByKey(_.user).
      mapGroupsWithState (GroupStateTimeout.ProcessingTimeTimeout)(updateState)



//      filter(col("operation") === "Authentication Failure" ).
//    withColumn("streamid", col("evt.StreamId")).
//    withColumn("eventid", col("evt.EventId")).
//    dropDuplicates(Seq("eventid", "streamid")).
//    groupBy(window (col("timestamp"), "1 hour"),
//      date_format(col("timestamp"), "EEEE").alias("day"),
//    hour(col("timestamp")).alias("hour")).count()

    val query = wideDf.writeStream
        .outputMode("update")
        .format("console")
       .start()

    query.awaitTermination()
  }
}
