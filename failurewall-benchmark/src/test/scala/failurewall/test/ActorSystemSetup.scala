package failurewall.test

import akka.actor.ActorSystem

trait ActorSystemSetup {
  def system: ActorSystem = ActorSystemSetup.system
}

object ActorSystemSetup {
  val system = ActorSystem("failurewall-benchmark")
}
