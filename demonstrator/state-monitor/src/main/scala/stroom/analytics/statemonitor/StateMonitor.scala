package stroom.analytics.statemonitor

import java.io.File

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json, lit, to_timestamp}
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import org.apache.spark.sql.types.DataType

import scala.collection.mutable
import stroom.analytics.statemonitor.beans._

object StateMonitor{

  case class RowDetails(user:String, timestamp:java.sql.Timestamp, state:String, open: Boolean, tag1: String, tag2: String, tag3 : String)

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
    def addOpenTransition(activity: String, timestamp: java.sql.Timestamp) : Unit = {

      addTransition(activity, StateTransition(timestamp, true))
    }

    def addCloseTransition(activity: String, timestamp: java.sql.Timestamp): Unit = {

      addTransition(activity, StateTransition(timestamp, false))
    }
    def addTransition (activity: String, stateTransition: StateTransition): Unit = {

//      if (stateTransition.open)
//        printf ("Open %s ", user)
//      else
//        printf ("Close %s ", user)
      if (sessionsForActivity.contains(activity))
        sessionsForActivity.put (activity,(stateTransition :: sessionsForActivity(activity)))
      else
        sessionsForActivity.put(activity, (stateTransition :: Nil))
    }
    def addTransition(row: RowDetails): Unit = addTransition(row.state, StateTransition (row.timestamp, row.open))

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
    sessions.addTransition(row)

    sessions
  }

  def checkInOuts(user: String, transitions: List[StateMonitor.StateTransition])={
    val opens = transitions.exists(_.open)
    val closes = transitions.exists(!_.open)
    if (opens && closes)
      printf ("%s Both ins and outs exist\n", user)
    else if (opens)
      printf ("%s Only opens here\n",user)
    else if (closes)
      printf ("%s Closes only\n",user)
    else
      printf ("%s Nothing at all!\n",user)

  }

  def updateState (user: String, rows: Iterator[RowDetails], groupState: GroupState[UserTransitions]): UserTransitions = {
    if (user == null) {
      printf ("User is null\n")
      new UserTransitions("Error")
    }
    else {
      if (groupState.hasTimedOut) { // If called when timing out, remove the state

        if (groupState.exists)
          checkInOuts (user,groupState.get.sessionsForActivity("MAINFRAME").
            sortWith((a, b)=> a.timestamp.compareTo(b.timestamp) < 1))

        if (rows.isEmpty)
          groupState.remove()
        else
          printf("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!There are values during timeout - this shouldn't happen?")
      }

      groupState.setTimeoutDuration("150 seconds")

      var transitions: UserTransitions = if (groupState.exists) groupState.get else new UserTransitions(user)

      for (row <- rows) {
        transitions = updateStateWithSingleRow(transitions, row)
        groupState.update(transitions)
      }

      transitions
    }
  }

  def main(args: Array[String]): Unit = {
    printf ("Initialising StateMonitor...")

    val mapper = new ObjectMapper(new YAMLFactory)
    mapper.registerModule(DefaultScalaModule)

    if (args.length == 0){
      printf ("Please specify the path of the yaml configuration file as argument.  This should be the last argument to spark-submit.")
      return
    }

    var configFile = new File(args.head)

//    if (!configFile.exists()) {
//        val classLoader = Thread.currentThread().getContextClassLoader()
//        if (classLoader.getResource(args.head) != null)
//          configFile = new File(classLoader.getResource(args.head).getFile())
//      }

    if (!configFile.exists()) {
      System.err.println("FATAL: Unable to read file " + args.head)
      System.exit(2)
    }


    val global = mapper.readValue(configFile, classOf[Global])

    printf ("Loaded Config: %s", global.states)

    val spark = SparkSession.builder.appName("State Monitor").getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    //Enables line as[InputRow]
    import spark.implicits._


    val jsonSchema = DataType.fromJson(schema)

      val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "ANALYTIC-DEMO-UEBA")
    .option("startingOffsets", "earliest")
//    .option("includeHeaders", "true") //Spark v3.0 needed for Kafka header support.
    .load()
      .withColumn("user",col("key").cast("string"))
      .withColumn("json",col("value").cast("string"))
      .withColumn("Event", from_json(col("json"), jsonSchema))
      .withColumn("streamid", col("Event.StreamId"))
      .withColumn("eventid", col("Event.EventId"))
      .dropDuplicates(Seq("eventid", "streamid"))
      .withColumn ("timestamp", to_timestamp(col("Event.EventTime.TimeCreated")).cast("timestamp"))


    val opens = global.states.map (state => df
           .filter(state.open.filter)
      .withColumn("state", lit(state.name))
      .withColumn("open", lit(true))
      .withColumn("tag1", lit("Tag One"))
      .withColumn("tag2", lit("Tag Two"))
      .withColumn("tag3", lit("Tag Three"))
    )

    val closes =  global.states.map (state => df
      .filter(state.close.filter)
      .withColumn("state", lit(state.name))
      .withColumn("tag1", lit("Tag One"))
      .withColumn("open", lit(false))
      .withColumn("tag2", lit("Tag Two"))
      .withColumn("tag3", lit("Tag Three"))
    )

    def unionize = (x : DataFrame, y : DataFrame) => x.union(y)

    val allDfs = opens ++ closes

    val unionDf = allDfs.tail.fold(allDfs.head) (unionize)

    val mappedGroups = unionDf.as[RowDetails].groupByKey(_.user)
        .mapGroupsWithState (GroupStateTimeout.ProcessingTimeTimeout)(updateState)

    //    val wideDf2 = df.
//      withColumn("user",col("key").cast("string")).
//      withColumn("json",col("value").cast("string")).
//      withColumn("evt", from_json(col("json"), jsonSchema)).
//      filter((col("evt.EventDetail.TypeId") === "Logout")).
//      withColumn ("timestamp", to_timestamp(col("evt.EventTime.TimeCreated")).cast("timestamp")).
//      withColumn("state", col("evt.EventSource.System.Name")).
//      withColumn("open", lit(false))



//    withColumn("streamid", col("evt.StreamId")).
//    withColumn("eventid", col("evt.EventId")).
//    dropDuplicates(Seq("eventid", "streamid")).
//    groupBy(window (col("timestamp"), "1 hour"),
//      date_format(col("timestamp"), "EEEE").alias("day"),
//    hour(col("timestamp")).alias("hour")).count()


//    val unionDf = wideDf1.union(wideDf2)
//      .as[RowDetails] //To strongly typed DataSet
//      .groupByKey (_.user)
//      //.groupByKey(_=> _(0)).
//      .mapGroupsWithState (GroupStateTimeout.ProcessingTimeTimeout)(updateState)

    val query = mappedGroups.writeStream
          .outputMode("update")
          .format("console")
         .start()
    query.awaitTermination()
  }
}
