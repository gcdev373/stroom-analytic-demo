package stroom.analytics.statemonitor

import java.io.File
import java.sql.Timestamp

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json, lit, to_timestamp}
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import org.apache.spark.sql.types.DataType

import scala.collection.mutable
import stroom.analytics.statemonitor.beans._

import scala.collection.immutable.HashMap

object StateMonitor{
  val DEFAULT_TIMEOUT = "P1D"
  var config : Global = null
  var stateMap : Map [String, State] = null
  var stateLatencyMap : Map [String, java.time.Duration] = new HashMap

  case class RowDetails(key:String, timestamp:java.sql.Timestamp, state:String, open: Boolean, tag1: String, tag2: String, tag3 : String)

  //All the sessions for all activities for a single user
//  class UserState (val user:String, val sessionsForActivity: mutable.HashMap[String, SessionList] = new mutable.HashMap)

  case class StateTransition (state : String, timestamp: java.sql.Timestamp, open : Boolean,
                              tag1: Option[String] = None, tag2: Option[String] = None, tag3: Option[String] = None)


  case class KeyState (transitions: Seq[StateTransition], previouslyAlerted: Seq[StateTransition], lastRun: Option[java.sql.Timestamp])

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

  def logError(str: String):Unit ={
    println("Error: " + str)
  }


  //Used to check that each option that is present on a possible matching state
  def compareTransitions (primary : StateTransition, secondary: StateTransition): Boolean ={
    if (primary.tag1.isDefined && !secondary.tag1.isDefined)
      false
    else if (primary.tag1.isDefined && primary.tag1.get != secondary.tag1.get)
      false
    else if (primary.tag2.isDefined && !secondary.tag2.isDefined)
      false
    else if (primary.tag2.isDefined && primary.tag2.get != secondary.tag2.get)
      false
    else if (primary.tag3.isDefined && !secondary.tag3.isDefined)
      false
    else if (primary.tag3.isDefined && primary.tag3.get != secondary.tag3.get)
      false
    else
      true
  }

  //Used to compare closes with opens (fewer tags might be available on close events, and so there is a need for
  //additional configuration to specify which tagged state to close (todo)
  //For now a close will match any open that has all its tags
  def compareApproximate (closingTransition : StateTransition, openingTransition: StateTransition): Boolean ={
    if (closingTransition.state != openingTransition.state)
      false;
    else if (closingTransition.tag1.isDefined && !openingTransition.tag1.isDefined &&
      closingTransition.tag1.get != openingTransition.tag1.get)
      false
    else if (closingTransition.tag2.isDefined && !openingTransition.tag2.isDefined &&
      closingTransition.tag2.get != openingTransition.tag2.get)
      false
    else if (closingTransition.tag3.isDefined && !openingTransition.tag3.isDefined &&
      closingTransition.tag3.get != openingTransition.tag3.get)
      false
    else
      true
  }

  def hasTimedOut(transition: StateTransition, atTime : Timestamp): Boolean = {
    val timeoutStr = Option(stateMap.get(transition.state).get.open.timeout).getOrElse(DEFAULT_TIMEOUT)

    val dur = java.time.Duration.parse(timeoutStr)

    atTime.after(new Timestamp(transition.timestamp.toInstant.plus(dur).toEpochMilli))
  }

  def maximumLatencyExpired(incomingTransition: StateMonitor.StateTransition, otherStateName: String, timestamp: Timestamp) : Boolean = {

    val latencyExpirationTime = incomingTransition.timestamp.toInstant.plus(stateLatencyMap.get(otherStateName).get)
    timestamp.after(new Timestamp(latencyExpirationTime.toEpochMilli))
  }

  def validateStates(key: String, keyState: KeyState, newRunTime : java.sql.Timestamp) : KeyState = {
    //Take account of timeout auto-closed states

    //Start at first transition

//Find in/out state (and associated tags) of each State by playing transitions one by one

    //If required state is missing AND that state is passed its maximum latency => alert

    //Transitions (but all open=true : when state is closed, there is simply no value)
    var stateAtPointInTime : Seq [StateTransition] = Nil

    var allAlerted = keyState.previouslyAlerted

    keyState.transitions.foreach(
      transition=>{
        transition.open match {
          case true => {
            //Open transition (add to list but remove any previous opens relating to this state and tags
            stateAtPointInTime = transition +: stateAtPointInTime.filter(x => {x.state != transition.state || !compareTransitions(x, transition)})


            //            printf("%s Open: State changes to %s\n", transition.timestamp, stateAtPointInTime)
            //Check that all the required states are present
            Option(stateMap.get(transition.state).get.open.requires) match {
              case Some(x) => {
                x.foreach(requiredState => {
                  val thing1 = stateAtPointInTime.filter(_.state == requiredState)
                  val thing2 = thing1.filter(compareTransitions(_, transition))
                  val thing3 = thing2.filter(!hasTimedOut(_, transition.timestamp))

                  val thing4 = stateLatencyMap.get(requiredState)

                  if (!allAlerted.contains(transition) &&
                      maximumLatencyExpired(transition,requiredState, newRunTime) &&
                      stateAtPointInTime.filter(_.state == requiredState).
                      filter(compareTransitions(_, transition)). //Check tags match
                      filter(!hasTimedOut(_, transition.timestamp)).

                      isEmpty) {
                    allAlerted = transition +: allAlerted
                    createAlert (key, requiredState, transition)
                  }

                })
              }
              case None => {}
            }
          }
          case false => {
            //Close transition
            stateAtPointInTime = stateAtPointInTime.filter(!compareApproximate(transition,_))

//            printf ("%s Close: State changes to %s\n", transition.timestamp, stateAtPointInTime )

          }

        }

      }

    )

    thinTransitionsAndOldAlerts (KeyState(keyState.transitions,allAlerted, Option(newRunTime)))
  }

  def createAlert (key: String, requiredState: String, transition: StateTransition): Unit ={
    printf("Required state %s is missing for %s at time %s whilst opening state : %s ", requiredState, key, transition.timestamp, transition.state)
    transition.tag1.foreach(printf ("%s: %s ", config.tags.tag1, _))
    transition.tag2.foreach(printf ("%s: %s ", config.tags.tag2, _))
    transition.tag3.foreach(printf ("%s: %s",config.tags.tag3, _))

    printf("\n")
  }

  //todo implement this to prevent memory blowing up
  def thinTransitionsAndOldAlerts(keyState: KeyState) :KeyState = {
    keyState
  }

  def collateAndValidate(key: String, unsortedKeyState: KeyState, timestamp: Timestamp = null) : KeyState = {
    if (key.startsWith("User20")) {
      printf ("Collating and validating %s\n", key)
    }

    val keyState = KeyState(unsortedKeyState.transitions.sortWith((a, b)=> a.timestamp.compareTo(b.timestamp) < 1),
      unsortedKeyState.previouslyAlerted,
      unsortedKeyState.lastRun)

    validateStates (key, keyState, Option(timestamp).getOrElse(new Timestamp(System.currentTimeMillis())))


  }

  def updateState(key: String, rows: Iterator[RowDetails], groupState: GroupState[KeyState]): KeyState = {

    if (key.startsWith("User20")) {
      printf("Updating state for %s\n", key)
    }

    if (groupState.hasTimedOut) { // If called when timing out, remove the state

      if (groupState.exists) {
        val keyState = collateAndValidate(key, groupState.get)

        if (key.startsWith("User20")) {
          printf("There are now %d transitions\n ", keyState.transitions.length)
          printf("Tag1 is %s ", keyState.transitions.filter(_.tag1.isDefined).headOption match { case Some(x) => x.tag1 case None => "Undefined" })
          printf("Tag2 is %s ", keyState.transitions.filter(_.tag2.isDefined).headOption match { case Some(x) => x.tag2 case None => "Undefined" })
          printf("Tag3 is %s \n", keyState.transitions.filter(_.tag3.isDefined).headOption match { case Some(x) => x.tag3 case None => "Undefined" })

        }

        if (keyState.transitions.isEmpty)
          groupState.remove()
        else
          groupState.update(keyState)

        groupState.setTimeoutDuration("150 seconds")
        keyState
      }
      else {
        groupState.setTimeoutDuration("150 seconds")
        KeyState(Nil, Nil, None)
      }

    } else {
      groupState.setTimeoutDuration("150 seconds")

      val keyState = groupState.getOption.getOrElse(KeyState(Nil, Nil, None))

      val updatedKeyState = KeyState(keyState.transitions ++ rows.map(row =>
        StateTransition(row.state, row.timestamp, row.open, Option(row.tag1), Option(row.tag2), Option(row.tag3))),
        keyState.previouslyAlerted,
        keyState.lastRun)

      if (key.startsWith("User20")) {
        printf("There are now %d transitions\n", updatedKeyState.transitions.length)
      }

      groupState.update(updatedKeyState)
      updatedKeyState
    }
  }

  def initialiseConfig (configFile : File) : Unit = {
    val mapper = new ObjectMapper(new YAMLFactory)
    mapper.registerModule(DefaultScalaModule)
    config = mapper.readValue(configFile, classOf[Global])

    stateMap = config.states.groupBy(_.name).mapValues(_.head)

    stateLatencyMap = stateLatencyMap ++ config.states.map(x => x.name -> java.time.Duration.parse(Option(x.maxlatency).getOrElse(DEFAULT_TIMEOUT)))
  }

  def main(args: Array[String]): Unit = {



    if (args.length == 0){
      printf ("Please specify the path of the yaml configuration file as argument.  This should be the last argument to spark-submit.")
      return
    }
    printf ("Initialising StateMonitor with config file %s...\n", args.head)

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

    initialiseConfig (configFile)

    printf ("Loaded Config: %s", config.states)

    val spark = SparkSession.builder.appName("State Monitor").getOrCreate()

    spark.sparkContext.setLogLevel("WARN")

    //Enables line as[InputRow]
    import spark.implicits._

    printf("Reading schema from file %s\n", config.schemaFile)
    val schema = scala.io.Source.fromFile(config.schemaFile, "utf-8").getLines.mkString



    val jsonSchema = DataType.fromJson(schema)

      val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", config.bootstrapServers)
    .option("subscribe", config.topic)
    .option("startingOffsets", "earliest")
//    .option("includeHeaders", "true") //Spark v3.0 needed for Kafka header support.
    .load()
      .withColumn("key",col("key").cast("string"))
      .withColumn("json",col("value").cast("string"))
      .withColumn("Event", from_json(col("json"), jsonSchema))
      .withColumn("streamid", col("Event.StreamId"))
      .withColumn("eventid", col("Event.EventId"))
      .dropDuplicates(Seq("eventid", "streamid"))
      .withColumn ("timestamp", to_timestamp(col("Event.EventTime.TimeCreated")).cast("timestamp"))

    var tagIdToNameMap : Map [String,String] = Map.empty
    Option(config.tags.tag1) match{
      case Some(x) => {tagIdToNameMap = tagIdToNameMap + ("tag1" -> x)}
      case None =>
    }
    Option(config.tags.tag2) match{
      case Some(x) => {tagIdToNameMap = tagIdToNameMap + ("tag2" -> x)}
      case None =>
    }
    Option(config.tags.tag3) match{
      case Some(x) => {tagIdToNameMap = tagIdToNameMap + ("tag3" -> x)}
      case None =>
    }


    val opens = config.states.map (state => {
      var updatedDf = df
        .filter(state.open.filter)
        .withColumn("state", lit(state.name))
        .withColumn("open", lit(true))


      for (tagNum <- 1 to 3) {
        val tagName = "tag" + tagNum

        tagIdToNameMap.get(tagName) match {
          case Some(x) => {
            val tagDefs = state.open.tags.filter(p => x == p.name)
            if (tagDefs.length == 0) {
              updatedDf = updatedDf.withColumn(tagName, lit(null))
            } else {
              updatedDf = updatedDf.withColumn(tagName, col(tagDefs.head.definition))
            }
          }
          case None => {
            updatedDf = updatedDf.withColumn(tagName, lit(null))
          }
        }
      }

      updatedDf
    }
    )

    //There is not necessarily a close for each state (can be null)
    val closes : Seq [Option[DataFrame]] = config.states.map (state => {
      val closeOpt = Option (state)
      closeOpt match {
        case Some(x)=>{
          var updatedDf = df
            .filter(state.close.filter)
            .withColumn("state", lit(state.name))
            .withColumn("open", lit(false))


          for (tagNum <- 1 to 3) {
            val tagName = "tag" + tagNum

            tagIdToNameMap.get(tagName) match {
              case Some(x) => {
                val tagDefs = state.close.tags.filter(p => x == p.name)
                if (tagDefs.length == 0) {
                  updatedDf = updatedDf.withColumn(tagName, lit(null))
                } else {
                  updatedDf = updatedDf.withColumn(tagName, col(tagDefs.head.definition))
                }
              }
              case None => {
                updatedDf = updatedDf.withColumn(tagName, lit(null))
              }
            }
          }

          Option(updatedDf)
        }
        case None => {
          None
        }
      }

    }
    )

//    val closes =  global.states.map (state => df
//      .filter(state.close.filter)
//      .withColumn("state", lit(state.name))
//      .withColumn("open", lit(false))
//      .withColumn("tag1", lit("Tag One"))
//      .withColumn("tag2", lit("Tag Two"))
//      .withColumn("tag3", lit("Tag Three"))
//    )

    def unionize = (x : DataFrame, y : DataFrame) => x.union(y)

    val allDfs = opens ++ closes.flatten

//    val unionDf = allDfs.tail.fold(allDfs.head) (unionize)
    val unionDf = allDfs.reduce (unionize)

    val mappedGroups = unionDf.as[RowDetails].groupByKey(_.key)
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
