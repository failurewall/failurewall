package failurewall

import failurewall.test.FailurewallBenchmark

object FailurewallBenchmark extends FailurewallBenchmark {
  override protected[this] val failurewall: Failurewall[Int, Int] = Failurewall.identity
}
