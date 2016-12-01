package failurewall.timeout

import failurewall.Failurewall
import failurewall.test.{ActorSystemSetup, FailurewallBenchmark}
import scala.concurrent.duration._

class AkkaTimeoutFailurewallBenchmark extends FailurewallBenchmark[Int] with ActorSystemSetup {
  override protected[this] val failurewall: Failurewall[Int, Int] = AkkaTimeoutFailurewall(
    duration = 10.seconds,
    scheduler = system.scheduler,
    executor = executionContext
  )(())
}
