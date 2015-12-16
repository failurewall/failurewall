package failurewall

import scala.concurrent.Future

/**
 * A [[Failurewall]] wraps and decorates calls.
 *
 * A [[Failurewall]] is finally made up of some Failurewalls by `compose`.
 *
 * {{{
 *   val circuitBreaker: Failurewall[Response] = ???
 *   val semaphore: Failurewall[Response] = ???
 *   val failurewall = circuitBreaker.compose(semaphore)
 * }}}
 */
trait Failurewall[A, B] {
  /**
   * Wraps a call in this [[Failurewall]] and calls it if possible.
   * @param body call to wrap
   * @return the result of `body` with this [[Failurewall]]
   */
  def call(body: => Future[A]): Future[B]

  final def compose[S](rhs: Failurewall[S, A]): Failurewall[S, B] = {
    val lhs = this
    new Failurewall[S, B] {
      override def call(body: => Future[S]): Future[B] = lhs.call(rhs.call(body))
    }
  }
}

object Failurewall {
  private[this] final class NopFailurewall[A] extends Failurewall[A, A] {
    override def call(body: => Future[A]): Future[A] = body
  }

  def identity[A]: Failurewall[A, A] = new NopFailurewall[A]
}
