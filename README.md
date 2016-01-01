# Failurewall

[![Build Status](https://travis-ci.org/failurewall/failurewall.svg?branch=master)](https://travis-ci.org/failurewall/failurewall)

This is a library to protect applications against failures, and helpful in developing stable, responsive and resilient systems.

Failurewall is inspired by [Hystrix](https://github.com/Netflix/Hystrix) and adapted for `scala.concurrent.Future`.

## Getting Started

You should add the following dependency.

```
libraryDependencies += "com.okumin" %% "failurewall-core" % "0.0.1"
```

If you are using Akka 2.4 and would like to use `AkkaCircuitBreakerFailurewall`, add also

```
libraryDependencies += "com.okumin" %% "failurewall-akka" % "0.0.1"
```

## How to use

### As a Proxy

Failurewall is simple to use, wrapping `scala.concurrent.Future` to be protected.
Each failurewall has abilities to handle failures.

```scala
object HttpClient {
  def get(url: String): Future[Response] = ???
}

val failurewall: Failurewall[Response, Response] = ???
val response: Future[Response] = failurewall.call(HttpClient.get("http://okumin.com/"))
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

## Build-in walls

This library provides some of Failurewalls to handle failures.

### Retry

RetriableFailurewall lets requests recover from temporary failures.

```scala
val executor: ExecutionContext = ???
val failurewall = RetriableFailurewall[Response](10, executor)
failure.call(Future.failed(new RuntimeException)) // retry 10 times
```

### Semaphore

StdSemaphoreFailurewall keeps resource usage constant.

```scala
val executor: ExecutionContext = ???
val failurewall = StdSemaphoreFailurewall[Response](10, executor)
val results = (1 to 100).map { _ =>
  // fails immediately while 10 calls are blocking
  failurewall.call(Promise[Response].future)
}
```

### Circuit breaker

AkkaCircuitBreakerFailurewall prevents a failure from leading to cascading other failures.

```scala
val executor: ExecutionContext = ???
// has a little complicated constructor
val failurewall: AkkaCircuitBreakerFailurewall[Response] = ???
val results = (1 to 100).map { _ =>
  // fail-fast after failure times exceeds the threshold
  failurewall.call(Future {
    Thread.sleep(1000)
    throw new RuntimeException
  })
}
```

### For microservices

MicroserviceFailurewall consists of a circuit breaker, a semaphore and a retry pattern.

```scala
val failurewall = MicroserviceFailurewall[Response](???, ???, ???, ???)
// protected by the composed wall
failurewall.call(HttpRequest.get("http://okumin.com/"))
```
