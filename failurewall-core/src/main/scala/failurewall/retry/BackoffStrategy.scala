package failurewall.retry

import java.util.concurrent.ThreadLocalRandom
import scala.concurrent.duration.FiniteDuration

/**
 * A strategy to compute a backoff.
 */
trait BackoffStrategy {
  /**
   * Calculates the next backoff.
   * @param trialTimes number of times of failed execution. 1 if it is the first retrial
   *                   `trialTimes` must be greater than 0
   * @return duration to delay
   */
  def nextDelay(trialTimes: Int): FiniteDuration
}

/**
 * A strategy that always returns the same backoff.
 * @param duration backoff
 */
final class ConstantBackoffStrategy(duration: FiniteDuration) extends BackoffStrategy {
  override def nextDelay(trialTimes: Int): FiniteDuration = duration
}

object ConstantBackoffStrategy {
  /**
   * Creates a [[ConstantBackoffStrategy]].
   * @param duration backoff
   */
  def apply(duration: FiniteDuration): ConstantBackoffStrategy = {
    new ConstantBackoffStrategy(duration)
  }
}

/**
 * A strategy to calculate an exponential backoff.
 *
 * The formula is as follows.
 * The randomValue value is [1.0, 1.0 + `randomFactor`].
 *
 * {{{
 *   minBackoff * (multiplier ^ (trialTimes - 1)) * randomValue
 * }}}
 *
 * However, `nextDelay` returns `maxBackoff` * `randomFactor`
 * if the calculated backoff exceeds `maxBackoff`.
 *
 * @param minBackoff minimum backoff for the first retrial
 * @param maxBackoff maximum backoff
 * @param multiplier multiplier for exponential backoff
 * @param randomFactor factor to randomize backoff duration
 */
final class ExponentialBackoffStrategy(minBackoff: FiniteDuration,
                                       maxBackoff: FiniteDuration,
                                       multiplier: Double,
                                       randomFactor: Double) extends BackoffStrategy {

  require(minBackoff.length >= 0, s"minBackoff($minBackoff) must be greater than or equal to 0.")
  require(
    minBackoff <= maxBackoff,
    s"minBackoff($minBackoff) must be smaller than or equal to maxBackoff($maxBackoff)."
  )
  require(multiplier >= 1.0, s"multiplier($multiplier) must be greater than or equal to 1.0.")
  require(randomFactor >= 0.0, s"randomFactor($randomFactor) must be greater than or equal to 0.0.")

  override def nextDelay(trialTimes: Int): FiniteDuration = {
    require(trialTimes > 0, s"trialTimes($trialTimes) must be greater than 0.")
    val random = 1.0 + ThreadLocalRandom.current().nextDouble(1.0) * randomFactor
    val exponential = minBackoff * math.pow(multiplier, trialTimes - 1)
    (maxBackoff min exponential) * random match {
      case f: FiniteDuration => f
      case _ => maxBackoff
    }
  }
}

object ExponentialBackoffStrategy {
  /**
   * Creates an [[ExponentialBackoffStrategy]].
   * @param minBackoff minimum backoff for the first retrial
   * @param maxBackoff maximum backoff
   * @param multiplier multiplier for exponential backoff
   * @param randomFactor factor to randomize backoff duration
   */
  def apply(minBackoff: FiniteDuration,
            maxBackoff: FiniteDuration,
            multiplier: Double = 1.5,
            randomFactor: Double = 0.5): ExponentialBackoffStrategy = {
    new ExponentialBackoffStrategy(minBackoff, maxBackoff, multiplier, randomFactor)
  }
}
