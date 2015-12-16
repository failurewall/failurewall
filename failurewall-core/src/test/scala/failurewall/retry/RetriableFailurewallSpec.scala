package failurewall.retry

import failurewall.test.{BodyPromise, TestHelper}
import java.util.concurrent.atomic.AtomicInteger
import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class RetriableFailurewallSpec extends WordSpec with GeneratorDrivenPropertyChecks {
  private[this] class RetriableBody(successfulTrial: Int) {
    private[this] val n = new AtomicInteger(0)

    def future: Future[Int] = n.incrementAndGet() match {
      case `successfulTrial` => Future.successful(successfulTrial)
      case _ => Future.failed(new RuntimeException)
    }
  }

  "RetriableFailurewall" should {
    "throws IllegalArgumentException if `maxTrialTimes` is less than or equal to 0" in {
      forAll(Gen.chooseNum(Int.MinValue, 0)) { times: Int =>
        intercept[IllegalArgumentException] {
          new RetriableFailurewall[Int](times, _.isSuccess, global)
        }
        intercept[IllegalArgumentException] {
          RetriableFailurewall[Int](times, global)
        }
        intercept[IllegalArgumentException] {
          RetriableFailurewall.withFeedback[Int](times, global)(_.isSuccess)
        }
      }
    }

    "finish if it succeeds calling body" in {
      forAll(Gen.chooseNum(1, 10)) { successfulTrial: Int =>
        val body = new RetriableBody(successfulTrial)
        val failurewall = RetriableFailurewall[Int](20, global)
        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Success(successfulTrial))
      }
    }

    "retry until it retries max trial times" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val failurewall = RetriableFailurewall[Int](times, global)
        val body = BodyPromise[Int]()
        val error = new RuntimeException
        body.failure(error)

        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === times)
      }
    }

    "fail with the exception if the given body throws a exception" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val failurewall = RetriableFailurewall[Int](times, global)
        val error = new RuntimeException
        val actual = failurewall.call(throw error)
        assert(TestHelper.await(actual) === Failure(error))
      }
    }

    "retry if feedback returns false" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val failurewall = RetriableFailurewall.withFeedback[Int](times, global) {
          case Success(10) => false
          case v => v.isSuccess
        }
        val body = BodyPromise[Int]()
        body.success(10)

        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Success(10))
        assert(body.invokedCount === times)
      }
    }
  }
}
