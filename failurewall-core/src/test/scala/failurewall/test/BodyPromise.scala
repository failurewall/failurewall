package failurewall.test

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Future, Promise}
import scala.util.Try

class BodyPromise[A] extends Promise[A] {
  private[this] val promise = Promise[A]()
  private[this] val counter = new AtomicInteger(0)

  override def future: Future[A] = {
    counter.incrementAndGet()
    promise.future
  }

  override def tryComplete(result: Try[A]): Boolean = promise.tryComplete(result)

  override def isCompleted: Boolean = promise.isCompleted

  def invokedCount: Int = counter.get()

  def isInvoked: Boolean = counter.get() != 0
}

object BodyPromise {
  def apply[A](): BodyPromise[A] = new BodyPromise[A]
}
