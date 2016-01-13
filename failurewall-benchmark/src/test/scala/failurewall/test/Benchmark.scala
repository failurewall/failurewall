package failurewall.test

import failurewall.Failurewall
import org.scalameter._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}

trait Benchmark extends Bench.LocalTime {
  protected[this] val sizes = Gen.exponential("body")(10000, 1000000, 10)

  protected[this] val lowLatencyGen: Gen[Seq[Future[Int]]] = sizes.map { x =>
    Seq.fill(x)(Future(Random.nextInt()))
  }

  protected[this] def await[A](future: Future[A]): Try[A] = Try(Await.result(future, 10.seconds))
}

trait FailurewallBenchmark extends Benchmark {
  protected[this] val failurewall: Failurewall[Int, Int]

  performance of s"${getClass.getName.replace("$", "")}(sequential execution)" in {
    measure method "call(low latency)" in {
      using(lowLatencyGen) in { bodies =>
        bodies.foreach(await)
      }
    }
  }
}
