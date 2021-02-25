package failurewall.retry

import failurewall.test.{BodyPromise, TestHelper, WallSpec}
import java.util.concurrent.atomic.AtomicInteger
import org.scalacheck.Gen
import scala.concurrent.Future
import scala.util.{Failure, Success}

class RetryFailurewallSpec extends WallSpec {
  private[this] class RetriableBody(successfulTrial: Int) {
    private[this] val n = new AtomicInteger(0)

    def future: Future[Int] = n.incrementAndGet() match {
      case `successfulTrial` => Future.successful(successfulTrial)
      case _ => Future.failed(new RuntimeException)
    }
  }

  "RetryFailurewall" should {
    "throws IllegalArgumentException if `maxTrialTimes` is less than or equal to 0" in {
      forAll(Gen.chooseNum(Int.MinValue, 0)) { (times: Int) =>
        intercept[IllegalArgumentException] {
          new RetryFailurewall[Int](times, _ => ShouldNotRetry, executor)
        }
      }
    }

    "finish if it succeeds calling body" in {
      forAll(Gen.chooseNum(1, 10)) { (successfulTrial: Int) =>
        val body = new RetriableBody(successfulTrial)
        val failurewall = RetryFailurewall[Int](20, executor)
        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Success(successfulTrial))
      }
    }

    "retry until it retries max trial times" in {
      forAll(Gen.chooseNum(1, 10)) { (times: Int) =>
        val failurewall = RetryFailurewall[Int](times, executor)
        val body = BodyPromise[Int]()
        val error = new RuntimeException
        body.failure(error)

        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === times)
      }
    }

    "fail with the exception if the given body throws a exception" in {
      forAll(Gen.chooseNum(1, 10)) { (times: Int) =>
        val failurewall = RetryFailurewall[Int](times, executor)
        val error = new RuntimeException
        val actual = failurewall.call(throw error)
        assert(TestHelper.await(actual) === Failure(error))
      }
    }

    "retry if feedback returns false" in {
      forAll(Gen.chooseNum(1, 10)) { (times: Int) =>
        val failurewall = RetryFailurewall.withFeedback[Int](times, executor) {
          case Success(10) => ShouldRetry
          case Success(_) => ShouldNotRetry
          case Failure(_) => ShouldRetry
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
