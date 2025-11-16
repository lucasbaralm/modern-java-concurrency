# modern-java-concurrency

A focused overview of concurrency and threads as used in this repository.

This project demonstrates different concurrency approaches in modern Java with emphasis on platform threads and virtual threads (Project Loom). The document below explains key concepts used in the code, how the sample services exercise threading, a short note about benchmarking results included in the project, and credits to the original author.

## Overview

The code in this repository shows examples of:
- Creating and running platform threads and virtual threads.
- Blocking vs non-blocking patterns and how virtual threads help with blocking IO.
- Using `Thread.join()` to coordinate and wait for thread completion.
- Simple benchmarking (using `ab`) to compare behavior and latency under concurrent load.

## Threads and join()

A thread represents a unit of execution. Java historically used platform (OS) threads. Project Loom introduces virtual threads which are lightweight user-mode threads that allow far higher concurrency for blocking workloads.

`Thread.join()` is a synchronization method that causes the calling thread to wait until the target thread has finished. In practice:
- Calling `t.join()` blocks the caller until `t` terminates (or the caller is interrupted).
- `join()` throws `InterruptedException` so callers should handle interruption and typically restore the interrupt status with `Thread.currentThread().interrupt()`.

Example (conceptual):

```java
Thread t1 = new Thread(() -> doWork());
Thread t2 = new Thread(() -> doOtherWork());

t1.start();
t2.start();

try {
    t1.join(); // wait for t1
    t2.join(); // wait for t2
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

// both threads finished here
```

Why use `join()`:
- To ensure results produced by other threads are ready before continuing.
- To provide deterministic ordering in shutdown or finalization sequences.

Edge cases:
- If a thread is a daemon, the JVM may exit before non-daemon callers join it.
- Long or deadlocked threads will indefinitely block a join; consider timeouts or interruption.

## Virtual threads (Project Loom)

Virtual threads are extremely lightweight and well-suited for workloads that perform blocking I/O. They enable you to create many concurrent handlers without exhausting OS thread resources.

In Spring Boot this project includes a toggle to enable virtual threads in `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

Benefits shown in the examples:
- When handling many concurrent blocking requests, virtual threads allow throughput and latency improvements compared to a small pool of platform threads.
- Simpler programming model — keep blocking code but scale with many virtual threads.

## Services and example endpoints in this repo

This project contains a small remote service and a client to exercise blocking behavior. Example endpoints used by the benchmark and demos:

- GET /currentThread — returns information about the thread processing the request (useful to observe whether the request ran on a virtual thread).
- GET /remote/{id} — a small remote service endpoint used by the client examples.
- GET /blocking/{seconds} — client endpoint which performs a blocking call to the remote service for the specified number of seconds. This endpoint is used for benchmarking comparisons.

Example quick checks (curl):

curl -i http://localhost:8080/currentThread
curl -i http://localhost:8085/remote/2
curl -i http://localhost:8080/blocking/1

## Benchmarking summary (high level)

Benchmarks in the original repository used `ab` (ApacheBench) to compare virtual threads vs platform threads using a constrained tomcat thread pool (for clearer comparison). Key takeaways from the included results:
- Virtual threads handled blocking IO more efficiently, reducing average request latency and total time for batches of concurrent requests.
- With many concurrent blocking requests, platform-thread-based configurations with small thread pools showed much higher per-request latency and longer total completion time.

Typical `ab` commands used in the repo's examples:

ab -n 20 -c 10 http://localhost:8080/blocking/2
ab -n 60 -c 20 http://localhost:8080/blocking/2

See the code and the included sample outputs in the original README for exact numbers; the project includes measured example outputs comparing virtual vs platform threads.

## Notes and credits

- This repository accompanies course materials used to explore concurrency and threads.
- Credit: original repository and author — dilipsundarraj1 (https://github.com/dilipsundarraj1). The repository preserves the original commit history which attributes the original work.
