# Remaining Graph Queue Failures Analysis

**Total Failures**: 392 (vs 393 baseline, 44 with legacy queue)

## Failure Breakdown by Exception Type

1. **AssertionFailedError**: ~123 failures
   - Test expectations not met (collection sizes, entity states, etc.)
   
2. **AssertionError**: ~53 failures
   - Similar to above, assertion violations

3. **ClassCastException**: ~43 failures
   - Type casting issues during operations

4. **NullPointerException**: ~30 failures
   - Unexpected null values

5. **ConstraintViolationException**: ~22 failures
   - FK/unique constraint violations still happening

6. **RuntimeException**: ~18 failures
   - Various runtime issues

## Failure Breakdown by Test Category

### 1. Collection Management Issues (~100+ failures)

**Primary affected tests:**
- `EntityWithNonInverseOneToManyJoinTest` (11 failures)
- `VersionedEntityWithNonInverseOneToManyJoinTest` (11 failures)
- `EntityWithNonInverseManyToManyTest` (7 failures)
- `VersionedEntityWithNonInverseManyToManyTest` (7 failures)
- `EntityWithInverseManyToManyTest` (6 failures)

**Common symptoms:**
- Wrong collection sizes after update/merge operations
- Elements not properly added/removed from collections
- Orphan removal not working correctly (ClassCastException at IntegerJavaType.java:22)

**Example failures:**
- `testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement` - AssertionFailedError
- `testDeleteOneToManyOrphan` - ClassCastException
- `testMoveOneToManyElementToExistingEntityCollection` - AssertionFailedError

**Pattern**: Non-inverse collections (where parent owns the relationship) have ordering/lifecycle issues

### 2. Embedded/Component Issues (~40 failures)

**Primary affected tests:**
- `EmbeddedTest` (18 failures)
- `NestedJsonEmbeddableTest` (13 failures)
- `JsonEmbeddableTest` (11 failures)
- `JsonWithArrayEmbeddableTest` (10 failures)
- `ComponentTest` (9 failures)

**Common symptoms:**
- NullPointerException when querying with embedded parameters
- Issues with embeddables in secondary tables
- JSON embeddable handling broken

**Example failures:**
- `testQueryWithEmbeddedWithNullUsingSubAttributes` - NPE
- `testEmbeddedInSecondaryTable` - NPE
- `testCompositeId` - NPE

**Pattern**: Embeddable/component field access or query parameter handling issues

### 3. Orphan Removal with Join Tables (~15 failures)

**Primary affected tests:**
- Tests with "DeleteOneToManyOrphan" in name
- Tests with "RemoveOneToManyOrphan" in name

**Common symptoms:**
- ClassCastException at IntegerJavaType.java:22
- Occurs specifically with non-inverse OneToMany join tables

**Example failures:**
- `VersionedEntityWithNonInverseOneToManyJoinTest.testDeleteOneToManyOrphan`
- `EntityWithNonInverseOneToManyUnidirTest.testRemoveOneToManyOrphanUsingMerge`

**Pattern**: Something wrong with ID/key handling during orphan removal on join tables

### 4. Bytecode Enhancement Issues (~20 failures)

**Tests with "Enhanced:" prefix:**
- Enhanced entity tests failing with various exceptions
- Lazy loading and dirty checking interaction issues

**Example failures:**
- `Enhanced:CascadeOnUninitializedTest` - ConstraintViolationException
- `Enhanced:SecondaryTableDynamicUpateTest` - ClassCastException
- `Enhanced:FetchGraphTest` - Various issues

**Pattern**: Bytecode enhancement and graph queue don't play well together

### 5. Locking/Versioning Issues (~15 failures)

**Primary affected tests:**
- `QueryLockingTest` (6 failures)
- Tests with "Versioned" prefix
- `NestedEmbeddableWithLockingDeletionTest` (4 failures)

**Common symptoms:**
- Optimistic locking not working correctly
- Version increments happening at wrong times

### 6. Merge/Detached Entity Issues (~10 failures)

**Primary affected tests:**
- `MergeTest` (5 failures)
- Tests with "Merge" in name

**Common symptoms:**
- Entities not properly merged
- Collection state not correctly synchronized

### 7. Constraint Violations (~10 failures)

**Still occurring despite our fixes:**
- Join table constraint violations
- FK ordering issues in complex scenarios
- Collection table unique constraint violations

**Example failures:**
- `ImprovedNamingCollectionElementTest.testAttributedJoin`
- `DefaultNamingCollectionElementTest.testAttributedJoin`
- `ConstraintViolationExceptionHandlingTest` tests

## Priority Categorization

### P1 - Critical Issues (Data Correctness)

1. **ClassCastException during orphan removal** (~15 failures)
   - IntegerJavaType.java:22 indicates ID/key type confusion
   - Affects join table orphan removal
   - **Root cause**: Likely unique slot extraction or value comparison issue

2. **Constraint violations** (~10 failures)
   - FK/unique constraints still being violated
   - **Root cause**: Graph ordering still has gaps for complex scenarios

### P2 - High Priority (Common Use Cases)

3. **Non-inverse collection management** (~60 failures)
   - OneToMany/ManyToMany without mappedBy
   - Collection operations produce wrong results
   - **Root cause**: Likely action ordering or missing collection updates

4. **Embedded/Component NullPointerExceptions** (~30 failures)
   - Query parameter handling broken
   - Embeddable field access issues
   - **Root cause**: Possibly decomposition or bind plan issues

### P3 - Medium Priority (Advanced Features)

5. **Bytecode enhancement interactions** (~20 failures)
   - Enhanced entities + graph queue problems
   - Lazy initialization issues
   - **Root cause**: Enhanced entity state not properly tracked

6. **Optimistic locking/versioning** (~15 failures)
   - Version increments at wrong times
   - Lock acquisition issues
   - **Root cause**: Version update timing in graph execution

### P4 - Lower Priority (Edge Cases)

7. **JSON embeddables** (~24 failures)
   - Specialized embeddable type issues
   - Less commonly used feature

8. **Various assertion failures** (~60 failures)
   - Test expectations vs. actual behavior mismatches
   - May include legitimate behavior differences

## Next Steps

### Immediate Investigation Targets

1. **ClassCastException at IntegerJavaType.java:22**
   - Appears in ~15 orphan removal tests
   - Systematic issue with ID type handling
   - Start here: `VersionedEntityWithNonInverseOneToManyJoinTest.testDeleteOneToManyOrphan`

2. **Non-inverse collection operations**
   - Affects most collection tests
   - May be fundamental to how collections are decomposed
   - Start here: `EntityWithNonInverseOneToManyJoinTest.testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement`

3. **Embedded component NPEs**
   - Systematic NPE pattern
   - Likely single root cause
   - Start here: `EmbeddedTest.testQueryWithEmbeddedWithNullUsingSubAttributes`

### Strategy

- Focus on categories with systematic failures (same exception, same pattern)
- Fix root causes rather than individual tests
- Target P1/P2 issues first for maximum impact
- Each category fix could resolve 10-30 tests

### Comparison to Legacy Queue

Legacy queue has only 44 failures, so graph queue has **348 additional failures**. This suggests:
- Graph queue is less mature/tested
- Several fundamental issues remain
- But the architecture is sound (we've proven fixes work)
