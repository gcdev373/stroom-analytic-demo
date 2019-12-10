import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object StateMonitor{

  case class RowDetails(user:String, timestamp:java.sql.Timestamp, operation:String)

//   class RangedTimestamp (val timestamp: Option [java.sql.Timestamp], val definite : Boolean = true) {
//    def this(eventTime: java.sql.Timestamp) = {
//      this(Option(eventTime))
//    }
//
//     def this(eventTime: java.sql.Timestamp, definite : Boolean) = {
//       this(Option(eventTime), definite)
//     }
//
//     def isEmpty() : Boolean = timestamp.isEmpty
//
//     def get(): java.sql.Timestamp = timestamp.get
//  }
//
//  //A single session of an activity by a single user.  Start or end might not have been determined
//  class Session(var start:RangedTimestamp, var end:RangedTimestamp) {
//    def this (eventTime: java.sql.Timestamp, isStart : Boolean) =
//    {
//        this(new RangedTimestamp(eventTime), new RangedTimestamp(eventTime))
//        isStart match {
//          case true => end = new RangedTimestamp(None)
//          case false => start = new RangedTimestamp(None)
//      }
//
//    }
//    def inside (timestamp : java.sql.Timestamp) : Boolean = {
//      if (start.isEmpty && timestamp.before(end.get))
//        true
//      else if (end.isEmpty && timestamp.after (start.get))
//        true
//      else if (timestamp.after(start.get) && timestamp.before(end.get))
//        true
//      else
//        false
//    }
//  }

  //A collection of sessions that relate to the same activity for a single user
  //It is possible for sessions to split, if data is processed out of order
//  class SessionList(var sessions: List[Session] = List()) {
//    def addStartTime (timestamp: java.sql.Timestamp) {
//      val currentSessions = sessions.filter (_.inside(timestamp))
//      if (currentSessions.isEmpty) {
//        //Immutable list is recreated with element added.  Potentially expensive, consider ListBuffer
//        sessions = new Session (timestamp, true) :: sessions
//      }
//      else {
//        if (currentSessions.size > 1)
//          throw new IllegalStateException("More than one open state at " + timestamp) // Deal with multiple opens
//
//        //There should only be one or zero states open at a point in time
//        val session = currentSessions.head
//        if (session.start.isEmpty) {
//          //Can modify the existing session
//          session.start = new RangedTimestamp(timestamp)
//        } else{
//          //Need to add a session
//          val additionalSession: Session = null
//          if (session.end.isEmpty()){
//            session.end = new RangedTimestamp(timestamp, false)
//            additionalSession
//          } else {
//
//          }
//        }
//
//      }
//
//  }

  //All the sessions for all activities for a single user
//  class UserState (val user:String, val sessionsForActivity: mutable.HashMap[String, SessionList] = new mutable.HashMap)

  case class StateTransition (timestamp: java.sql.Timestamp, open : Boolean)

  case class UserTransitions (val user: String, val sessionsForActivity: mutable.HashMap[String, List[StateTransition]] = new mutable.HashMap()) {
    def addOpenTransition (activity: String, timestamp: java.sql.Timestamp) =
      addTransition(activity, StateTransition(timestamp, true))

    def addCloseTransition (activity: String, timestamp: java.sql.Timestamp) =
      addTransition(activity, StateTransition(timestamp, false))

    def addTransition (activity: String, stateTransition: StateTransition) = {
      //todo don't sort after each add
      sessionsForActivity += (activity -> (stateTransition :: sessionsForActivity(activity)).sortWith((a, b)=> a.timestamp.compareTo(b.timestamp) < 1))
    }

  }

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

  def updateStateWithSingleRow (sessions : UserTransitions, row : RowDetails) : UserTransitions = {
    val tokens = row.operation.split("|")

    if (tokens.length != 2){
      printf("Not got correct format operation %s\n there are %d tokens", row.operation, tokens.length)
    }
    else {
      val activity = tokens.head

      if (tokens.tail.head == "Login")
        sessions.addOpenTransition(activity, row.timestamp)
      else
        sessions.addCloseTransition(activity, row.timestamp)
    }
    sessions
  }

  def updateState (user: String, rows: Iterator[RowDetails], groupState: GroupState[UserTransitions]): UserTransitions = {
printf ("**************************Updating state for rows and user %s ", user)
      if (groupState.hasTimedOut) {                // If called when timing out, remove the state
        printf("**!*!*!*!*!*!*!*!*!*!*!State timed out")

        if (rows.isEmpty)
          groupState.remove()
        else
          printf("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!There are values during timeout - this shouldn't happen?")
      }

    groupState.setTimeoutDuration("1 hour")

    var transitions : UserTransitions = if (groupState.exists) groupState.get else new UserTransitions(user)

    for (row <- rows) {
      transitions = updateStateWithSingleRow(transitions, row)
      groupState.update(transitions)
    }


    transitions
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
      filter((col("evt.EventDetail.TypeId") === "Login") ||  (col("evt.EventDetail.TypeId") === "Logout")).
      withColumn ("timestamp", to_timestamp(col("evt.EventTime.TimeCreated")).cast("timestamp")).
      withColumn ("user", col("evt.EventSource.User.Id")).
      withColumn("operation", concat_ws("|",col("evt.EventSource.System.Name"),col("evt.EventDetail.TypeId"))).
      select(col("user"),col("timestamp"), col("operation")).
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
