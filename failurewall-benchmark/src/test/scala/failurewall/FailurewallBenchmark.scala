package failurewall

import failurewall.test.Benchmark

object FailurewallBenchmark extends Benchmark {
  val failurewall = Failurewall.identity[Int]

  performance of "Failurewall(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies.foreach { body =>
          await(failurewall.call(body))
        }
      }
    }
  }
}
