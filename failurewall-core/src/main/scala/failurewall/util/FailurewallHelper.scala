package failurewall.util

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object FailurewallHelper {
  /**
   * Calls a body safely.
   */
  def callSafely[A](body: => Future[A]): Future[A] = {
    try {
      body
    } catch {
      case NonFatal(e) => Future.failed(e)
    }
  }

  /**
   * Maps a [[Future]]'s result to [[Try]].
   */
  def mapToTry[A](future: Future[A])(implicit executor: ExecutionContext): Future[Try[A]] = {
    future.map[Try[A]](Success.apply).recover { case e => Failure(e) }
  }
}
