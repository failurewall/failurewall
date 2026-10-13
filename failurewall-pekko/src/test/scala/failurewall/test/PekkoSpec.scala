package failurewall.test

import org.apache.pekko.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll

trait PekkoSpec
  extends WallSpec
  with BeforeAndAfterAll {

  val system = ActorSystem("pekko-circuit-breaker-failure-spec")

  override protected def afterAll(): Unit = TestHelper.await(system.terminate())
}
