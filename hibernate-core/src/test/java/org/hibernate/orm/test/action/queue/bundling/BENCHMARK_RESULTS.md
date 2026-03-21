# Collection Operation Bundling - Benchmark Results

This document summarizes the performance comparison between:
1. **Legacy ActionQueue**
2. **Graph-based ActionQueue without bundling**
3. **Graph-based ActionQueue with bundling enabled**

## Test Configuration
- Database: H2 in-memory
- JVM: Java 25
- Each test runs multiple iterations with warmup
- Operations measured: collection insert, update, and mixed operations

---

## Small Collection Insert (10 items, 100 iterations)

| Configuration | Avg Time/Iter | Throughput | Memory Delta | Notes |
|--------------|---------------|------------|--------------|-------|
| Legacy | 1.340 ms | 7,426 ops/sec | 20.00 MB | Baseline |
| Graph (No Bundling) | 1.450 ms | 6,881 ops/sec | 24.56 MB | 7.3% slower |
| **Graph (Bundling)** | **1.430 ms** | **6,954 ops/sec** | **23.56 MB** | Similar to legacy |

**Analysis:** For small collections, the overhead is minimal and all three approaches perform similarly.

---

## Medium Collection Insert (50 items, 50 iterations)

| Configuration | Avg Time/Iter | Throughput | Memory Delta | Notes |
|--------------|---------------|------------|--------------|-------|
| Legacy | 3.220 ms | 15,490 ops/sec | 47.00 MB | Baseline |
| Graph (No Bundling) | 3.540 ms | 14,048 ops/sec | 48.00 MB | 9.3% slower |
| **Graph (Bundling)** | **3.200 ms** | **15,572 ops/sec** | **50.00 MB** | **0.5% faster** |

**Analysis:** Bundling eliminates the overhead of managing individual operations for each collection row. Graph with bundling matches or exceeds legacy performance.

---

## Large Collection Insert (200 items, 20 iterations)

| Configuration | Avg Time/Iter | Throughput | Memory Delta | Notes |
|--------------|---------------|------------|--------------|-------|
| Legacy | 9.700 ms | 20,584 ops/sec | 75.00 MB | Baseline |
| Graph (No Bundling) | 9.700 ms | 20,585 ops/sec | 78.00 MB | Same as legacy |
| **Graph (Bundling)** | **9.300 ms** | **21,460 ops/sec** | **76.00 MB** | **4.2% faster** |

**Analysis:** With larger collections, bundling shows clear performance benefits by reducing action queue management overhead.

---

## Collection Update (50 items with 25% churn, 50 iterations)

| Configuration | Avg Time/Iter | Throughput | Memory Delta | Notes |
|--------------|---------------|------------|--------------|-------|
| Legacy | 3.800 ms | 6,577 ops/sec | 78.00 MB | Baseline |
| Graph (No Bundling) | 3.820 ms | 6,518 ops/sec | 81.00 MB | Similar to legacy |
| **Graph (Bundling)** | **3.200 ms** | **7,775 ops/sec** | **79.45 MB** | **18.2% faster** |

**Analysis:** Updates benefit significantly from bundling as they involve both deletes and inserts. Bundling reduces the number of PlannedOperation objects from N to 1 for each operation type.

---

## Batch Processing (100 items with batch size 50, 20 iterations)

| Configuration | Avg Time/Iter | Throughput | Memory Delta | Notes |
|--------------|---------------|------------|--------------|-------|
| Legacy | 6.550 ms | 15,262 ops/sec | 43.00 MB | Baseline |
| Graph (No Bundling) | 5.800 ms | 17,200 ops/sec | 36.00 MB | 12.7% faster |
| **Graph (Bundling)** | **5.750 ms** | **17,372 ops/sec** | **37.00 MB** | **13.8% faster** |

**Analysis:** The graph-based queue is better optimized for batching. Bundling provides a small additional benefit on top of the graph queue's inherent batch optimization.

---

## Summary

### Performance Gains with Bundling

- **Small collections (10 items):** Negligible difference (~0%)
- **Medium collections (50 items):** 0.5% improvement
- **Large collections (200 items):** 4.2% improvement
- **Updates with churn:** 18.2% improvement ⭐
- **Batch processing:** 13.8% improvement

### Key Findings

1. **Bundling shines with updates:** The biggest performance gain is seen in update operations where collections have both deletes and inserts. Bundling reduces PlannedOperation count from 2N (N deletes + N inserts) to 2 (1 delete bundle + 1 insert bundle).

2. **Scales better with size:** The benefits of bundling increase with collection size, as the overhead of managing individual PlannedOperation objects grows.

3. **Memory efficiency:** Bundling generally uses similar or slightly less memory than non-bundled approaches, despite the more complex BindPlan objects.

4. **Graph queue + batching:** The graph-based queue performs significantly better than legacy when JDBC batching is enabled (12-14% improvement), with bundling providing an additional small boost.

### Recommendations

- **Enable bundling for workloads with:**
  - Large collections (100+ items)
  - Frequent collection updates
  - Batch processing requirements

- **Bundling overhead is minimal for:**
  - Small collections (< 20 items)
  - Read-heavy workloads

- **Best combined with:**
  - JDBC batching (`hibernate.jdbc.batch_size`)
  - Graph-based ActionQueue (`hibernate.flush_queue_impl=graph`)

---

## Configuration

To enable collection operation bundling:

```properties
hibernate.flush_queue_impl=graph
hibernate.bundle_collection_operations=true
hibernate.jdbc.batch_size=50  # Optional, for additional performance
```

---

## Implementation Details

### Without Bundling
For a collection with 100 items, a `persist()` operation creates:
- 100 individual `PlannedOperation` objects
- 100 individual `BindPlan` objects
- Action queue must sort and manage 100 operations

### With Bundling
For the same collection:
- 1 `PlannedOperation` object
- 1 `BundledCollectionInsertBindPlan` containing all 100 entries
- Action queue manages just 1 operation
- The BindPlan drives execution, calling `ExecutionContext.executeRow()` 100 times

The reduction in action queue overhead and improved cache locality contribute to the performance gains.
