# Graph Queue Test Failures - Categorized Analysis

**Date**: 2026-04-15
**Total Failures**: 178 (graph queue) vs 3 (legacy queue)
**Delta**: 175 additional failures with graph queue

## Summary by Exception Type

| Exception Type | Count | % of Total |
|----------------|-------|------------|
| AssertionFailedError / AssertionError | 99 | 55.6% |
| ConstraintViolationException | 28 | 15.7% |
| HibernateException (generic) | 25 | 14.0% |
| OptimisticLockException / StaleObjectStateException | 24 | 13.5% |
| DataException | 20 | 11.2% |
| NullPointerException | 15 | 8.4% |
| RollbackException | 12 | 6.7% |
| IllegalArgumentException | 4 | 2.2% |
| ClassCastException | 1 | 0.6% |

*Note: Tests may have multiple exception types in their stack traces*

## Summary by Test Category

| Category | Count | Notes |
|----------|-------|-------|
| Collection Operations | 35 | List, Map, Set operations |
| Cascade Operations | 17 | Cascade persist, delete, merge |
| Locking / Versioning | 16 | Optimistic/pessimistic locking |
| Merge Operations | 11 | Entity merging, detached state |
| Insert Ordering | 9 | Batch insert ordering |
| Bytecode Enhancement | 6 | Enhanced entity tests |
| Decomposer Tests | 5 | Graph decomposer functionality |
| Other | 79 | Misc test categories |

---

## Detailed Breakdown

### 1. Optimistic Locking / Versioning Issues (24 failures)

**Symptoms:**
- `OptimisticLockException` / `StaleObjectStateException` / `StaleStateException`
- Version increments at wrong times
- Lock acquisition failures

**Representative Tests:**
- `Enhanced:FetchGraphTest > testQueryAndDeleteDEntity` (3 variants)
- `QueryLockingTest` (6 variants - pessimistic/optimistic forced increment)
- `OneToOneTest > testBidirectionalFkOneToOne`
- `BidirectionalOneToOneCascadeRemoveTest > testWithoutFlush`
- `CachedMutableNaturalIdStrictReadWriteTest > testCreateDeleteRecreate`
- `RowIdUpdateAndDeleteTest` (2 tests)

**Root Cause Hypothesis:**
- Version update timing differs between legacy and graph queue
- Graph queue may be applying version increments at different points in the flush lifecycle
- Possibly related to when `EntityEntry.postUpdate()` is called vs when version is actually incremented

**Priority:** HIGH - Data corruption risk if versions aren't managed correctly

---

### 2. Constraint Violations (28 failures)

**Symptoms:**
- `ConstraintViolationException` / `JdbcBatchUpdateException`
- FK constraint violations
- Unique constraint violations
- NOT NULL violations

**Representative Tests:**
- `Enhanced:CascadeOnUninitializedTest` (2 variants)
- `MultipleMappingsTest > test`
- `ConstraintViolationExceptionHandlingTest` (6 parameterized variants)
- `MixedTypeEmbeddableGeneratorsTest` (2 variants)
- `JoinedSubclass*DiscriminatorTest > basicUsageTest` (3 variants)
- `JoinTest > testCustomColumnReadAndWrite`
- `MergeMultipleEntityCopiesAllowedTest > testCascadeFromDetachedToGT2DirtyRepresentations`
- `TemporalEntityServerSideTest > test`

**Root Cause Hypothesis:**
- Graph ordering still has gaps for complex scenarios
- Possibly related to joined inheritance with discriminators
- May be issues with temporal/audit entity handling
- Cascade operations may not be ordered correctly

**Priority:** CRITICAL - Data integrity violations

---

### 3. Null Pointer Exceptions (15 failures)

**Symptoms:**
- Unexpected null values during operations
- Missing state or metadata

**Representative Tests:**
- `Enhanced:SecondaryTableDynamicUpateTest > testSetSecondaryTableColumnToNull`
- `EntityManagerFactorySerializationTest > testSerialization`
- `EntityManagerSerializationTest > testSerialization`
- `OneToManyAbstractTablePerClassTest > testAddAndRemove`
- `RowIdUpdateAndDeleteTest` (2 variants)
- `OneToManyCustomSqlMutationsTest` (2 variants - SQL delete/update)
- `AuditEntityTest > test`
- `TemporalEntityHistoryTest` (2 variants)
- `TemporalEntityHistoryServerSideTest` (2 variants)

**Root Cause Hypothesis:**
- Secondary table dynamic update handling issue
- Serialization doesn't preserve all necessary state
- RowId operations may have missing metadata
- Temporal/history entity tracking issues
- Custom SQL mutations may not be properly handled

**Priority:** HIGH - Can cause runtime failures

---

### 4. Generic Hibernate Exceptions (25 failures)

**Symptoms:**
- General `HibernateException` without specific subtype
- Often related to lifecycle or state management

**Representative Tests:**
- `BasicHibernateAnnotationsTest` (3 variants - versioning, type, entity)
- `OrderByTest > testInverseIndexCascaded`
- `NotNullManyToOneTest > testSaveChildWithoutParent`
- `CascadeDeleteTest > testDelete`
- `ListDelayedOperationTest > testSimpleAddTransient`
- `PersistentMapTest > testMapKeyColumnNonInsertableNonUpdatableUnidirOneToMany`
- `NestedEmbeddableWithLockingDeletionTest` (4 variants)
- `MapIndexFormulaTest` (2 variants)
- `InheritanceVersionedParentTest > testUpdateAssociation`
- `ManyToManyTest > testManyToManyWithFormula`
- `CompositeIdBatchDeletionTest` (2 variants)

**Root Cause Hypothesis:**
- Map/List operations with special configurations (index formulas, non-insertable keys)
- Nested embeddables with locking
- Composite ID batch operations
- Delayed collection operations

**Priority:** MEDIUM - May be configuration-specific edge cases

---

### 5. Data Exceptions (20 failures)

**Symptoms:**
- `org.hibernate.exception.DataException` / `JdbcSQLDataException`
- Data integrity issues at SQL level

**Representative Tests:**
- `DeleteUnloadedProxyTest` (2 variants - attached/detached)
- `QueryLockingTest` (6 variants with lock modes)

**Root Cause Hypothesis:**
- Unloaded proxy deletion may generate invalid SQL
- Lock mode handling differs from legacy queue
- Related to optimistic locking issues

**Priority:** HIGH - SQL-level errors

---

### 6. Test Assertion Failures (99 failures - 55.6%)

**Note:** These are test expectations not being met, not framework exceptions. Many may be legitimate behavior differences or test assumptions.

**Subcategories:**

**a) Soft Delete / Decomposer Tests (3)**
- `DeleteDecomposerTest > testStaticSoftDeleteGroup`
- `DeleteDecomposerTest > testSoftDeleteDecomposition`
- `DeleteDecomposerTest > testSoftDeleteWithVersion`

**b) Batching / Insert Ordering (10+)**
- `InsertOrdering*` tests (7 variants)
- `BatchingBatchFailureTest > testBasicInsertion`
- `BatchNoUseJdbcMetadataTest > testBatching`
- `BatchedMultiTableDynamicStatementTests > testBatched`
- `InMemoryTimestampGenerationBatchTest > test`

**c) Collection Operations (10+)**
- `ListAddTest` (2 variants)
- `MergeNotNullCollectionUsingIdentityTest > testOneToManyNullCollection`
- `UnversionedNoCascadeDereferencedCollectionTest` (2 variants)
- `UnversionedCascadeDereferencedCollectionTest` (2 variants)

**d) Event Listener / Cascade (5+)**
- `PreInsertEventListenerVeto*Test > testVeto` (2 variants)
- `PreDeleteEventListenerTest > testAccessUninitializedCollectionInListener`
- `MultiCircleJpaCascadeTest` (2 variants)

**e) Merge Operations (5+)**
- `MergeMultipleEntityCopiesAllowedTest` (2 variants)
- `MergeCascadeWithMapCollectionTest > testMergeParentWithChildren`

**f) Exception Handling Tests (6)**
- `ConstraintViolationExceptionHandlingTest` (6 parameterized variants)
- `TransientObjectExceptionHandlingTest` (4 variants)

**g) Mutation Delegate / Statement Release (2)**
- `MutationDelegateStatementReleaseTest` (2 variants)

**h) Other (50+)**
- Various edge cases and specific scenario tests

**Root Cause Hypothesis:**
- Many of these may be test assumptions that don't hold for graph queue
- Some may be legitimate behavioral differences (e.g., batching order)
- Event listener timing may differ
- Exception handling behavior may differ

**Priority:** VARIES - Need individual assessment

---

### 7. IllegalArgumentException (4 failures)

**Representative Tests:**
- `InsertOrderingWithSecondaryTable > testInheritanceWithSecondaryTable`
- `InsertOrderingSelfReferenceTest > testReferenceItself`
- `SessionJdbcBatchTest > testSessionFactorySetting`

**Root Cause Hypothesis:**
- Invalid arguments being passed to graph construction
- Possibly circular reference or self-reference handling

**Priority:** MEDIUM

---

### 8. ClassCastException (1 failure)

**Representative Tests:**
- `BatchAndUserTypeIdCollectionTest > initializationError`

**Root Cause Hypothesis:**
- Type mismatch in collection handling with user types

**Priority:** LOW - Single occurrence

---

## Priority Categorization

### P1 - Critical (Data Integrity)
1. **Constraint Violations** (28 tests) - Can cause data corruption
2. **Optimistic Locking Issues** (24 tests) - Can cause lost updates

**Total P1**: 52 failures (29%)

### P2 - High (Correctness)
3. **Null Pointer Exceptions** (15 tests) - Runtime failures
4. **Data Exceptions** (20 tests) - SQL-level errors
5. **Generic Hibernate Exceptions** (25 tests) - State management issues

**Total P2**: 60 failures (34%)

### P3 - Medium (Functionality)
6. **Batching/Insert Ordering** (10+ tests) - Performance and ordering
7. **Collection Operations** (10+ tests) - Collection behavior differences
8. **IllegalArgumentException** (4 tests) - Invalid arguments

**Total P3**: 24+ failures (14%)

### P4 - Lower (Edge Cases / Test Assumptions)
9. **Test Assertion Failures** (remaining ~42) - May be legitimate differences
10. **Decomposer Tests** (5 tests) - Graph queue specific
11. **Other** (~20)

**Total P4**: ~42 failures (23%)

---

## Recommended Investigation Order

1. **Start with Constraint Violations (P1)**
   - Focus on joined inheritance discriminator tests
   - Look at cascade ordering
   - Check temporal entity handling

2. **Then Optimistic Locking (P1)**
   - Version increment timing
   - Lock acquisition sequence
   - Compare with legacy queue behavior

3. **NullPointerException patterns (P2)**
   - Secondary table dynamic updates
   - Serialization state preservation
   - Temporal/audit entity tracking

4. **Data Exceptions (P2)**
   - Proxy deletion SQL generation
   - Lock mode handling

5. **Systematic Assertion Failures (P3/P4)**
   - Batching/ordering behavior
   - Event listener timing
   - Exception handling expectations

---

## Notes

- **55.6% of failures** are assertion failures where test expectations don't match behavior
  - Many may be acceptable behavioral differences
  - Some may indicate real bugs
  - Need case-by-case analysis

- **29% are P1 critical** (constraint violations + locking issues)
  - These represent data integrity risks
  - Should be highest priority

- **Bytecode enhancement** appears relatively stable (only 6 failures)
  - Much better than the ~20 documented earlier

- **Insert ordering** still has issues (9 failures)
  - But much improved from earlier state
  
- **Collection operations** remain a significant problem area (35 failures)
  - Map/List/Set operations with special configurations
  - Index formulas, non-insertable keys, etc.
