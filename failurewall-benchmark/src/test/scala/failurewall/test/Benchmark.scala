package failurewall.test

import failurewall.Failurewall
import org.scalameter._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Random, Try}

trait Benchmark extends Bench.LocalTime {
  implicit protected[this] def executionContext: ExecutionContext = global

  protected[this] val lowLatencySizes = Gen.exponential("body")(100, 10000, 10)

  protected[this] val lowLatencyGen: Gen[() => Iterator[Future[Int]]] = lowLatencySizes.map { x =>
    () => Iterator.fill(x)(Future(Random.nextInt()))
  }

  protected[this] val lowLatencyGens: Gen[Seq[() => Iterator[Future[Int]]]] = lowLatencyGen.map {
    x =>
      (1 to Runtime.getRuntime.availableProcessors()).map { _ => x }
  }

  protected[this] val highLatencySizes = Gen.exponential("body")(100, 10000, 10)

  protected[this] val highLatencyGen: Gen[() => Iterator[Future[Int]]] = highLatencySizes.map { x =>
    () => Iterator.fill(x) {
      Scheduler.after(30.millis)(Random.nextInt())
    }
  }

  protected[this] val highLatencyGens: Gen[Seq[() => Iterator[Future[Int]]]] = highLatencyGen.map {
    x =>
      (1 to Runtime.getRuntime.availableProcessors()).map { _ => x }
  }

  protected[this] def await[A](future: Future[A]): Try[A] = Try(Await.result(future, 10.seconds))
}

trait FailurewallBenchmark[A] extends Benchmark {
  protected[this] val failurewall: Failurewall[Int, A]

  performance of s"${getClass.getName}(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies().foreach { body =>
          await(failurewall.call(body)).get
        }
      }
    }
  }

  performance of s"${getClass.getName}(parallel execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGens) in { generators =>
        val result = generators.par.map { bodies =>
          bodies().foldLeft(Future.successful(())) { (acc, body) =>
            failurewall.call(body).map(_ => ())
          }
        }
        await(Future.sequence(result.toList)).get
      }
    }

    measure method "call(high latency)" in {
      using(highLatencyGens) in { generators =>
        val result = generators.par.map { bodies =>
          bodies().foldLeft(Future.successful(())) { (acc, body) =>
            failurewall.call(body).map(_ => ())
          }
        }
        await(Future.sequence(result.toList)).get
      }
    }
  }
}
