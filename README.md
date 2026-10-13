# Failurewall

This is a library to protect applications against failures, and helpful in developing stable, responsive and resilient systems.

Failurewall is inspired by [Hystrix](https://github.com/Netflix/Hystrix) and adapted for `scala.concurrent.Future`.

## Getting Started

You should add the following dependency.

```
libraryDependencies += "com.okumin" %% "failurewall-core" % "0.5.0"
```

For applications using Apache Pekko, `failurewall-pekko` provides the following failurewalls.

- circuit breaker
- retry with backoff
- timeout

To use the Pekko module from this checkout, publish it and its core dependency locally:

```sh
sbt "show failurewallPekko / version" publishLocal
```

The command publishes for the build's default Scala version. If your application uses a different Scala version, select it with `++<scala-version>` before running these commands.
Use the version printed by sbt in your application's build:

```scala
libraryDependencies += "com.okumin" %% "failurewall-pekko" % "<version printed by sbt>"
```

## How to use

### As a Proxy

Failurewall is simple to use, wrapping `scala.concurrent.Future` to be protected.
Each failurewall has abilities to handle failures.

```scala
object HttpClient {
  def get(url: String): Future[Response] = ???
}

val wall: Failurewall[Response, Response] = ???
val response: Future[Response] = wall.call(HttpClient.get("http://okumin.com/"))
```

### Composability

Failurewalls has their own ability, e.g. retrying, checking rate limits and throttling.
`Failurewall#compose` makes Failurewalls decorate such features.

```scala
val wallSina: Failurewall[Int, String] = ???
val wallRose: Failurewall[Int, Int] = ???
val wallMaria: Failurewall[Double, Int] = ???

val walls: [Double, String] = wallSina compose wallRose compose wallMaria
```

## Built-in walls(failurewall-core)

### [RetryFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-core/src/main/scala/failurewall/retry/RetryFailurewall.scala)

Retries on temporary failures.

```scala
val wall = RetryFailurewall[Response](10, executionContext)
wall.call(Future.failed(new RuntimeException)) // retry 10 times
```

### [StdSemaphoreFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-core/src/main/scala/failurewall/semaphore/StdSemaphoreFailurewall.scala)

Keeps resource usage constant.

```scala
val wall = StdSemaphoreFailurewall[Response](10, executionContext)
val results = (1 to 100).map { _ =>
  // fails immediately while other 10 calls are running
  wall.call(doSomeOperation())
}
```

### [StopwatchFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-core/src/main/scala/failurewall/stopwatch/StopwatchFailurewall.scala)

Measures the execution time.

```scala
val wall = StopwatchFailurewall[Response](executionContext)
wall.call(doSomeOperation()) // returns Future[(Try[Response], FiniteDuration)]
```

## Built-in walls(failurewall-pekko)

These walls use Apache Pekko's scheduler, obtained from your actor system:

```scala
val pekkoScheduler: org.apache.pekko.actor.Scheduler = actorSystem.scheduler
```

### [PekkoCircuitBreakerFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-pekko/src/main/scala/failurewall/circuitbreaker/PekkoCircuitBreakerFailurewall.scala)

Prevents a failure from leading to cascading other failures.

```scala
// has a little complicated constructor
val wall: PekkoCircuitBreakerFailurewall[Response] = ???
val results = (1 to 100).map { _ =>
  // fail-fast after failure times exceeds the threshold
  wall.call(Future {
    throw new RuntimeException
  })
}
```

### [PekkoRetryFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-pekko/src/main/scala/failurewall/retry/PekkoRetryFailurewall.scala)

Retries with backoff on temporary failures.

```scala
val backoffStrategy = ExponentialBackoffStrategy(
  minBackoff = 100.millis,
  maxBackoff = 10.seconds,
  multiplier = 2.0
)
val wall = PekkoRetryFailurewall[Response](
  10,
  backoffStrategy,
  pekkoScheduler,
  executionContext
)
// retry 10 times with exponential backoff
wall.call(Future.failed(new RuntimeException))
```

### [PekkoTimeoutFailurewall](https://github.com/failurewall/failurewall/blob/main/failurewall-pekko/src/main/scala/failurewall/timeout/PekkoTimeoutFailurewall.scala)

Times out when it takes some duration.

```scala
val wall = PekkoTimeoutFailurewall[String](5.seconds, pekkoScheduler, executionContext) {
  logger.error("Timed out.")
}
// fails with FailurewallException
wall.call(Future {
  Thread.sleep(10000)
  "mofu"
})
```
