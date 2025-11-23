package com.modernjava.future;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CompletableFutureExamples {

    public static void main(String[] args) throws Exception {
        exampleSupplyThenApply();
        exampleThenCompose();
        exampleThenCombine();
        exampleAllOfAnyOf();
        exampleExceptionHandling();
        exampleApplyToEitherAndManualComplete();
    }

    // supplyAsync + thenApply + join
    static void exampleSupplyThenApply() {
        // Build a CompletableFuture pipeline:
        // 1) supplyAsync: submit a Supplier that returns "Hello" on the common ForkJoinPool.
        // 2) thenApply : synchronously transform the result by appending " world".
        // 3) join      : block and retrieve the final value or rethrow failure as an unchecked exception.
        String greeting = CompletableFuture
                .supplyAsync(() -> "Hello") // start async work that produces "Hello"
                .thenApply(s -> s + " world") // on completion, append " world" to the supplied value
                .join(); // block until complete and return the computed string (or throw if failed)
        // Print the computed greeting to standard output
        System.out.println("supplyThenApply: " + greeting);
    }

    // chaining dependent async calls with thenCompose
    static void exampleThenCompose() {
        // Start an async step that returns an id string.
        // thenCompose receives that id and starts another async task that depends on the id.
        CompletableFuture<String> cf = CompletableFuture
                .supplyAsync(() -> "user-42") // async supplier: pretend we looked up a user id
                .thenCompose(id -> CompletableFuture.supplyAsync(() -> "profile-for-" + id)); // chain dependent async task that builds a profile string
        // Block and print the result of the composed pipeline (both steps must finish)
        System.out.println("thenCompose: " + cf.join());
    }

    // combining two independent futures
    static void exampleThenCombine() {
        // Start two independent asynchronous computations in parallel.
        CompletableFuture<Integer> price = CompletableFuture.supplyAsync(() -> 100); // compute price
        CompletableFuture<Integer> tax = CompletableFuture.supplyAsync(() -> 20); // compute tax
        // thenCombine waits for both futures to complete and then applies the provided BiFunction (Integer::sum)
        Integer total = price.thenCombine(tax, Integer::sum).join(); // combine results and block for the total
        // Print the computed total value
        System.out.println("thenCombine total: " + total);
    }

    // allOf (wait all) and anyOf (first)
    static void exampleAllOfAnyOf() throws ExecutionException, InterruptedException {
        // Kick off two independent async tasks returning strings.
        CompletableFuture<String> a = CompletableFuture.supplyAsync(() -> "A"); // returns "A"
        CompletableFuture<String> b = CompletableFuture.supplyAsync(() -> "B"); // returns "B"
        // allOf returns a CompletableFuture<Void> that completes when all input futures complete.
        CompletableFuture<Void> all = CompletableFuture.allOf(a, b);
        all.join(); // block until both a and b have completed
        // After all completed we can safely obtain each result; join() rethrows exceptions as unchecked
        List<String> results = List.of(a.join(), b.join()); // collect results into an immutable list
        System.out.println("allOf results: " + results); // print the list [A, B]
        // anyOf returns a future that completes with whichever input future completes first (its result).
        CompletableFuture<Object> any = CompletableFuture.anyOf(
                // provide one slower task that sleeps first then returns "slow"
                CompletableFuture.supplyAsync(() -> {
                    sleep(200); // pause the current worker thread to simulate latency
                    return "slow"; // then return the slow result
                }),
                // provide a fast task that immediately returns "fast"
                CompletableFuture.supplyAsync(() -> "fast") // immediate fast result
        );
        // any.join() returns the first completed result (either "fast" or "slow" depending on timing)
        System.out.println("anyOf first: " + any.join());
    }

    // exception handling: exceptionally, handle, whenComplete
    static void exampleExceptionHandling() {
        // Create a future that intentionally throws a RuntimeException during computation.
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("boooom"); // always throw to demonstrate error handling
        });

        // exceptionally provides a fallback value when the upstream future completes exceptionally.
        // The lambda receives the Throwable and returns a replacement value for the pipeline.
        String recovered = cf.exceptionally(ex -> "recovered").join(); // on exception, recover with "recovered"
        System.out.println("exceptionally recovered: " + recovered);

        // handle receives both the result (if any) and the exception and returns a new value based on either.
        String handled = cf.handle((res, ex) -> ex != null ? "handled:" + ex.getMessage() : res).join();
        System.out.println("handle: " + handled);

        // whenComplete is used to observe completion and perform side-effects, it does not change the result.
        CompletableFuture<String> side = CompletableFuture.supplyAsync(() -> "value")
                .whenComplete((r, ex) -> System.out.println("whenComplete side effect, result=" + r + ", ex=" + ex));
        // The returned future from whenComplete completes with the original value ("value") after the side-effect runs.
        System.out.println("whenComplete returned: " + side.join());
    }

    // applying to either, manual complete, timeouts
    static void exampleApplyToEitherAndManualComplete() {
        // Create two futures: one slower (simulated delay) and one immediate.
        CompletableFuture<String> c1 = CompletableFuture.supplyAsync(() -> {
            // simulate work by sleeping then returning "first"
            sleep(150);
            return "first"; // value returned by the slower supplier
        });
        CompletableFuture<String> c2 = CompletableFuture.supplyAsync(() -> "second"); // immediate supplier returns "second"
        // applyToEither will apply the provided function to the result of whichever future completes first.
        String either = c1.applyToEither(c2, s -> "winner: " + s).join(); // map the first result to a "winner: ..." string
        System.out.println("applyToEither: " + either);

        // manual completion: create an unresolved CompletableFuture and complete it manually.
        CompletableFuture<String> manual = new CompletableFuture<>(); // create an empty future
        manual.complete("manually done"); // explicitly complete the future with a value
        System.out.println("manual complete: " + manual.join()); // join returns the manually completed value

        // completeOnTimeout returns a future that will be completed with the supplied default value if the supplier is too slow.
        CompletableFuture<String> to = CompletableFuture.supplyAsync(() -> {
            sleep(500); // simulate a slow supplier
            return "slow"; // would normally be returned if not timed out
        }).completeOnTimeout("timed-out-default", 200, TimeUnit.MILLISECONDS); // if supplier hasn't finished in 200ms, complete with default
        System.out.println("completeOnTimeout: " + to.join()); // print the resulting value (either slow or timed-out-default)
    }

    // small helper to sleep without throwing checked exceptions in the examples
    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
