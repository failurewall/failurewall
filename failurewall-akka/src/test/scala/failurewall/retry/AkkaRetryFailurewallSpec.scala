package failurewall.retry

import failurewall.test.{AkkaSpec, BodyPromise, TestHelper}
import java.util.concurrent.atomic.AtomicInteger
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.mock.MockitoSugar
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class AkkaRetryFailurewallSpec extends AkkaSpec with MockitoSugar {
  private[this] class RetriableBody(successfulTrial: Int) {
    private[this] val n = new AtomicInteger(0)

    def future: Future[Int] = n.incrementAndGet() match {
      case `successfulTrial` => Future.successful(successfulTrial)
      case _ => Future.failed(new RuntimeException)
    }
  }

  private[this] def mockedStrategy(duration: FiniteDuration = 0.millis): BackoffStrategy = {
    val strategy = mock[BackoffStrategy]
    when(strategy.nextDelay(any[Int])).thenReturn(duration)
    strategy
  }

  "AkkaRetryFailurewall" should {
    "throws IllegalArgumentException if `maxTrialTimes` is less than or equal to 0" in {
      forAll(Gen.chooseNum(Int.MinValue, 0)) { maxTrialTimes: Int =>
        intercept[IllegalArgumentException] {
          new AkkaRetryFailurewall[Int](
            maxTrialTimes,
            ConstantBackoffStrategy(1.milli),
            system.scheduler,
            _ => ShouldNotRetry,
            executor
          )
        }
      }
    }

    "finish if it succeeds calling body" in {
      forAll(Gen.chooseNum(1, 10)) { successfulTrial: Int =>
        val strategy = mockedStrategy()
        val failurewall = AkkaRetryFailurewall[Int](20, strategy, system.scheduler, executor)

        val body = new RetriableBody(successfulTrial)
        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Success(successfulTrial))
        (1 until successfulTrial).foreach { i =>
          verify(strategy).nextDelay(i)
        }
        verifyNoMoreInteractions(strategy)
      }
    }

    "retry until it retries max trial times" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val strategy = mockedStrategy()
        val failurewall = AkkaRetryFailurewall[Int](times, strategy, system.scheduler, executor)

        val body = BodyPromise[Int]()
        val error = new RuntimeException
        body.failure(error)

        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === times)
        (1 until times).foreach { i =>
          verify(strategy).nextDelay(i)
        }
        verifyNoMoreInteractions(strategy)
      }
    }

    "fail with the exception if the given body throws a exception" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val strategy = mockedStrategy()
        val failurewall = AkkaRetryFailurewall[Int](times, strategy, system.scheduler, executor)

        val error = new RuntimeException
        val actual = failurewall.call(throw error)
        assert(TestHelper.await(actual) === Failure(error))
        (1 until times).foreach { i =>
          verify(strategy).nextDelay(i)
        }
        verifyNoMoreInteractions(strategy)
      }
    }

    "retry if feedback returns false" in {
      forAll(Gen.chooseNum(1, 10)) { times: Int =>
        val strategy = mockedStrategy()
        val failurewall = AkkaRetryFailurewall.withFeedback[Int](
          times,
          strategy,
          system.scheduler,
          executor
        ) {
          case Success(10) => ShouldRetry
          case Success(_) => ShouldNotRetry
          case Failure(_) => ShouldRetry
        }

        val body = BodyPromise[Int]().success(10)
        val actual = failurewall.call(body.future)
        assert(TestHelper.await(actual) === Success(10))
        assert(body.invokedCount === times)
        (1 until times).foreach { i =>
          verify(strategy).nextDelay(i)
        }
        verifyNoMoreInteractions(strategy)
      }
    }

    "delay retrials by backoff" in {
      val start = System.currentTimeMillis()

      val strategy = mockedStrategy(200.millis)
      val failurewall = AkkaRetryFailurewall[Int](5, strategy, system.scheduler, executor)

      val body = BodyPromise[Int]()
      val error = new RuntimeException
      body.failure(error)

      val actual = failurewall.call(body.future)
      assert(TestHelper.await(actual) === Failure(error))

      val end = System.currentTimeMillis()
      assert((end - start).millis >= 800.millis)
      assert((end - start).millis < 1000.millis)

      assert(body.invokedCount === 5)
      (1 until 5).foreach { i =>
        verify(strategy).nextDelay(i)
      }
      verifyNoMoreInteractions(strategy)
    }
  }
}
