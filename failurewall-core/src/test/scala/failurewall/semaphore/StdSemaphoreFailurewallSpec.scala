package failurewall.semaphore

import failurewall.FailurewallException
import failurewall.test.{BodyPromise, TestHelper}
import java.util.concurrent.Semaphore
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Promise
import scala.util.{Failure, Success}

class StdSemaphoreFailurewallSpec extends WordSpec with GeneratorDrivenPropertyChecks {
  "StdSemaphoreFailurewall" when {
    "permits are available" should {
      "invoke the given body" in {
        forAll { result: Int =>
          val semaphore = new Semaphore(3)
          val failurewall = StdSemaphoreFailurewall[Int](semaphore, global)
          val body = BodyPromise[Int]()
          val actual = failurewall.call(body.future)

          assert(!actual.isCompleted)
          TestHelper.awaitAssert(semaphore.availablePermits() === 2)

          body.success(result)
          assert(TestHelper.await(actual) === Success(result))
          TestHelper.awaitAssert(semaphore.availablePermits() === 3)
          assert(body.invokedCount === 1)
        }
      }

      "invoke the given failed body" in {
        val semaphore = new Semaphore(3)
        val failurewall = StdSemaphoreFailurewall[Int](semaphore, global)
        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)

        assert(!actual.isCompleted)
        TestHelper.awaitAssert(semaphore.availablePermits() === 2)

        val error = new RuntimeException
        body.failure(error)
        assert(TestHelper.await(actual) === Failure(error))
        TestHelper.awaitAssert(semaphore.availablePermits() === 3)
        assert(body.invokedCount === 1)
      }

      "not leak permits even if the given body throws an exception" in {
        val semaphore = new Semaphore(3)
        val failurewall = StdSemaphoreFailurewall[Int](semaphore, global)
        val error = new RuntimeException
        val actual = failurewall.call(throw error)
        assert(TestHelper.await(actual) === Failure(error))
        TestHelper.awaitAssert(semaphore.availablePermits() === 3)
      }

      "be able to invoke the number of permits at the same time and release a permit after the invocation" in {
        forAll(Gen.nonEmptyListOf(Arbitrary.arbInt.arbitrary), Arbitrary.arbInt.arbitrary) {
          (results: Seq[Int], trial: Int) =>
            val size = results.size
            val semaphore = new Semaphore(size)
            val failurewall = StdSemaphoreFailurewall[Int](semaphore, global)
            val bodies = (1 to size).map { _ => Promise[Int]() }
            val futures = bodies.map { p => failurewall.call(p.future) }
            assert(futures.forall(!_.isCompleted))

            {
              val actual = failurewall.call(Promise[Int]().future)
              intercept[FailurewallException] {
                TestHelper.await(actual).get
              }
              TestHelper.awaitAssert(semaphore.availablePermits() === 0)
            }

            {
              bodies.head.success(results.head)
              TestHelper.awaitAssert(semaphore.availablePermits() === 1)

              val promise = Promise[Int]()
              val actual = failurewall.call(promise.future)
              promise.success(trial)
              assert(TestHelper.await(actual) === Success(trial))
            }

            bodies.zip(results).tail.foreach {
              case (promise, result) => promise.success(result)
            }
            futures.zip(results).foreach {
              case (future, result) =>
                assert(TestHelper.await(future) === Success(result))
            }
        }
      }
    }

    "permits are not available" should {
      "never call body and fail with FailurewallException" in {
        val semaphore = new Semaphore(1)
        val failurewall = StdSemaphoreFailurewall[Int](semaphore, global)
        failurewall.call(Promise[Int]().future)

        val body = BodyPromise[Int]()
        val actual = failurewall.call(body.future)
        intercept[FailurewallException] {
          TestHelper.await(actual).get
        }
        TestHelper.assertWhile(!body.isInvoked)
        TestHelper.awaitAssert(semaphore.availablePermits() === 0)
      }
    }
  }
}
