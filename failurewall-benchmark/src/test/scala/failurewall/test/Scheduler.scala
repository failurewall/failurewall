package failurewall.test

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object Scheduler {
  private[this] val system = ActorSystem("benchmark-scheduler")

  def after[A](duration: FiniteDuration)(value: => A): Future[A] = {
    import system.dispatcher
    akka.pattern.after(duration, system.scheduler)(Future(value))
  }
}
