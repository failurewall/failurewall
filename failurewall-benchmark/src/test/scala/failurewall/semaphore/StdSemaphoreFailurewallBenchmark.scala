package failurewall.semaphore

import failurewall.Failurewall
import failurewall.test.FailurewallBenchmark

class StdSemaphoreFailurewallBenchmark extends FailurewallBenchmark {
  override protected[this] val failurewall: Failurewall[Int, Int] = StdSemaphoreFailurewall(
    permits = Int.MaxValue,
    executionContext
  )
}
