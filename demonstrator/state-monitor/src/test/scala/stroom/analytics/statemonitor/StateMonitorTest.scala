package stroom.analytics.statemonitor
import java.io.File
import java.sql.Timestamp
import java.time.Instant

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import stroom.analytics.statemonitor.StateMonitor.{KeyState, StateTransition}

@RunWith(classOf[JUnitRunner])
class StateMonitorTest extends org.scalatest.FunSuite {
  val path = "build/resources/test/"

  val configFile = "ueba.yml"

  val test1Data = "test1.csv"


  val stateMonitor = StateMonitor
  var test1 : Seq[StateTransition] = Nil

  test ("It is possible to load the test data"){

    val bufferedSource = scala.io.Source.fromFile(path + test1Data)
    for (line <- bufferedSource.getLines) {
      val fields = line.split(",").map(_.trim)
      val key = fields(0)
      val timestamp = new Timestamp(Instant.parse(fields(1)).toEpochMilli)
      val open = "Login" == fields(2)
      test1 = StateTransition(key,timestamp,open) +: test1
    }
    bufferedSource.close

    assert (test1.length > 10)
  }

  test("Two states should be loaded from the config file") {

    stateMonitor.initialiseConfig(new File(path + configFile))

    assert(stateMonitor.config.states.length == 2)

  }

  test ("Something happens when all the test data is piled through in one go"){

    stateMonitor.collateAndValidate(test1.head.state, KeyState(test1, None))
  }


}
