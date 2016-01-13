package failurewall

import failurewall.test.{TestHelper, WallSpec}
import scala.concurrent.Future
import scala.util.Success

class FailurewallSpec extends WallSpec {
  "Failurewall" should {
    "be able to be mixed with another Failurewall" in {
      val wallSina = new Failurewall[Int, String] {
        override def call(body: => Future[Int]): Future[String] = body.map(_.toString)
      }
      val wallRose = new Failurewall[Int, Int] {
        override def call(body: => Future[Int]): Future[Int] = body.map { x => - x }
      }
      val wallMaria = new Failurewall[Double, Int] {
        override def call(body: => Future[Double]): Future[Int] = body.map(_.toInt)
      }

      val failurewall = wallSina compose wallRose compose wallMaria
      val actual = failurewall.call(Future(33.4))
      assert(TestHelper.await(actual) === Success("-33"))
    }
  }
}
