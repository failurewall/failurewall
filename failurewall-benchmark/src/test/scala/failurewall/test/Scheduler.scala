package failurewall.test

import org.apache.pekko.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Scheduler {
  private[this] val system = ActorSystem("benchmark-scheduler")

  def after[A](duration: FiniteDuration)(value: => A): Future[A] = {
    import system.dispatcher
    org.apache.pekko.pattern.after(duration, system.scheduler)(Future(value))
  }
}
