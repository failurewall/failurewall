package failurewall.test

import akka.actor.ActorSystem
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, WordSpec}

trait AkkaSpec
  extends WordSpec
  with GeneratorDrivenPropertyChecks
  with BeforeAndAfterAll {

  val system = ActorSystem("akka-circuit-breaker-failure-spec")

  override protected def afterAll(): Unit = TestHelper.await(system.terminate())
}
