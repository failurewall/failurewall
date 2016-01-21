package failurewall

import akka.actor.ActorSystem
import failurewall.MicroserviceFailurewall.{CircuitBreakerConfig, RetryConfig, SemaphoreConfig}
import failurewall.circuitbreaker.{Available, Unavailable}
import failurewall.retry.{ShouldNotRetry, ShouldRetry}
import failurewall.test.{BodyPromise, TestHelper, WallSpec}
import org.scalatest.BeforeAndAfterAll
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class MicroserviceFailurewallSpec extends WallSpec with BeforeAndAfterAll {
  private[this] val system = ActorSystem("microservice-failurewall")

  override protected def afterAll(): Unit = TestHelper.await(system.terminate())

  private[this] def create[A](maxFailures: Int = 5,
                              permits: Int = 5,
                              maxTrialTimes: Int = 5)
                             (isSuccess: Try[A] => Boolean): Failurewall[A, A] = {
    MicroserviceFailurewall[A](
      CircuitBreakerConfig(
        system.scheduler,
        maxFailures,
        10.seconds,
        10.seconds,
        isSuccess.andThen {
          case true => Available
          case false => Unavailable
        }
      ),
      SemaphoreConfig(permits),
      RetryConfig(maxTrialTimes, isSuccess.andThen {
        case true => ShouldNotRetry
        case false => ShouldRetry
      }),
      executor
    )
  }

  "MicroserviceFailurewall" should {
    "invoke the given body" in {
      val failurewall = create[Int]()(_.isSuccess)
      val body = BodyPromise[Int]()
      val actual = failurewall.call(body.future)
      body.success(10)
      assert(TestHelper.await(actual) === Success(10))
      TestHelper.assertWhile(body.invokedCount === 1)
    }

    "retry calling the given body if it fails" in {
      val failurewall = create[Int](maxTrialTimes = 10)(_.isSuccess)
      val body = BodyPromise[Int]()
      val actual = failurewall.call(body.future)

      val error = new RuntimeException
      body.failure(error)
      assert(TestHelper.await(actual) === Failure(error))
      TestHelper.assertWhile(body.invokedCount === 10)
    }

    "restrict simultaneous invocation by the semaphore" in {
      val failurewall = create[Int](permits = 3)(_.isSuccess)
      val bodies = (1 to 3).map { _ => BodyPromise[Int]() }
      val results = bodies.map { body => failurewall.call(body.future) }

      val failedBody = BodyPromise[Int]()
      val actual = failurewall.call(failedBody.future)
      intercept[FailurewallException] {
        TestHelper.await(actual).get
      }
      TestHelper.assertWhile(!failedBody.isInvoked)

      assert(results.forall(!_.isCompleted))
      bodies.zip(results).zipWithIndex.foreach {
        case ((body, result), i) =>
          body.success(i)
          assert(TestHelper.await(result) === Success(i))
      }
      bodies.foreach { body =>
        TestHelper.assertWhile(body.invokedCount === 1)
      }
    }

    "fail immediately by the circuit breaker when the resource is unavailable" in {
      val failurewall = create[Int](maxFailures = 1)(_.isSuccess)
      TestHelper.await(failurewall.call(Future.failed(new RuntimeException)))
      TestHelper.sleep()

      val body = BodyPromise[Int]()
      val actual = failurewall.call(body.future)
      body.success(10)

      intercept[FailurewallException] {
        TestHelper.await(actual).get
      }
      TestHelper.assertWhile(!body.isInvoked)
    }

    "retry if the feedback returns false" in {
      val failurewall = create[Int](maxFailures = 1, maxTrialTimes = 5) {
        case Success(10) => false
        case v => v.isSuccess
      }

      {
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        body.success(10)
        assert(TestHelper.await(actual) === Success(10))
        assert(body.invokedCount === 5)
      }

      {
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        body.success(10)
        intercept[FailurewallException] {
          TestHelper.await(actual).get
        }
        assert(!body.isInvoked)
      }
    }
  }
}
