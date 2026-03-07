# ActionQueue JMH Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks for comparing the performance of legacy ActionQueue vs graph-based ActionQueue implementations.

## Running Benchmarks

### Run all benchmarks
```bash
./gradlew :hibernate-core:jmh
```

### Run specific benchmark pattern
```bash
# Run only simple insert benchmarks
./gradlew :hibernate-core:jmh --args="simpleInserts"

# Run only parent-child benchmarks
./gradlew :hibernate-core:jmh --args="parentChild"

# Run only self-referencing tree benchmarks
./gradlew :hibernate-core:jmh --args="selfRefTree"

# Run only mixed operations benchmarks
./gradlew :hibernate-core:jmh --args="mixedOperations"
```

### Run specific benchmark with custom parameters
```bash
# Run with more iterations
./gradlew :hibernate-core:jmh --args="-wi 10 -i 20 simpleInserts"

# Run with more forks for statistical stability
./gradlew :hibernate-core:jmh --args="-f 3 simpleInserts"

# Output results in JSON format
./gradlew :hibernate-core:jmh --args="-rf json -rff results.json"
```

## Benchmark Scenarios

### 1. Simple Inserts (`simpleInserts_*`)
- **What**: Bulk insert of entities with no foreign key relationships
- **Why**: Baseline performance comparison without FK dependency overhead
- **Variants**:
  - `simpleInserts_100_*`: 100 entities per transaction
  - `simpleInserts_1000_*`: 1,000 entities per transaction

### 2. Parent-Child Inserts (`parentChild_*`)
- **What**: Parent entities with cascaded children (one-to-many with FK)
- **Why**: Measures FK dependency handling performance
- **Variants**:
  - `parentChild_10_*`: 10 parents × 10 children = 110 entities
  - `parentChild_100_*`: 100 parents × 10 children = 1,100 entities

### 3. Self-Referencing Trees (`selfRefTree_*`)
- **What**: Hierarchical tree structures with self-referential FKs
- **Why**: Tests self-referencing FK optimization (grouping strategy)
- **Variants**:
  - `selfRefTree_10_*`: 10 trees × 13 nodes = 130 entities
  - `selfRefTree_50_*`: 50 trees × 13 nodes = 650 entities
- **Structure**: Each tree has 1 root + 3 children + 9 grandchildren

### 4. Mixed Operations (`mixedOperations_*`)
- **What**: INSERT, UPDATE, DELETE operations in sequence
- **Why**: Tests realistic mixed workload performance
- **Operations**:
  - Insert 50 parents with 5 children each (300 entities)
  - Update all 50 parents
  - Delete all 50 parents and their children

## Understanding Results

JMH reports average time per operation in milliseconds. Lower is better.

Example output:
```
Benchmark                                      Mode  Cnt   Score   Error  Units
ActionQueueBenchmark.simpleInserts_100_Legacy  avgt   10  45.231 ± 2.156  ms/op
ActionQueueBenchmark.simpleInserts_100_Graph   avgt   10  41.087 ± 1.943  ms/op
```

This means:
- Legacy: ~45ms per operation (100 inserts)
- Graph: ~41ms per operation (100 inserts)
- **Graph is ~9% faster** for this scenario

## Benchmark Configuration

Default settings (configured in `hibernate-core.gradle`):
- **Warmup iterations**: 5 (1 second each)
- **Measurement iterations**: 10 (1 second each)
- **Fork**: 1 JVM fork
- **Mode**: Average time per operation
- **Time unit**: Milliseconds

These can be overridden via command-line args.

## JMH Arguments Reference

Common JMH arguments:
- `-wi <count>`: Warmup iterations (default: 5)
- `-w <time>`: Warmup time per iteration (default: 1s)
- `-i <count>`: Measurement iterations (default: 10)
- `-r <time>`: Measurement time per iteration (default: 1s)
- `-f <count>`: Number of forks (default: 1)
- `-t <count>`: Number of threads
- `-rf <format>`: Result format (text, csv, json, latex)
- `-rff <file>`: Result file name
- `-prof <profiler>`: Enable profiler (gc, stack, etc.)

Example with profiling:
```bash
./gradlew :hibernate-core:jmh --args="-prof gc simpleInserts"
```

## Comparing Results

To compare legacy vs graph implementations:
1. Look for pairs of benchmarks ending in `_Legacy` and `_Graph`
2. Compare the `Score` column (lower is better)
3. Calculate improvement: `(Legacy - Graph) / Legacy × 100%`

## CI/Automated Testing

For CI environments, use fewer iterations for faster results:
```bash
./gradlew :hibernate-core:jmh --args="-wi 2 -i 5 -rf json -rff benchmark-results.json"
```

Then parse the JSON results for regression detection.
