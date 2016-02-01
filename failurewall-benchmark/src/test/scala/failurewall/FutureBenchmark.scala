package failurewall

import failurewall.test.Benchmark
import scala.concurrent.Future

class FutureBenchmark extends Benchmark {
  performance of "Future(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies().foreach(await(_).get)
      }
    }
  }

  performance of "Future(parallel execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGens) in { generators =>
        val result = generators.par.map { bodies =>
          bodies().foldLeft(Future.successful(())) { (acc, body) =>
            body.map(_ => ())
          }
        }
        await(Future.sequence(result.toList)).get
      }
    }

    measure method "call(high latency)" in {
      using(highLatencyGens) in { generators =>
        val result = generators.par.map { bodies =>
          bodies().foldLeft(Future.successful(())) { (acc, body) =>
            body.map(_ => ())
          }
        }
        await(Future.sequence(result.toList)).get
      }
    }
  }
}
