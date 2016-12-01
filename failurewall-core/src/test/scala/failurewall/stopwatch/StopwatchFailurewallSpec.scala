package failurewall.stopwatch

import failurewall.test.{TestHelper, WallSpec}
import scala.concurrent.Future
import scala.util.{Failure, Success}

class StopwatchFailurewallSpec extends WallSpec {
  "StopwatchFailurewall" should {
    "measure the execution time" when {
      "the given body succeeds" in {
        val failurewall = StopwatchFailurewall[Int](executor)
        val Success((actual, _)) = TestHelper.await(failurewall.call(Future(10)))
        assert(actual === Success(10))
      }

      "the given body fails" in {
        val failurewall = StopwatchFailurewall[Int](executor)
        val error = new RuntimeException
        val Success((actual, _)) = TestHelper.await(failurewall.call(Future(throw error)))
        assert(actual === Failure(error))
      }

      "the given body throws an Exception" in {
        val failurewall = StopwatchFailurewall[Int](executor)
        val error = new RuntimeException
        val Success((actual, _)) = TestHelper.await(failurewall.call(throw error))
        assert(actual === Failure(error))
      }
    }
  }
}
