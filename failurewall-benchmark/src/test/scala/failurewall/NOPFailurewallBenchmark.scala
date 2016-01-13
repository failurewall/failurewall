package failurewall

import failurewall.test.FailurewallBenchmark

class NOPFailurewallBenchmark extends FailurewallBenchmark {
  override protected[this] val failurewall: Failurewall[Int, Int] = Failurewall.identity
}
