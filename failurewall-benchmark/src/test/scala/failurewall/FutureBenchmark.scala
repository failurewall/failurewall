package failurewall

import failurewall.test.Benchmark

object FutureBenchmark extends Benchmark {
  performance of "Future(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies.foreach(await)
      }
    }
  }
}
