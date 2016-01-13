package failurewall.retry

import failurewall.Failurewall
import failurewall.test.FailurewallBenchmark
import scala.concurrent.ExecutionContext.global

object RetryFailurewallBenchmark extends FailurewallBenchmark {
  override protected[this] val failurewall: Failurewall[Int, Int] = RetryFailurewall(
    maxTrialTimes = 10,
    global
  )
}
