package failurewall.retry

import failurewall.Failurewall
import failurewall.test.FailurewallBenchmark

class RetryFailurewallBenchmark extends FailurewallBenchmark[Int] {
  override protected[this] val failurewall: Failurewall[Int, Int] = RetryFailurewall(
    maxTrialTimes = 10,
    executionContext
  )
}
