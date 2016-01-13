package failurewall.retry

import failurewall.Failurewall
import failurewall.test.{ActorSystemSetup, FailurewallBenchmark}
import scala.concurrent.duration._

class AkkaRetryFailurewallBenchmark extends FailurewallBenchmark with ActorSystemSetup {
  override protected[this] val failurewall: Failurewall[Int, Int] = AkkaRetryFailurewall(
    maxTrialTimes = 5,
    strategy = ExponentialBackoffStrategy(minBackoff = 10.millis, maxBackoff = 100.millis),
    scheduler = system.scheduler,
    executor = executionContext
  )
}
