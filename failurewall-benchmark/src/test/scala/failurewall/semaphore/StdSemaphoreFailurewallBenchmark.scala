package failurewall.semaphore

import failurewall.Failurewall
import failurewall.test.FailurewallBenchmark
import scala.concurrent.ExecutionContext.global

object StdSemaphoreFailurewallBenchmark extends FailurewallBenchmark {
  override protected[this] val failurewall: Failurewall[Int, Int] = StdSemaphoreFailurewall(
    permits = 100,
    global
  )
}
