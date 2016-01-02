package failurewall.util

import scala.concurrent.Future
import scala.util.control.NonFatal

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
}
