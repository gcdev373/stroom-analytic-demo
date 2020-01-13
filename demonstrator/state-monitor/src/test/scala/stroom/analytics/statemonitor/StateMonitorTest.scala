package stroom.analytics.statemonitor
import java.io.File
import java.sql.Timestamp
import java.time.Instant

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

import scala.collection.immutable.HashMap
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class StateMonitorTest extends org.scalatest.FunSuite {
  val path = "build/resources/test/"

  val configFile = "ueba.yml"
  val configFileAccelerated = "ueba-accelerated.yml"

  val test1Data = "test1.csv"
  val test2Data = "test2.csv"
  val test3Data = "test3.csv"
  val test4Data = "test4.csv"

  var key : String= null

  val stateMonitor = new StateMonitor
  val stateMonitorAccelerated = new StateMonitor

  var test1 : Seq[StateTransition] = Nil

  var test2 = new HashMap[Timestamp, Seq[StateTransition]]()
  var test2withTag = new HashMap[Timestamp, Seq[StateTransition]]()
  var test2withTwoTags = new HashMap[Timestamp, Seq[StateTransition]]()
  var test3 = new HashMap[Timestamp, Seq[StateTransition]]()
  var test4 : Seq[StateTransition] = Nil

  test ("It is possible to load the test data"){

    val bufferedSource = scala.io.Source.fromFile(path + test1Data)
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)

      key = fields(3)
      val state = fields(0)
      val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
      val open = "Login" == fields(2)
      test1 = StateTransition(state,timestamp,open) +: test1
    }
    bufferedSource.close

    assert (key != null)
    assert (test1.length > 10)
  }

  test ("It is possible to load the second load of test data for batch sending test"){

    val bufferedSource = scala.io.Source.fromFile(path + test2Data)

    var currentTimestamp = new Timestamp(System.currentTimeMillis())
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)

      if (fields.length == 1){
        currentTimestamp = new Timestamp(Instant.parse(fields(0)).toEpochMilli)
      }else {
        val state = fields(0)

        val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
        val open = "Login" == fields(2)
        test2 += currentTimestamp -> (StateTransition(state,timestamp,open) +: test2.getOrElse(currentTimestamp,Nil))
        test2withTag += currentTimestamp -> (StateTransition(state,timestamp,open,
          ((fields.length > 4) match {case true=> Option(fields(4)) case false => None}))
          +: test2withTag.getOrElse(currentTimestamp,Nil))
        test2withTwoTags += currentTimestamp -> (StateTransition(state,timestamp,open,
          ((fields.length > 4) match {case true=> Option(fields(4)) case false => None}),
          ((fields.length > 5) match {case true=> Option(fields(5)) case false => None}))
          +: test2withTwoTags.getOrElse(currentTimestamp,Nil))
      }
    }
    bufferedSource.close

    assert (test2.keys.size > 3)
  }

  test ("It is possible to load the third load of test data for batch sending test"){

    val bufferedSource = scala.io.Source.fromFile(path + test3Data)

    var currentTimestamp = new Timestamp(System.currentTimeMillis())
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)

      if (fields.length == 1){
        currentTimestamp = new Timestamp(Instant.parse(fields(0)).toEpochMilli)
      }else {
        val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
        val open = "Login" == fields(2)

        test3 += currentTimestamp -> (StateTransition(fields(0),timestamp,open,
          ((fields.length > 4) match {case true=> Option(fields(4)) case false => None}),
          ((fields.length > 5) match {case true=> Option(fields(5)) case false => None}))
          +: test3.getOrElse(currentTimestamp,Nil))
      }
    }
    bufferedSource.close

    assert (test2.keys.size > 3)
  }

  test ("It is possible to load the fourth load of test data"){

    val bufferedSource = scala.io.Source.fromFile(path + test4Data)
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)

      assert (key == fields(3))

      val state = fields(0)
      val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
      val open = "Login" == fields(2)
      test4 = StateTransition(state,timestamp,open) +: test4
    }
    bufferedSource.close

    assert (test4.length > 10)
  }

  test("Two states should be loaded from the config file") {

    stateMonitor.initialiseConfig(new File(path + configFile))

    assert(stateMonitor.config.states.length == 2)

  }

  test("Two states should be loaded from the accelerated config file") {

    stateMonitorAccelerated.initialiseConfig(new File(path + configFileAccelerated))

    assert(stateMonitorAccelerated.config.states.length == 2)

  }

  test ("It is possible to use contains to find StateTransition objects within lists"){
    val list : Seq[StateTransition] = Nil :+ StateTransition("test", new Timestamp(System.currentTimeMillis()), true, Option("tag1"))

    assert (list.contains(StateTransition(list.head.state, list.head.timestamp, list.head.open, list.head.tag1)))
  }

  test ("Missing tags don't need to be matched when looking for matching state"){
    printf ("Missing tag test starting.\n")
    val keyState = stateMonitorAccelerated.collateAndValidate(key, KeyState(test4, Nil, None), test4.head.timestamp)

    assert (keyState.previouslyAlerted.length == 0)
  }

  test ("A missing state will be identified"){
    printf ("Basic missing state test started.\n")
    val keyState = stateMonitor.collateAndValidate(key, KeyState(test1, Nil, None), test1.head.timestamp)

    assert (keyState.previouslyAlerted.length == 1)
  }

  test ("Data can be successfully built up incrementally and still only alerts once"){
    printf ("Incremental missing state test started.\n")
    var keyState = KeyState(Nil, Nil, None)
    for (batchTime <- test2.keys.toList.sortBy(_.toInstant)){
      keyState = stateMonitor.collateAndValidate(key,
        KeyState(test2.get(batchTime).get ++ keyState.transitions, keyState.previouslyAlerted, keyState.lastRun),
        batchTime)

    }
    assert (keyState.previouslyAlerted.length == 1)
  }
  test ("Data can be successfully built up incrementally with tags and still only alerts once"){
    printf ("Incremental missing state with partial tags test started.\n")
    var keyState = KeyState(Nil, Nil, None)
    for (batchTime <- test2withTwoTags.keys.toList.sortBy(_.toInstant)){
      keyState = stateMonitor.collateAndValidate(key,
        KeyState(test2withTwoTags.get(batchTime).get ++ keyState.transitions, keyState.previouslyAlerted, keyState.lastRun),
        batchTime)

    }
    assert (keyState.previouslyAlerted.length == 1)
  }

  test ("Data can be successfully built up incrementally with additional tags and still only alerts once"){
    printf ("Incremental missing state test with tags started.\n")
    var keyState = KeyState(Nil, Nil, None)
    for (batchTime <- test2withTag.keys.toList.sortBy(_.toInstant)){
      keyState = stateMonitor.collateAndValidate(key,
        KeyState(test2withTag.get(batchTime).get ++ keyState.transitions, keyState.previouslyAlerted, keyState.lastRun),
        batchTime)

    }
    assert (keyState.previouslyAlerted.length == 1)
  }

  test ("Tags are checked when identifying states to close"){
    printf ("Tag checking on close state test started.\n")
    var keyState = KeyState(Nil, Nil, None)
    for (batchTime <- test3.keys.toList.sortBy(_.toInstant)){
      keyState = stateMonitor.collateAndValidate(key,
        KeyState(test3.get(batchTime).get ++ keyState.transitions, keyState.previouslyAlerted, keyState.lastRun),
        batchTime)

    }
    assert (keyState.previouslyAlerted.length == 2)
  }

  test ("States are closed when they time out"){
    printf ("Autoclose after timeout state test starting.\n")
    var keyState = KeyState(Nil, Nil, None)
    for (batchTime <- test3.keys.toList.sortBy(_.toInstant)){
      keyState = stateMonitor.collateAndValidate(key,
        KeyState(test3.get(batchTime).get ++ keyState.transitions.filter(t=>{t.state != 'vpn || !t.open}), keyState.previouslyAlerted, keyState.lastRun),
        batchTime)

    }
    assert (keyState.previouslyAlerted.length == 2)
  }


}
