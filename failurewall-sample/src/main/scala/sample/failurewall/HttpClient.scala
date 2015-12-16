package sample.failurewall

import scala.concurrent.Future

case class Response(status: Int, body: String)

class HttpClient {
  def get(url: String): Future[Response] = {
    println(url)
    url match {
      case "not url" => Future.failed(new IllegalArgumentException)
      case "https://400.failure/" => Future.successful(Response(400, "BAD_REQUEST"))
      case "https://500.failure/" => Future.successful(Response(500, "INTERNAL_SERVER_ERROR"))
      case "https://maintenance.failure/" => Future.successful(Response(503, "MAINTENANCE"))
      case _ => Future.successful(Response(200, "OK"))
    }
  }
}
