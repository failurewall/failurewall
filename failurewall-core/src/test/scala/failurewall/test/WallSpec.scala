package failurewall.test

import java.util.concurrent.Executors
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.concurrent.ExecutionContext

trait WallSpec extends WordSpec with GeneratorDrivenPropertyChecks {
  implicit protected[this] def executor: ExecutionContext = WallSpec.executor
}

object WallSpec {
  val executor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(256))
}
