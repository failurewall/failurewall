package failurewall.retry

import failurewall.test.DurationArbitrary._
import failurewall.test.WallSpec
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import scala.concurrent.duration._

class BackoffStrategySpec extends WallSpec {
  "ConstantBackoffStrategy" should {
    "always return the same duration" in {
      forAll { (duration: FiniteDuration, trialTimes: Int) =>
        val strategy = ConstantBackoffStrategy(duration)
        assert(strategy.nextDelay(trialTimes) === duration)
      }
    }
  }

  "ExponentialBackoffStrategy" should {
    "fail initializing if the given parameters are incorrect" in {
      ExponentialBackoffStrategy(1.millis, 1.millis, 1.0, 0.0)

      intercept[IllegalArgumentException] {
        ExponentialBackoffStrategy(-1.millis, 1.millis, 1.0, 0.0)
      }
      intercept[IllegalArgumentException] {
        ExponentialBackoffStrategy(2.millis, 1.millis, 1.0, 0.0)
      }
      intercept[IllegalArgumentException] {
        ExponentialBackoffStrategy(1.millis, 1.millis, 0.9, 0.0)
      }
      intercept[IllegalArgumentException] {
        ExponentialBackoffStrategy(1.millis, 1.millis, 1.0, -0.1)
      }
    }

    "calculate a exponential backoff" in {
      forAll(genFiniteDuration(1, 100), Gen.choose(1.0, 2.0), Gen.choose(0.0, 16.0), Gen.choose(1, 30)) {
        (minBackoff: FiniteDuration, multiplier: Double, randomFactor: Double, trialTimes: Int) =>
          val strategy = ExponentialBackoffStrategy(
            minBackoff,
            Long.MaxValue.nanos,
            multiplier,
            randomFactor
          )
          val actual = strategy.nextDelay(trialTimes)
          val minExpected = minBackoff * math.pow(multiplier, trialTimes - 1)
          val maxExpected = minExpected * (1 + randomFactor)

          assert(actual >= minExpected)
          assert(actual <= maxExpected)
      }
    }

    "return the maximum backoff if the exponential backoff exceeds the maximum" in {
      import failurewall.test.DurationArbitrary._
      forAll { (minBackoff: FiniteDuration, maxBackoff: FiniteDuration) =>
        whenever(minBackoff <= maxBackoff) {
          val strategy = ExponentialBackoffStrategy(
            minBackoff,
            maxBackoff,
            multiplier = 2.0,
            randomFactor = 0.0
          )
          val actual = strategy.nextDelay(Int.MaxValue)
          assert(actual === maxBackoff)
        }
      }
    }

    "throws IllegalArgumentException if the given trialTimes <= 0" in {
      forAll(Gen.choose(Int.MinValue, 0)) { (trialTimes: Int) =>
        val strategy = ExponentialBackoffStrategy(1.millis, 1.millis)
        intercept[IllegalArgumentException] {
          strategy.nextDelay(trialTimes)
        }
      }
    }

    "have default multiplier and random factor" in {
      val strategy = ExponentialBackoffStrategy(1.millis, 16.millis)
      assert(strategy.nextDelay(1) >= 1.millis)
      assert(strategy.nextDelay(1) <= 1.5.millis)
      assert(strategy.nextDelay(2) >= 1.5.millis)
      assert(strategy.nextDelay(2) <= (1.5 * 1.5).millis)
      assert(strategy.nextDelay(8) >= 16.millis)
      assert(strategy.nextDelay(8) <= (16 * 1.5).millis)
    }
  }
}
