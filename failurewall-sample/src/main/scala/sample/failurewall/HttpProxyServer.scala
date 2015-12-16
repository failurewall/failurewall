package sample.failurewall

import akka.actor.ActorSystem
import failurewall.MicroserviceFailurewall
import failurewall.MicroserviceFailurewall.{CircuitBreakerConfig, RetryConfig, SemaphoreConfig}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

object HttpProxyServer {
  val client = new HttpClient
  val system = ActorSystem("http-proxy-server")
  val failurewall = MicroserviceFailurewall[Response](
    CircuitBreakerConfig[Response](
      system.scheduler,
      maxFailures = 3,
      callTimeout = 10.seconds,
      resetTimeout = 5.seconds,
      feedback = {
        case Success(Response(500, _)) => false
        case Success(Response(503, _)) => false
        case _ => true
      }
    ),
    SemaphoreConfig(permits = 10),
    RetryConfig[Response](
      maxTrialTimes = 5,
      feedback = {
        case Success(Response(500, _)) => false
        case _ => true
      }
    ),
    global
  )

  def get(url: String): Future[Response] = {
    failurewall.call(client.get(url))
  }
}
