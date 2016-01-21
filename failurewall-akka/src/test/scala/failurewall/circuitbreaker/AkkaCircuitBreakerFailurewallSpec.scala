package failurewall.circuitbreaker

import akka.pattern.CircuitBreaker
import failurewall.FailurewallException
import failurewall.test.{AkkaSpec, BodyPromise, TestHelper}
import java.util.concurrent.atomic.AtomicBoolean
import org.scalacheck.Gen
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class AkkaCircuitBreakerFailurewallSpec extends AkkaSpec {

  private[this] def circuitBreaker(maxFailure: Int = 3,
                                   callTimeout: FiniteDuration = 10.seconds,
                                   resetTimeout: FiniteDuration = 10.seconds): CircuitBreaker = {
    CircuitBreaker(system.scheduler, maxFailure, callTimeout, resetTimeout)
  }

  "AkkaCircuitBreakerFailurewall" when {
    "in CLOSED state" should {
      "invoke the given body" in {
        forAll { result: Int =>
          val breaker = circuitBreaker()
          val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
          val body = BodyPromise[Int]()
          val actual = failurewall.call(body.future)
          body.success(result)
          assert(TestHelper.await(actual) === Success(result))
          assert(body.invokedCount === 1)
        }
      }

      "fail with the exception with which the given body fails" in {
        val breaker = circuitBreaker()
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        val error = new RuntimeException
        body.failure(error)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === 1)
      }

      "fail with the exception that the given body throws" in {
        val breaker = circuitBreaker()
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        val error = new RuntimeException
        val actual = failurewall.call(throw error)
        assert(TestHelper.await(actual) === Failure(error))
      }

      "fail with scala.concurrent.TimeoutException if the given body fails with that error" in {
        val breaker = circuitBreaker()
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        val error = new scala.concurrent.TimeoutException
        body.failure(error)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === 1)
      }

      "fail with FailurewallException if calling the body times out" in {
        val breaker = circuitBreaker(callTimeout = 1.nano)
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        intercept[FailurewallException] {
          TestHelper.await(actual).get
        }
        assert(body.invokedCount === 1)
      }

      "switch into the OPEN state if too many failures occurs" in {
        forAll(Gen.chooseNum(1, 10)) { failureTimes: Int =>
          val breaker = circuitBreaker(maxFailure = failureTimes)
          val isOpen = new AtomicBoolean(false)
          breaker.onOpen(isOpen.set(true))
          val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
          (1 to failureTimes).foreach { _ =>
            val error = new RuntimeException
            val actual = failurewall.call(Future.failed(error))
            assert(TestHelper.await(actual) === Failure(error))
          }
          TestHelper.awaitAssert(isOpen.get())

          val actual = failurewall.call(Promise[Int]().future)
          intercept[FailurewallException] {
            TestHelper.await(actual).get
          }
        }
      }

      "reset failure counter if the given body succeeds" in {
        val failureTimes = 5
        val breaker = circuitBreaker(maxFailure = failureTimes)
        val isOpen = new AtomicBoolean(false)
        breaker.onOpen(isOpen.set(true))
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        (1 to failureTimes - 1).foreach { _ =>
          val error = new RuntimeException
          val actual = failurewall.call(Future.failed(error))
          assert(TestHelper.await(actual) === Failure(error))
        }
        assert(!isOpen.get())

        TestHelper.sleep() // It is possible for callbacks to be reordered

        {
          val actual = failurewall.call(Future(10))
          assert(TestHelper.await(actual) === Success(10))
          assert(!isOpen.get())
        }

        (1 to failureTimes - 1).foreach { _ =>
          val error = new RuntimeException
          val actual = failurewall.call(Future.failed(error))
          assert(TestHelper.await(actual) === Failure(error))
        }
        TestHelper.assertWhile(!isOpen.get())

        val error = new RuntimeException
        val actual = failurewall.call(Future.failed(error))
        assert(TestHelper.await(actual) === Failure(error))
        TestHelper.awaitAssert(isOpen.get())
      }

      "increase the failure count if the given feedback return false" in {
        val breaker = circuitBreaker(maxFailure = 1)
        val isOpen = new AtomicBoolean(false)
        breaker.onOpen(isOpen.set(true))
        val failurewall = AkkaCircuitBreakerFailurewall.withFeedback[Int](breaker, executor) {
          case Success(10) => Unavailable
          case Success(_) => Available
          case Failure(_) => Unavailable
        }

        val actual = failurewall.call(Future(10))
        assert(TestHelper.await(actual) === Success(10))
        TestHelper.awaitAssert(isOpen.get())
      }
    }

    "in OPEN state" should {
      "never call the given body and fail immediately" in {
        val breaker = circuitBreaker(maxFailure = 1)
        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        TestHelper.await(failurewall.call(Future.failed(new RuntimeException)))
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        intercept[FailurewallException] {
          TestHelper.await(actual).get
        }
        assert(!body.isInvoked)
      }

      "switch into the HALF-OPEN state after the configured timeout" in {
        val breaker = circuitBreaker(maxFailure = 1, resetTimeout = 100.millis)
        val isOpen = new AtomicBoolean(false)
        val isHalfOpen = new AtomicBoolean(false)
        breaker.onOpen(isOpen.set(true))
        breaker.onHalfOpen(isHalfOpen.set(true))

        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        failurewall.call(Future.failed(new RuntimeException))
        TestHelper.awaitAssert(isOpen.get())
        assert(!isHalfOpen.get())

        TestHelper.awaitAssert(isHalfOpen.get(), 200.millis)
      }
    }

    "in HALF-OPEN state" should {
      "the first call is executed and the other calls are never executed" in {
        val breaker = circuitBreaker(maxFailure = 2, resetTimeout = 1.nanos)
        val isHalfOpen = new AtomicBoolean(false)
        breaker.onHalfOpen(isHalfOpen.set(true))

        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        failurewall.call(Future.failed(new RuntimeException))
        failurewall.call(Future.failed(new RuntimeException))
        TestHelper.awaitAssert(isHalfOpen.get())

        val bodies = (1 to 5).map { _ => BodyPromise[Int]() }
        val results = bodies.map { body => failurewall.call(body.future) }
        bodies.zip(results).tail.foreach {
          case (body, result) =>
            intercept[FailurewallException] {
              TestHelper.await(result).get
            }
            assert(!body.isInvoked)
        }
        assert(!results.head.isCompleted)

        bodies.head.success(10)
        assert(TestHelper.await(results.head) === Success(10))
        assert(bodies.head.invokedCount === 1)
      }

      "switch into the CLOSED state if the first call succeeds" in {
        val breaker = circuitBreaker(maxFailure = 1, resetTimeout = 1.nanos)
        val isHalfOpen = new AtomicBoolean(false)
        breaker.onHalfOpen(isHalfOpen.set(true))

        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        failurewall.call(Future.failed(new RuntimeException))
        TestHelper.awaitAssert(isHalfOpen.get())

        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        val isClosed = new AtomicBoolean(false)
        breaker.onClose(isClosed.set(true))

        intercept[FailurewallException] {
          TestHelper.await(failurewall.call(throw new RuntimeException)).get
        }

        body.success(10)
        assert(TestHelper.await(actual) === Success(10))
        assert(body.invokedCount === 1)
        TestHelper.awaitAssert(isClosed.get())
      }

      "switch into the OPEN state if the first call fails" in {
        val breaker = circuitBreaker(maxFailure = 1, resetTimeout = 1.nanos)
        val isHalfOpen = new AtomicBoolean(false)
        breaker.onHalfOpen(isHalfOpen.set(true))

        val failurewall = AkkaCircuitBreakerFailurewall[Int](breaker, executor)
        failurewall.call(Future.failed(new RuntimeException))
        TestHelper.awaitAssert(isHalfOpen.get())

        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        val isOpen = new AtomicBoolean(false)
        breaker.onOpen(isOpen.set(true))

        val error = new RuntimeException
        body.failure(error)
        assert(TestHelper.await(actual) === Failure(error))
        assert(body.invokedCount === 1)
        TestHelper.awaitAssert(isOpen.get())
      }
    }
  }
}
