package failurewall.test

import org.scalameter.{Bench, Gen}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}

trait Benchmark extends Bench.LocalTime {
  protected[this] val sizes = Gen.exponential("body")(10000, 1000000, 10)

  protected[this] val lowLatencyGen: Gen[Iterator[Future[Int]]] = sizes.map { x =>
    Iterator.fill(x)(Future(Random.nextInt()))
  }

  protected[this] def await[A](future: Future[A]): Try[A] = Try(Await.result(future, 10.seconds))
}
