package failurewall.retry

import failurewall.Failurewall
import failurewall.test.{ActorSystemSetup, FailurewallBenchmark}
import scala.concurrent.duration._

class PekkoRetryFailurewallBenchmark extends FailurewallBenchmark[Int] with ActorSystemSetup {
  override protected[this] val failurewall: Failurewall[Int, Int] = PekkoRetryFailurewall(
    maxTrialTimes = 5,
    strategy = ExponentialBackoffStrategy(minBackoff = 10.millis, maxBackoff = 100.millis),
    scheduler = system.scheduler,
    executor = executionContext
  )
}
