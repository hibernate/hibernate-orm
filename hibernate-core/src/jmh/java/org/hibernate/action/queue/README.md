# ActionQueue JMH Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks for Hibernate's ActionQueue implementations.

## Available Benchmarks

### 1. ActionQueueBenchmark.java
Compares Legacy vs Graph-based ActionQueue implementations for:
- Simple entity inserts
- Parent-child relationship inserts
- Self-referencing tree structures
- Mixed operations (insert + update + delete)

### 2. CollectionBundlingBenchmark.java
Compares Legacy, Graph (no bundling), and Graph (with bundling) for collection operations:
- Small collection inserts (10 items)
- Medium collection inserts (50 items)
- Large collection inserts (200 items)
- Multiple collections per entity
- Batch processing with collections
- Very large collections (500 items)
- Mixed operations

## Running Benchmarks

### Run All ActionQueue Benchmarks
```bash
./gradlew :hibernate-core:jmh
```

### Run Specific Benchmark Class
```bash
./gradlew :hibernate-core:jmh -Pjmh.include=".*ActionQueueBenchmark.*"
./gradlew :hibernate-core:jmh -Pjmh.include=".*CollectionBundlingBenchmark.*"
```

### Run Specific Benchmark Method
```bash
./gradlew :hibernate-core:jmh -Pjmh.include=".*collectionInsert_Small.*"
./gradlew :hibernate-core:jmh -Pjmh.include=".*collectionInsert_Medium.*"
./gradlew :hibernate-core:jmh -Pjmh.include=".*collectionInsert_Large.*"
```

### Run with Custom Parameters
```bash
# More iterations for higher confidence
./gradlew :hibernate-core:jmh -Pjmh.include=".*CollectionBundling.*" \
  -Djmh.iterations=20 \
  -Djmh.warmup=10

# Different fork count
./gradlew :hibernate-core:jmh -Pjmh.include=".*CollectionBundling.*" \
  -Djmh.fork=3
```

## Understanding Results

### Benchmark Modes

The benchmarks run in two modes:

1. **Throughput (ops/ms):** Operations per millisecond - **higher is better**
2. **Average Time (ms/op):** Milliseconds per operation - **lower is better**

### Sample Output
```
Benchmark                                                       Mode  Cnt   Score   Error   Units
CollectionBundlingBenchmark.collectionInsert_Small_Legacy      thrpt   10   0.237 ± 0.002  ops/ms
CollectionBundlingBenchmark.collectionInsert_Small_GraphBundling  thrpt   10   0.313 ± 0.004  ops/ms
CollectionBundlingBenchmark.collectionInsert_Small_Legacy       avgt   10   4.267 ± 0.072   ms/op
CollectionBundlingBenchmark.collectionInsert_Small_GraphBundling   avgt   10   3.144 ± 0.058   ms/op
```

Interpretation:
- Graph with bundling: **0.313 ops/ms** (32% faster throughput than legacy)
- Graph with bundling: **3.144 ms/op** (26% faster average time than legacy)

### Error Margins

The `±` value represents the 99.9% confidence interval. Smaller is better, indicating more consistent results.

## Results Location

Benchmark results are saved to:
```
hibernate-core/target/results/jmh/results.txt
```

## Benchmark Configuration

Default JMH settings (from `hibernate-core.gradle`):
```groovy
jmh {
    jmhVersion = '1.37'
    warmupIterations = 5
    iterations = 10
    fork = 1
}
```

### What These Mean

- **warmupIterations:** Number of warmup iterations to let JVM optimize code
- **iterations:** Number of measurement iterations for actual results
- **fork:** Number of separate JVM processes to run benchmarks in

## Tips for Accurate Benchmarking

1. **Close unnecessary applications** to reduce system noise
2. **Run multiple times** and compare results for consistency
3. **Don't run other heavy tasks** during benchmarking
4. **Use profilers** to understand why performance differs:
   ```bash
   ./gradlew :hibernate-core:jmh -Pjmh.include=".*YourBenchmark.*" -Djmh.prof=gc
   ```

5. **Compare similar workloads** (same collection size, same operation type)

## Adding New Benchmarks

### Basic Structure
```java
@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 1)
public class MyBenchmark {

    @State(Scope.Benchmark)
    public static class MyState {
        SessionFactory sessionFactory;

        @Setup(Level.Trial)
        public void setup() {
            // Initialize session factory
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            // Cleanup
        }
    }

    @Benchmark
    public void myBenchmark(MyState state, Blackhole blackhole) {
        // Benchmark code
        blackhole.consume(result);
    }
}
```

### State Scopes

- **Scope.Benchmark:** Shared across all threads in benchmark
- **Scope.Thread:** One instance per thread
- **Scope.Group:** One instance per thread group

### Setup/TearDown Levels

- **Level.Trial:** Once per benchmark run
- **Level.Iteration:** Once per iteration
- **Level.Invocation:** Once per invocation (use sparingly, adds overhead)

## Profiling

JMH includes several profilers:

```bash
# GC profiler
./gradlew :hibernate-core:jmh -Pjmh.include=".*MyBench.*" -Djmh.prof=gc

# Stack profiler
./gradlew :hibernate-core:jmh -Pjmh.include=".*MyBench.*" -Djmh.prof=stack

# Class loader profiler
./gradlew :hibernate-core:jmh -Pjmh.include=".*MyBench.*" -Djmh.prof=cl

# List all available profilers
./gradlew :hibernate-core:jmh -Pjmh.include=".*MyBench.*" -Djmh.prof=help
```

## Common Issues

### OutOfMemoryError
Increase heap size:
```bash
export GRADLE_OPTS="-Xmx4g"
./gradlew :hibernate-core:jmh
```

### Inconsistent Results
- Increase warmup iterations
- Run with more forks
- Close background applications

### Slow Benchmarks
- Reduce iterations for quick testing
- Use specific benchmark includes
- Comment out slow benchmarks during development

## References

- [JMH Home](https://github.com/openjdk/jmh)
- [JMH Samples](https://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
- [How to Write Good Benchmarks](https://psy-lob-saw.blogspot.com/p/jmh-related-posts.html)

## Result Documentation

- [JMH_BENCHMARK_RESULTS.md](JMH_BENCHMARK_RESULTS.md) - Detailed collection bundling results
- [BENCHMARK_RESULTS.md](../../../test/java/org/hibernate/orm/test/action/queue/bundling/BENCHMARK_RESULTS.md) - Simple benchmark results
