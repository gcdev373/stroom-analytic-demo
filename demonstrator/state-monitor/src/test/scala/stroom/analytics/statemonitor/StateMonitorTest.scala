package stroom.analytics.statemonitor
import java.io.File
import java.sql.Timestamp
import java.time.Instant

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import stroom.analytics.statemonitor.StateMonitor.{KeyState, StateTransition}

import scala.collection.immutable.HashMap
import scala.collection.mutable

@RunWith(classOf[JUnitRunner])
class StateMonitorTest extends org.scalatest.FunSuite {
  val path = "build/resources/test/"

  val configFile = "ueba.yml"

  val test1Data = "test1.csv"
  val test2Data = "test2.csv"

  var key : String= null

  val stateMonitor = StateMonitor
  var test1 : Seq[StateTransition] = Nil

  var test2 = new HashMap[Timestamp, Seq[StateTransition]]()

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

  test ("It is possible to load the second load of test data"){

    val bufferedSource = scala.io.Source.fromFile(path + test2Data)

    var currentTimestamp = new Timestamp(System.currentTimeMillis())
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)

      if (fields.length == 1){
        currentTimestamp = new Timestamp(Instant.parse(fields(0)).toEpochMilli)
      }else {
        val key = fields(0)

        val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
        val open = "Login" == fields(2)
        test2 += currentTimestamp -> (StateTransition(key,timestamp,open) +: test2.getOrElse(currentTimestamp,Nil))
      }
    }
    bufferedSource.close

    assert (test1.length > 10)
  }

  test("Two states should be loaded from the config file") {

    stateMonitor.initialiseConfig(new File(path + configFile))

    assert(stateMonitor.config.states.length == 2)

  }

//  test ("A missing state will be identified"){
//
//    stateMonitor.collateAndValidate(key, KeyState(test1, None))
//  }

  test ("Data can be successfully built up incrementally and still only alerts once"){
    var keyState = KeyState(Nil, None)
    for (batchTime <- test2.keys){
      printf("Now processing batch %s\n", batchTime)
      keyState = KeyState(test2.get(batchTime).get ++ keyState.transitions, keyState.lastRun)
      stateMonitor.collateAndValidate(key, keyState, batchTime)
    }
  }

}
