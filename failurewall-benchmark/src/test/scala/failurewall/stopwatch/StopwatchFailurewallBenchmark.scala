package failurewall.stopwatch

import failurewall.Failurewall
import failurewall.test.FailurewallBenchmark
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class StopwatchFailurewallBenchmark extends FailurewallBenchmark[(Try[Int], FiniteDuration)] {
  override protected[this] val failurewall: Failurewall[Int, (Try[Int], FiniteDuration)] = {
    StopwatchFailurewall[Int](executionContext)
  }
}
