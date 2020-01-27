package stroom.analytics.statemonitor

import java.io.{File, FileWriter}
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneId, ZoneOffset}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json, lit, to_timestamp}
import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import org.apache.spark.sql.types.DataType

import scala.collection.mutable

import scala.collection.immutable.{HashMap, HashSet}
import scala.util.Random


case class RowDetails(key:String, timestamp:java.sql.Timestamp, state:String, open: Boolean,
                      tag1: String, tag2: String, tag3 : String,
                      eventid: String, streamid: String)

case class StateTransition (state : String, timestamp: java.sql.Timestamp, open : Boolean,
                            eventid : Option[String] = None, streamid : Option[String] = None,
                            tag1: Option[String] = None, tag2: Option[String] = None, tag3: Option[String] = None)


case class KeyState (transitions: Seq[StateTransition], previouslyAlerted: Seq[StateTransition], lastRun: Option[java.sql.Timestamp],
                     lastWarn: Option[java.sql.Timestamp] = None)


class StateMonitor extends Serializable {
  val POP_STRATEGY = "pop"
  val POP_ALL_STRATEGY = "close"
  val WARN_STRATEGY = "warn"

  private val verboseTrace = false
  private val DEFAULT_TIMEOUT = "P1D"
  private val DEFAULT_INTERVAL = "5 minutes"
  private val EVENT_ID_COLUMN_NAME = "eventid"
  private val STREAM_ID_COLUMN_NAME = "streamid"

  private var configFileBasename : String = null
  private var config : Config = null
  private var interval :String = null
  private var stateMap : Map [String, State] = null
  private var stateLatencyMap : Map [String, Duration] = new HashMap

  private var closeStateAmbiguityStrategyMap : Map [String, String] = new HashMap


  private var ambiguousTransitionsAlreadyWarned : Set [StateTransition] = new HashSet

  private var alertFilename : String = null
  private var reportAlertsAfter : Instant = null;
  private var reportingDelayDuration : Duration = null
  private var eventThinningDelay: Duration = null

  private var retainAlerts : Boolean = false

  def logError(str: String):Unit ={
    println("Error: " + str)
  }


  //Used to check that each option that is present on a possible matching state
  def transitionTagsMatch (primary : StateTransition, secondary: StateTransition): Boolean ={
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
  def transitionTagsMatchApproximately (closingTransition : StateTransition, openingTransition: StateTransition): Boolean ={
    if (closingTransition.state != openingTransition.state)
      false
    else if (closingTransition.tag1.isDefined && (!openingTransition.tag1.isDefined ||
      closingTransition.tag1.get != openingTransition.tag1.get))
      false
    else if (closingTransition.tag2.isDefined && (!openingTransition.tag2.isDefined ||
      closingTransition.tag2.get != openingTransition.tag2.get))
      false
    else if (closingTransition.tag3.isDefined && (!openingTransition.tag3.isDefined ||
      closingTransition.tag3.get != openingTransition.tag3.get))
      false
    else
      true
  }

  def hasTimedOut(transition: StateTransition, atTime : Timestamp): Boolean = {
    val timeoutStr = Option(stateMap.get(transition.state).get.open.timeout).getOrElse(DEFAULT_TIMEOUT)

    val dur = java.time.Duration.parse(timeoutStr)

    atTime.after(new Timestamp(transition.timestamp.toInstant.plus(dur).toEpochMilli))
  }

  def maximumLatencyExpired(incomingTransition: StateTransition, otherStateName: String, timestamp: Timestamp) : Boolean = {

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

    var lastWarn : Instant = null;

    if (keyState.lastWarn.isDefined)
      lastWarn = keyState.lastWarn.get.toInstant

    var allAlerted = keyState.previouslyAlerted

    keyState.transitions//HERE .filter(t=>canThin(new Instant(t.timestamp), new Instant(newRunTime)))
      .foreach(
      transition=> {
        transition.open match {
          case true => {


            //Open transition (add to list but remove any previous opens relating to this state and tags
            //stateAtPointInTime = transition +: stateAtPointInTime.filter(x => {x.state != transition.state || !transitionTagsMatch(x, transition)})
            stateAtPointInTime = transition +: stateAtPointInTime
            //Check that all the required states are present
            Option(stateMap.get(transition.state).get.open.requires) match {
              case Some(x) => {
                x.foreach(requiredState => {
                  if (!allAlerted.contains(transition) &&
                    maximumLatencyExpired(transition, requiredState, newRunTime) &&
                    stateAtPointInTime.filter(_.state == requiredState).
                      filter(transitionTagsMatch(transition, _)). //Check tags match
                      isEmpty) {
                    allAlerted = transition +: allAlerted

                    if (!hasTimedOut(transition, newRunTime) && //Might have been autoclosed
                      (!keyState.lastRun.isDefined ||
                        !maximumLatencyExpired(transition, requiredState, keyState.lastRun.get))) //Only allow one attempt to match, to avoid false positives after thinning
                      createAlert(key, requiredState, transition)

                    if (verboseTrace) {
                      printf("Transitions are currently %s\n", keyState.transitions)
                      printf("State is currently %s\n", stateAtPointInTime)
                    }
                  }
                })
              }
              case None => {}
            }
          }
          case false => {
            //Close transition
            val newState = stateAtPointInTime.filter(!transitionTagsMatchApproximately(transition, _))

            if (newState.length < stateAtPointInTime.length - 1) {
              //Multiple opens for this close (what to do)
              val ambiguousEvents : Seq [StateTransition] = stateAtPointInTime.filter(!newState.contains(_)).sortWith((a, b)=> a.timestamp.compareTo(b.timestamp) < 1)
              val strategy :String = closeStateAmbiguityStrategyMap.get(transition.state).getOrElse(WARN_STRATEGY)

              if (strategy == WARN_STRATEGY) {
                //Default behaviour is to alert (if not already for this
                if (lastWarn == null) {

                  printf("WARN: Ambigious close state. This can be caused by insufficient tag detail to discriminate or duplicate open events: %s\n",
                    stringifyTransition(transition))

                  val newBuilder = new StringBuilder()
                  ambiguousEvents.foreach(s => {
                    newBuilder.append(stringifyTransition(s) + " ")
                  })
                  printf("The ambiguous opening events follow: %s\n", newBuilder.toString())
                  printf("Temporarily suppressing further such warnings related to this key\n")
                  lastWarn = newRunTime.toInstant
                }
              } else if (strategy == POP_ALL_STRATEGY) {
                stateAtPointInTime = newState
              } else {
                //Pop the least recent matching open only
                stateAtPointInTime = newState ++ ambiguousEvents.tail
              }
            } else {
              stateAtPointInTime = newState
            }

          }

        }
      }
    )

    var updatedLastWarn : Option [Timestamp] = None
    if (lastWarn != null)
      updatedLastWarn = Option(new Timestamp(lastWarn.toEpochMilli))
    thinTransitionsAndOldAlerts (KeyState(keyState.transitions,allAlerted, Option(newRunTime), updatedLastWarn))
  }

  def stringifyTransition (transition: StateTransition): String = {
    val alertMsg : mutable.StringBuilder = new mutable.StringBuilder
    alertMsg.append(s"Event at ${transition.timestamp} state of ${transition.state}")
    if (transition.open)
      alertMsg.append ("-open")
    else
      alertMsg.append ("-close")

    if (transition.tag1.isDefined || transition.tag2.isDefined || transition.tag3.isDefined) {
      alertMsg.append(" (")
      transition.tag1.foreach((v) => {
        alertMsg.append(s"${config.tags.tag1}: ${v}")
      })
      transition.tag2.foreach((v) => {
        alertMsg.append(s" ${config.tags.tag2}: ${v}")
      })
      transition.tag3.foreach((v) => {
        alertMsg.append(s" ${config.tags.tag3}: ${v}")
      })
      alertMsg.append(")")
    }

    transition.eventid.foreach((v)=>{alertMsg.append(s" Associated eventid ${v}")})
    transition.streamid.foreach((v)=>{alertMsg.append(s" from streamid ${v}")})
    alertMsg.toString()
  }

  def createAlert (key: String, requiredState: String, transition: StateTransition): Unit ={

    val alertTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
    val timeStamp = Instant.now
    val alertMsg : mutable.StringBuilder = new mutable.StringBuilder
    alertMsg.append(alertTimeFormatter.format(timeStamp))

    alertMsg.append (',')

    alertMsg.append ("Unexpected state detected,")
    alertMsg.append(s"Required state $requiredState is missing for $key while opening state ${transition.state}")

    alertMsg.append(",analytic,StateMonitor")

    alertMsg.append(",config,")
    alertMsg.append(configFileBasename)

    if (transition.eventid.isDefined && transition.streamid.isDefined)
      alertMsg.append (s",eventref,${transition.streamid.get}:${transition.eventid.get}")

    alertMsg.append(s",key,$key")

    alertMsg.append(s",state,${transition.state}")

    alertMsg.append(s",requiredState,$requiredState")

    transition.tag1.foreach((v) => {
      alertMsg.append(s",${config.tags.tag1},${v}")
    })
    transition.tag2.foreach((v) => {
      alertMsg.append(s",${config.tags.tag2},${v}")
    })
    transition.tag3.foreach((v) => {
      alertMsg.append(s",${config.tags.tag3},${v}")
    })

    alertMsg.append ('\n')

    val alert = alertMsg.toString()

    if (timeStamp.isAfter(reportAlertsAfter)) {
      printf(alert)

      if (alertFilename != null) {
        val alertFile = new FileWriter(alertFilename, true)
        alertFile.write(alert)
        alertFile.flush()
        alertFile.close()
      }
    } else {
      printf ("Suppressing alert: %s", alertMsg)
    }
  }

  def canThin(eventTime: Instant, lastBatchTime: Instant): Boolean ={
    lastBatchTime.isAfter(eventTime.plus(eventThinningDelay))
  }

  def thinTransitionsAndOldAlerts(keyState: KeyState) :KeyState = {
    //Test harness needs alerts to be retained in order to assess results after execution
    var thinnedAlerts = keyState.previouslyAlerted

    if (!retainAlerts) //Normal (non-test) operation is always to thin
      thinnedAlerts = keyState.previouslyAlerted.filter(a => !canThin(Instant.ofEpochMilli(a.timestamp.getTime),
        Instant.ofEpochMilli(keyState.lastRun.getOrElse(Timestamp.from(Instant.now())).getTime)))

    KeyState(
      keyState.transitions.filter(t => !canThin(Instant.ofEpochMilli(t.timestamp.getTime),
        Instant.ofEpochMilli(keyState.lastRun.getOrElse(Timestamp.from(Instant.now())).getTime))),
      thinnedAlerts,
      keyState.lastRun,
      keyState.lastWarn
    )
  }

  def collateAndValidate(key: String, unsortedKeyState: KeyState, timestamp: Timestamp = null) : KeyState = {
    if (verboseTrace) {
      printf ("Collating and validating %s\n", key)
    }

    val keyState = KeyState(unsortedKeyState.transitions.distinct
      .sortWith((a, b)=> a.timestamp.compareTo(b.timestamp) < 1),
      unsortedKeyState.previouslyAlerted,
      unsortedKeyState.lastRun, unsortedKeyState.lastWarn)

    validateStates (key, keyState, Option(timestamp).getOrElse(new Timestamp(System.currentTimeMillis())))


  }

  def initialiseConfig (configFile : File, retainAlerts : Boolean = false) : Integer = {
    this.retainAlerts = retainAlerts

    printf("StateMonitor: Reading config from %s\n", configFile.getCanonicalPath)

    val pathTokens : Array[String] = configFile.getCanonicalPath.split('/')

    configFileBasename = pathTokens(pathTokens.length - 1)

    val mapper = new ObjectMapper(new YAMLFactory)
    mapper.registerModule(DefaultScalaModule)
    config = mapper.readValue(configFile, classOf[Config])

    alertFilename = config.alertFile

    stateMap = config.states.groupBy(_.name).mapValues(_.head).map(identity)

    interval = Option(config.interval).getOrElse(DEFAULT_INTERVAL)

    if (! interval.matches("^\\d+ \\w+$"))
      throw new IllegalArgumentException("interval should be a time interval (both number and unit are required), e.g. 150 seconds, or 1 hour")

    stateLatencyMap = stateLatencyMap ++ config.states.map(x => x.name -> Duration.parse(Option(x.maxlatency).getOrElse(DEFAULT_TIMEOUT)))

    reportingDelayDuration = Duration.parse (config.alertingDelay)

    val globalMaxLatency = stateLatencyMap.values.toList.sortWith((a,b)=>a.getSeconds > b.getSeconds).head
    eventThinningDelay = globalMaxLatency.plus(globalMaxLatency).plus(reportingDelayDuration)

    reportAlertsAfter = Instant.now().plus(reportingDelayDuration)
    printf ("Alerts will be suppressed prior to %s\n", reportAlertsAfter)

    closeStateAmbiguityStrategyMap = closeStateAmbiguityStrategyMap  ++
      config.states.map(x => {

        if (x.close.multipleMatchStrategy == null) {
          x.name -> WARN_STRATEGY
        } else if (x.close.multipleMatchStrategy == POP_ALL_STRATEGY) {
          x.name -> POP_ALL_STRATEGY
        } else if (x.close.multipleMatchStrategy == POP_STRATEGY) {
          x.name -> POP_STRATEGY
        } else {
          printf("ERROR: Property multipleMatchStrategy must be one of "
            + POP_ALL_STRATEGY + ", "
            + POP_STRATEGY + ", or "
            + WARN_STRATEGY)
          x.name -> WARN_STRATEGY
        }
      })

    config.states.length
  }

  val updateState = (key: String, rows: Iterator[RowDetails], groupState: GroupState[KeyState]) => {

    if (verboseTrace) {
      printf("Updating state for %s\n", key)
    }

    if (groupState.hasTimedOut) { // If called when timing out, remove the state

      if (groupState.exists) {

        val keyState = collateAndValidate(key, groupState.get)

        if (verboseTrace) {
          printf("There are now %d transitions\n ", keyState.transitions.length)
          printf("Tag1 is %s ", keyState.transitions.filter(_.tag1.isDefined).headOption match { case Some(x) => x.tag1 case None => "Undefined" })
          printf("Tag2 is %s ", keyState.transitions.filter(_.tag2.isDefined).headOption match { case Some(x) => x.tag2 case None => "Undefined" })
          printf("Tag3 is %s \n", keyState.transitions.filter(_.tag3.isDefined).headOption match { case Some(x) => x.tag3 case None => "Undefined" })

        }

        if (keyState.transitions.isEmpty)
          groupState.remove()
        else
          groupState.update(keyState)

        groupState.setTimeoutDuration(interval)
        keyState
      }
      else {
        groupState.setTimeoutDuration(interval)
        KeyState(Nil, Nil, None)
      }

    } else {
      groupState.setTimeoutDuration(interval)

      val keyState = groupState.getOption.getOrElse(KeyState(Nil, Nil, None))

      val updatedKeyState = KeyState(keyState.transitions ++ rows.map(row =>
        StateTransition(row.state, row.timestamp, row.open,
          Option (row.eventid), Option (row.streamid),
          Option(row.tag1), Option(row.tag2), Option(row.tag3))),
        keyState.previouslyAlerted,
        keyState.lastRun)

      if (verboseTrace) {
        printf("There are now %d transitions\n", updatedKeyState.transitions.length)
      }

      groupState.update(updatedKeyState)
      updatedKeyState
    }
  }

  def run (args: Array[String]): Unit = {



    if (args.length == 0){
      printf ("Please specify the path of the yaml configuration file as argument.  This should be the last argument to spark-submit.")
      return
    }
    printf ("Initialising StateMonitor with config file %s...\n", args.head)

    val configFile = new File(args.head)

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

    printf("Attempting to read from topic %s from bootstrap servers %s\n", config.topic, config.bootstrapServers)
      val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", config.bootstrapServers)
    .option("subscribe", config.topic)
    .option("startingOffsets", "latest")
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

      //Add eventid and stream id columns (if defined)
      if (config.eventId != null) {
        updatedDf = updatedDf.withColumn(EVENT_ID_COLUMN_NAME, col(config.eventId))
        if (config.streamId != null)
          updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, col(config.streamId))
        else
          updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, lit(null))
      }else{
        updatedDf = updatedDf.withColumn(EVENT_ID_COLUMN_NAME, lit(null))
        updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, lit(null))
      }

      updatedDf
    }
    )

    //There is not necessarily a close for each state (can be null)
    val closes : Seq [Option[DataFrame]] = config.states.map (state => {
      val closeOpt = Option (state)
      closeOpt match {
        case Some(x)=> {
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

          //Add eventid and stream id columns (if defined)
          if (config.eventId != null) {
            updatedDf = updatedDf.withColumn(EVENT_ID_COLUMN_NAME, col(config.eventId))
            if (config.streamId != null)
              updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, col(config.streamId))
            else
              updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, lit(null))
          }else{
            updatedDf = updatedDf.withColumn(EVENT_ID_COLUMN_NAME, lit(null))
            updatedDf = updatedDf.withColumn(STREAM_ID_COLUMN_NAME, lit(null))
          }

          Option(updatedDf)
        }
        case None => {
          None
        }
      }

    }
    )


    def unionize = (x : DataFrame, y : DataFrame) => x.union(y)

    val allDfs = opens ++ closes.flatten

    val unionDf = allDfs.reduce (unionize)

    val mappedGroups = unionDf.as[RowDetails].groupByKey(_.key)
        .mapGroupsWithState (GroupStateTimeout.ProcessingTimeTimeout)(updateState)

    printf("Starting. Alerts will be written to %s\n", alertFilename)
    val query = mappedGroups.writeStream
          .outputMode("update")
          .format("memory")
          .queryName("ueba")
         .start()
    query.awaitTermination()
  }
}

object StateMonitor extends Serializable {
  val sm = new StateMonitor

  def main(args: Array[String]): Unit = {

    sm.run(args)
  }


}
