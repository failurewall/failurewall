package failurewall.test

import org.apache.pekko.actor.ActorSystem

trait ActorSystemSetup {
  def system: ActorSystem = ActorSystemSetup.system
}

object ActorSystemSetup {
  val system = ActorSystem("failurewall-benchmark")
}
