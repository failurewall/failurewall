package failurewall.test

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll

trait AkkaSpec
  extends WallSpec
  with BeforeAndAfterAll {

  val system = ActorSystem("akka-circuit-breaker-failure-spec")

  override protected def afterAll(): Unit = TestHelper.await(system.terminate())
}
