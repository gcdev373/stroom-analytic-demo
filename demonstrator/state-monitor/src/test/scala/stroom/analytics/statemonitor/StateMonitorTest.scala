package stroom.analytics.statemonitor
import java.io.File

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class StateMonitorTest extends org.scalatest.FunSuite {
  val configFile = "build/resources/test/ueba.yml"

  test("Two states should be loaded from the config file") {

    val stateMonitor = StateMonitor


    stateMonitor.initialiseConfig(new File(configFile))


    assert(stateMonitor.config.states.length == 2)

  }
}
