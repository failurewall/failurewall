package failurewall.test

import java.util.concurrent.Executors
import org.scalatest.WordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scala.concurrent.ExecutionContext

trait WallSpec extends WordSpec with ScalaCheckDrivenPropertyChecks {
  implicit protected[this] def executor: ExecutionContext = WallSpec.executor
}

object WallSpec {
  private val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(256))
}
