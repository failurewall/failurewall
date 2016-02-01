package failurewall.test

import failurewall.Failurewall
import org.scalameter._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Random, Try}

trait Benchmark extends Bench.LocalTime {
  implicit protected[this] def executionContext: ExecutionContext = global

  protected[this] val sizes = Gen.exponential("body")(100, 10000, 10)

  protected[this] val lowLatencyGen: Gen[() => Iterator[Future[Int]]] = sizes.map { x =>
    () => Iterator.fill(x)(Future(Random.nextInt()))
  }

  protected[this] def await[A](future: Future[A]): Try[A] = Try(Await.result(future, 10.seconds))
}

trait FailurewallBenchmark extends Benchmark {
  protected[this] val failurewall: Failurewall[Int, Int]

  performance of s"${getClass.getName}(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies().foreach(await(_).get)
      }
    }
  }
}
