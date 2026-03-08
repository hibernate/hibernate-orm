# ActionQueue JMH Benchmarks

This directory contains JMH (Java Microbenchmark Harness) benchmarks for comparing the performance of legacy ActionQueue vs graph-based ActionQueue implementations.

## Benchmark Types

### 1. **ActionQueueBenchmark** (Average Time Mode)
Measures average time per operation in milliseconds. Lower is better.
- Focus: Latency and timing characteristics
- Mode: `AverageTime`
- Unit: `ms/op`

### 2. **ActionQueueThroughputBenchmark** (Throughput Mode)
Measures operations per second. Higher is better.
- Focus: Maximum throughput and operations/second
- Mode: `Throughput`
- Unit: `ops/s`

## Running Benchmarks

### Run all benchmarks
```bash
./gradlew :hibernate-core:jmh
```

### Run specific benchmark class
```bash
# Run only average time benchmarks
./gradlew :hibernate-core:jmh --args="ActionQueueBenchmark"

# Run only throughput benchmarks
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark"
```

### Run specific benchmark pattern
```bash
# Run only simple insert benchmarks (both average time and throughput)
./gradlew :hibernate-core:jmh --args="simpleInserts"

# Run only throughput benchmarks for single entity inserts
./gradlew :hibernate-core:jmh --args="singleEntityInsert"

# Run only parent-child benchmarks
./gradlew :hibernate-core:jmh --args="parentChild"

# Run only self-referencing tree benchmarks (average time only)
./gradlew :hibernate-core:jmh --args="selfRefTree"

# Run only mixed operations benchmarks
./gradlew :hibernate-core:jmh --args="mixedOperations"

# Run only OrderColumn benchmarks (throughput only)
./gradlew :hibernate-core:jmh --args="orderColumn"
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

### ActionQueueBenchmark (Average Time)

#### 1. Simple Inserts (`simpleInserts_*`)
- **What**: Bulk insert of entities with no foreign key relationships
- **Why**: Baseline performance comparison without FK dependency overhead
- **Variants**:
  - `simpleInserts_100_*`: 100 entities per transaction
  - `simpleInserts_1000_*`: 1,000 entities per transaction

#### 2. Parent-Child Inserts (`parentChild_*`)
- **What**: Parent entities with cascaded children (one-to-many with FK)
- **Why**: Measures FK dependency handling performance
- **Variants**:
  - `parentChild_10_*`: 10 parents × 10 children = 110 entities
  - `parentChild_100_*`: 100 parents × 10 children = 1,100 entities

#### 3. Self-Referencing Trees (`selfRefTree_*`)
- **What**: Hierarchical tree structures with self-referential FKs
- **Why**: Tests self-referencing FK optimization (grouping strategy)
- **Variants**:
  - `selfRefTree_10_*`: 10 trees × 13 nodes = 130 entities
  - `selfRefTree_50_*`: 50 trees × 13 nodes = 650 entities
- **Structure**: Each tree has 1 root + 3 children + 9 grandchildren

#### 4. Mixed Operations (`mixedOperations_*`)
- **What**: INSERT, UPDATE, DELETE operations in sequence
- **Why**: Tests realistic mixed workload performance
- **Operations**:
  - Insert 50 parents with 5 children each (300 entities)
  - Update all 50 parents
  - Delete all 50 parents and their children

### ActionQueueThroughputBenchmark (Throughput)

#### 1. Single Entity Insert (`singleEntityInsert_*`)
- **What**: Insert one entity per transaction (minimal overhead)
- **Why**: Measures pure transaction throughput with minimal ActionQueue load
- **Metric**: Transactions per second

#### 2. Batch Insert (`batchInsert_50_*`)
- **What**: Insert 50 entities in a single transaction
- **Why**: Measures batch insertion throughput
- **Metric**: Batch operations per second

#### 3. Parent-Child Insert (`parentChildInsert_*`)
- **What**: Insert 1 parent with 10 children (cascade)
- **Why**: Measures throughput with FK dependencies
- **Metric**: Parent-child graph inserts per second

#### 4. Entity Update (`entityUpdate_*`)
- **What**: Complete lifecycle: insert → update → delete
- **Why**: Measures update operation throughput
- **Metric**: Update cycles per second

#### 5. Collection Add (`collectionAdd_*`)
- **What**: Add children to existing parent collection
- **Why**: Measures collection modification throughput
- **Metric**: Collection modification operations per second

#### 6. OrderColumn Insert (`orderColumnInsert_*`)
- **What**: Insert order with 20 line items using @OrderColumn
- **Why**: Measures @OrderColumn update SQL generation performance
- **Metric**: OrderColumn operations per second

#### 7. Mixed Workload (`mixedWorkload_*`)
- **What**: Insert 10 entities, update 5, delete all
- **Why**: Realistic mixed operation throughput
- **Metric**: Mixed workload cycles per second

#### 8. Complex Cascade (`complexCascade_*`)
- **What**: Insert 5 parents with 8 children each (40 entities total)
- **Why**: Measures complex cascade throughput
- **Metric**: Complex cascade operations per second

#### 9. Exceed Batch Size 100 (`exceedBatchSize_100_*`)
- **What**: Insert 100 entities (batch size is 50, requires 2 batches)
- **Why**: Measures performance when operations span multiple JDBC batches
- **Metric**: Operations per second with multi-batch handling

#### 10. Exceed Batch Size 500 (`exceedBatchSize_500_*`)
- **What**: Insert 500 entities (batch size is 50, requires 10 batches)
- **Why**: Measures scalability with large operations requiring many batches
- **Metric**: Large batch operations per second

#### 11. Mixed Exceed Batch (`mixedExceedBatch_*`)
- **What**: Insert 100, update 60, delete 100 (all exceed batch size)
- **Why**: Tests mixed operation types with multi-batch execution
- **Metric**: Mixed multi-batch cycles per second

#### 12. Cascade Exceed Batch (`cascadeExceedBatch_*`)
- **What**: Insert 15 parents with 5 children each (90 entities, exceeds batch size)
- **Why**: Tests cascade operations requiring multiple batches
- **Metric**: Large cascade operations per second

## Understanding Results

### Average Time Mode (ActionQueueBenchmark)
JMH reports average time per operation in milliseconds. **Lower is better.**

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
- Calculation: `(45.231 - 41.087) / 45.231 × 100% = 9.2% improvement`

### Throughput Mode (ActionQueueThroughputBenchmark)
JMH reports operations per second. **Higher is better.**

Example output:
```
Benchmark                                             Mode  Cnt     Score     Error  Units
ActionQueueThroughputBenchmark.singleEntityInsert_Legacy  thrpt    5   245.123 ±  12.345  ops/s
ActionQueueThroughputBenchmark.singleEntityInsert_Graph   thrpt    5   278.456 ±  10.234  ops/s
```

This means:
- Legacy: ~245 operations per second
- Graph: ~278 operations per second
- **Graph is ~14% higher throughput** for this scenario
- Calculation: `(278.456 - 245.123) / 245.123 × 100% = 13.6% improvement`

### Which Mode to Use?
- **Average Time**: Best for understanding latency and timing characteristics
- **Throughput**: Best for understanding maximum sustainable load
- Both are valuable for different performance concerns

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
2. Compare the `Score` column:
   - **Average Time**: Lower is better
   - **Throughput**: Higher is better
3. Calculate improvement:
   - **Average Time**: `(Legacy - Graph) / Legacy × 100%` (positive = faster)
   - **Throughput**: `(Graph - Legacy) / Legacy × 100%` (positive = more throughput)

### Quick Comparison Commands
```bash
# Run both implementations side-by-side for single entity inserts
./gradlew :hibernate-core:jmh --args="singleEntityInsert"

# Compare throughput across all scenarios
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark -rf json -rff throughput-results.json"

# Compare average time across all scenarios
./gradlew :hibernate-core:jmh --args="ActionQueueBenchmark -rf json -rff avgtime-results.json"
```

## CI/Automated Testing

For CI environments, use fewer iterations for faster results:
```bash
./gradlew :hibernate-core:jmh --args="-wi 2 -i 5 -rf json -rff benchmark-results.json"
```

Then parse the JSON results for regression detection.
