# ActionQueue Throughput Benchmarks

## Overview

Throughput benchmarks measure **operations per second (ops/s)** to compare the maximum sustainable load between legacy and graph-based ActionQueue implementations.

**Higher scores are better** - more operations per second means better throughput.

## Quick Start

```bash
# Run all throughput benchmarks
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark"

# Run specific throughput benchmark
./gradlew :hibernate-core:jmh --args="singleEntityInsert"

# Run with JSON output for analysis
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark -rf json -rff throughput-results.json"
```

## Benchmark Suite

### 1. Single Entity Insert (`singleEntityInsert_*`)
**What it measures**: Pure transaction throughput with minimal ActionQueue overhead

```java
// Measures how many of these can execute per second
session.beginTransaction();
session.persist(new ThroughputEntity("Entity", 42));
session.getTransaction().commit();
```

**Why it matters**: Baseline for single-entity transaction throughput

---

### 2. Batch Insert 50 (`batchInsert_50_*`)
**What it measures**: Bulk insertion throughput (50 entities per transaction)

```java
session.beginTransaction();
for (int i = 0; i < 50; i++) {
    session.persist(new ThroughputEntity("Entity-" + i, i));
}
session.getTransaction().commit();
```

**Why it matters**: Shows batching efficiency and ActionQueue scalability

---

### 3. Parent-Child Insert (`parentChildInsert_*`)
**What it measures**: Cascade insertion with FK dependencies (1 parent + 10 children)

```java
ThroughputParent parent = new ThroughputParent("Parent");
for (int i = 0; i < 10; i++) {
    parent.addChild(new ThroughputChild("Child-" + i, i));
}
session.persist(parent); // Cascade ALL
```

**Why it matters**: Tests FK dependency resolution performance

---

### 4. Entity Update (`entityUpdate_*`)
**What it measures**: Full CRUD cycle throughput (insert → update → delete)

```java
// Insert
session.persist(entity);
// Update
entity.name = "Updated";
entity.value = 999;
// Delete
session.remove(entity);
```

**Why it matters**: Real-world update operation throughput

---

### 5. Collection Add (`collectionAdd_*`)
**What it measures**: Collection modification throughput

```java
// Add 4 new children to existing parent with 1 child
ThroughputParent parent = session.find(ThroughputParent.class, id);
for (int i = 2; i <= 5; i++) {
    parent.addChild(new ThroughputChild("Child-" + i, i));
}
```

**Why it matters**: Collection change detection and UPDATE/INSERT generation

---

### 6. OrderColumn Insert (`orderColumnInsert_*`)
**What it measures**: @OrderColumn list operations (1 order + 20 line items)

```java
OrderHeader order = new OrderHeader("ORD-001", "Customer");
for (int i = 0; i < 20; i++) {
    order.addItem(new OrderedItem("Item-" + i));
}
session.persist(order);
```

**Why it matters**: Tests @OrderColumn UPDATE statement generation efficiency
- Graph-based has optimized handling (no redundant UPDATEs)
- Legacy may generate unnecessary UPDATE statements

---

### 7. Mixed Workload (`mixedWorkload_*`)
**What it measures**: Realistic mixed operation throughput

```java
// Insert 10 entities
// Update 5 of them
// Delete all 10
```

**Why it matters**: Real-world transaction mix performance

---

### 8. Complex Cascade (`complexCascade_*`)
**What it measures**: Large cascade graph throughput (5 parents × 8 children = 40 entities)

```java
for (int i = 0; i < 5; i++) {
    ThroughputParent parent = new ThroughputParent("Parent-" + i);
    for (int j = 0; j < 8; j++) {
        parent.addChild(new ThroughputChild("Child-" + i + "-" + j, j));
    }
    session.persist(parent);
}
```

**Why it matters**: Stress test for complex object graphs

---

### 9. Exceed Batch Size 100 (`exceedBatchSize_100_*`)
**What it measures**: Insert 100 entities (batch size = 50, requires 2 batches)

```java
for (int i = 0; i < 100; i++) {
    session.persist(new ThroughputEntity("Entity-" + i, i));
}
```

**Why it matters**: Tests multi-batch execution performance
- Batch size is 50, so 100 entities require 2 JDBC batch flushes
- Shows how ActionQueue handles batch boundaries
- Important for bulk import scenarios

---

### 10. Exceed Batch Size 500 (`exceedBatchSize_500_*`)
**What it measures**: Insert 500 entities (batch size = 50, requires 10 batches)

```java
for (int i = 0; i < 500; i++) {
    session.persist(new ThroughputEntity("Entity-" + i, i));
}
```

**Why it matters**: Tests ActionQueue scalability with many batches
- 10 JDBC batches to complete
- Reveals overhead in batch coordination
- Critical for large data import operations

---

### 11. Mixed Exceed Batch (`mixedExceedBatch_*`)
**What it measures**: Mixed operations all exceeding batch size

```java
// Insert 100 entities
// Update 60 entities
// Delete 100 entities
```

**Why it matters**: Real-world scenario with multiple operation types
- Each operation type requires multiple batches
- Tests ActionQueue's ability to handle mixed operations across batches
- Simulates complex transactions

---

### 12. Cascade Exceed Batch (`cascadeExceedBatch_*`)
**What it measures**: 15 parents with 5 children each = 90 entities

```java
for (int i = 0; i < 15; i++) {
    ThroughputParent parent = new ThroughputParent("Parent-" + i);
    for (int j = 0; j < 5; j++) {
        parent.addChild(new ThroughputChild("Child-" + i + "-" + j, j));
    }
    session.persist(parent);
}
```

**Why it matters**: Cascade operations spanning multiple batches
- 90 total entities exceed batch size of 50
- Tests FK dependency handling across batch boundaries
- Validates correct parent-child ordering in multiple batches

---

## Expected Results

### Performance Characteristics

**Graph-based ActionQueue** should show:
- **Higher throughput** for FK-heavy workloads (parent-child, cascades)
- **Similar or better** for simple inserts
- **Significantly better** for @OrderColumn operations
- **Consistent** performance across workload sizes

**Legacy ActionQueue** may show:
- **Lower throughput** for complex FK dependencies
- **Degraded performance** with @OrderColumn (redundant UPDATEs)
- **Variable** performance based on action ordering

### Sample Output

```
Benchmark                                                  Mode  Cnt     Score     Error  Units
ActionQueueThroughputBenchmark.singleEntityInsert_Legacy  thrpt    5   245.123 ±  12.345  ops/s
ActionQueueThroughputBenchmark.singleEntityInsert_Graph   thrpt    5   278.456 ±  10.234  ops/s

ActionQueueThroughputBenchmark.parentChildInsert_Legacy   thrpt    5    32.567 ±   2.134  ops/s
ActionQueueThroughputBenchmark.parentChildInsert_Graph    thrpt    5    41.234 ±   1.876  ops/s

ActionQueueThroughputBenchmark.orderColumnInsert_Legacy   thrpt    5    18.456 ±   1.234  ops/s
ActionQueueThroughputBenchmark.orderColumnInsert_Graph    thrpt    5    29.789 ±   1.567  ops/s
```

**Interpretation**:
- Single entity: Graph is 13.6% faster
- Parent-child: Graph is 26.6% faster
- OrderColumn: Graph is 61.4% faster (due to @OrderColumn optimization)

## Configuration

Default settings:
- **Warmup**: 3 iterations × 2 seconds = 6 seconds
- **Measurement**: 5 iterations × 3 seconds = 15 seconds
- **Fork**: 1 JVM
- **Mode**: Throughput (ops/s)

### Custom Configuration

```bash
# Longer measurement for stability
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark -wi 5 -i 10"

# Multiple forks for statistical confidence
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark -f 3"

# With GC profiling
./gradlew :hibernate-core:jmh --args="ActionQueueThroughputBenchmark -prof gc"
```

## Comparing with Average Time Benchmarks

| Metric | Throughput Benchmark | Average Time Benchmark |
|--------|---------------------|------------------------|
| **Unit** | ops/s | ms/op |
| **Better** | Higher | Lower |
| **Focus** | Maximum load capacity | Latency per operation |
| **Use Case** | Sustained workload performance | Individual operation timing |

Both provide complementary performance insights.

## Troubleshooting

### Low Throughput Numbers
- Ensure database is warmed up (warmup iterations)
- Check for disk I/O bottlenecks (use in-memory H2)
- Verify JDBC batch size is configured (default: 50)

### High Variance (Error bars)
- Increase measurement iterations: `-i 10` or `-i 20`
- Increase measurement time: `-r 5` (5 seconds per iteration)
- Use multiple forks for JVM stability: `-f 3`

### Running Out of Memory
- Benchmarks include cleanup operations
- If OOM occurs, check H2 database settings
- May need to increase JVM heap: `org.gradle.jvmargs=-Xmx4g`
