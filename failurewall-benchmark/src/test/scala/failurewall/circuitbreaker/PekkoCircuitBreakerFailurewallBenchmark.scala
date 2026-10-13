package failurewall.circuitbreaker

import failurewall.Failurewall
import failurewall.test.{ActorSystemSetup, FailurewallBenchmark}
import scala.concurrent.duration._

class PekkoCircuitBreakerFailurewallBenchmark extends FailurewallBenchmark[Int] with ActorSystemSetup {
  override protected[this] val failurewall: Failurewall[Int, Int] = PekkoCircuitBreakerFailurewall(
    system.scheduler,
    maxFailures = 5,
    callTimeout = 10.seconds,
    resetTimeout = 10.seconds,
    executionContext
  )
}
