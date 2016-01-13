package failurewall.circuitbreaker

import failurewall.Failurewall
import failurewall.test.{ActorSystemSetup, FailurewallBenchmark}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration._

class AkkaCircuitBreakerFailurewallBenchmark extends FailurewallBenchmark with ActorSystemSetup {
  override protected[this] val failurewall: Failurewall[Int, Int] = AkkaCircuitBreakerFailurewall(
    system.scheduler,
    maxFailures = 5,
    callTimeout = 10.seconds,
    resetTimeout = 10.seconds,
    global
  )
}
