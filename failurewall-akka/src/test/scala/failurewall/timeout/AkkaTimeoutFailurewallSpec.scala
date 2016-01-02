package failurewall.timeout

import failurewall.FailurewallException
import failurewall.test.{AkkaSpec, BodyPromise, TestHelper}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class AkkaTimeoutFailurewallSpec extends AkkaSpec {
  "AkkaTimeoutFailurewall" should {
    "call the given body and return the result" in {
      val counter = new AtomicInteger(0)
      val failurewall = AkkaTimeoutFailurewall[Int](10.millis, system.scheduler, global) {
        counter.incrementAndGet()
      }
      val promise = BodyPromise[Int]()
      val actual = failurewall.call(promise.future)
      promise.success(10)
      assert(TestHelper.await(actual) === Success(10))
      TestHelper.assertWhile(promise.invokedCount === 1)
      TestHelper.assertWhile(counter.get() === 0, 300.millis)
    }

    "call the given body and return a timeout error and onTimeout is invoked" in {
      val counter = new AtomicInteger(0)
      val failurewall = AkkaTimeoutFailurewall[Int](1.millis, system.scheduler, global) {
        counter.incrementAndGet()
      }
      val promise = BodyPromise[Int]()
      val actual = failurewall.call(promise.future)
      intercept[FailurewallException] {
        TestHelper.await(actual).get
      }
      TestHelper.assertWhile(promise.invokedCount === 1)
      TestHelper.assertWhile(counter.get() === 1, 300.millis)
    }

    "not invoke onTimeout even if the given body fails with FailurewallException" in {
      val counter = new AtomicInteger(0)
      val failurewall = AkkaTimeoutFailurewall[Int](10.millis, system.scheduler, global) {
        counter.incrementAndGet()
      }

      val Failure(error) = TestHelper.await(failurewall.call(Promise[Int]().future))
      assert(error.isInstanceOf[FailurewallException])
      assert(counter.get() === 1)

      val actual = failurewall.call(Future.failed(error))
      intercept[FailurewallException] {
        TestHelper.await(actual).get
      }
      TestHelper.assertWhile(counter.get() === 1, 300.millis)
    }
  }
}
