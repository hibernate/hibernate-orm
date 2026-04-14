# Current Failure Analysis (236 failures after decompose fix)

## Exception Type Breakdown

1. **AssertionFailedError (56) + AssertionError (34) = 90 total**
   - Test expectations not met
   - Wrong values, wrong counts, wrong states

2. **NullPointerException (24)**
   - Missing initialization or state
   - Uninitialized fields/references

3. **HibernateException (26)**
   - Various Hibernate-specific errors
   - Configuration or usage issues

4. **ConstraintViolationException (20)**
   - FK constraint violations
   - Unique constraint violations
   - Still some ordering issues

5. **RuntimeException (18)**
   - General runtime errors
   - Often wrapping other exceptions

6. **AssertionFailure (17)**
   - Hibernate internal assertions
   - Unexpected states

7. **RollbackException (14)**
   - Transaction rollbacks due to errors

8. **Other (27)**
   - OptimisticLockException (6)
   - IllegalStateException (4)
   - NotSupportedException (4)
   - Various others

## Test Class Breakdown (Top 20)

1. **EmbeddedTest (18)** - Pre-existing, OptionalTableUpdate MERGE issues
2. **ComponentTest (9)** - Pre-existing, component handling issues  
3. **QueryLockingTest (6)** - Locking/versioning issues
4. **ConstraintViolationExceptionHandlingTest (5)** - Constraint handling tests
5. **TransientObjectExceptionHandlingTest (4)** - Exception handling tests
6. **NestedEmbeddableWithLockingDeletionTest (4)** - Embedded + locking
7. **MergeTest (4)** - Merge operation issues
8. **CurrentSessionConnectionTest (4)** - Session connection issues
9. **BidirectionalLazyTest (4)** - Enhanced entities + lazy loading
10. **AggressiveReleaseTest (4)** - Connection release issues
11. **WhereTest (3)** - @Where annotation issues
12. **OneToManyCustomSqlMutationsTest (3)** - Custom SQL mutations
13. **Enhanced:FetchGraphTest (3)** - Enhanced entities + fetch graphs
14. **DeleteDecomposerTest (3)** - Delete decomposition tests
15. Others with 2-3 failures each

## Categories of Issues

### 1. Pre-Existing Issues (Not Caused by Decompose Fix)
- EmbeddedTest (18) - MERGE operation for secondary tables
- ComponentTest (9) - Component query/persistence issues
- **Total: ~27 failures**

### 2. Bytecode Enhancement Issues (~30-40 failures)
Tests with "Enhanced:" prefix having issues with:
- Lazy loading and dirty checking
- Cascade operations on uninitialized entities
- Fetch graphs with enhanced entities
- Collection operations with enhanced entities

### 3. Locking/Versioning Issues (~15 failures)
- QueryLockingTest (6)
- NestedEmbeddableWithLockingDeletionTest (4)
- OptimisticLockTest (2)
- Version increments, lock acquisition

### 4. Constraint Violations (~20 failures)
- ConstraintViolationException still occurring
- FK ordering in complex scenarios
- Unique constraint violations
- Some custom SQL mutation issues

### 5. Connection/Transaction Management (~15 failures)
- CurrentSessionConnectionTest (4)
- AggressiveReleaseTest (4)
- SessionJdbcBatchTest (2)
- Connection release timing
- Transaction boundaries

### 6. Merge/Detached Entity Issues (~10 failures)
- MergeTest (4)
- MergeMultipleEntityCopiesAllowedTest (2)
- Merge operations not working correctly

### 7. Custom SQL Mutations (~6 failures)
- OneToManyCustomSqlMutationsTest (3)
- Custom DELETE/UPDATE operations

### 8. Temporal/Versioned Data (~10 failures)
- TemporalEntityTest (2)
- TemporalEntityHistoryTest (2)
- TemporalEntityHistoryServerSideTest (2)
- TemporalEntityTxIdTest (2)

### 9. Query-Related Issues (~15 failures)
- WhereTest (3)
- Query caching, query locks
- Query parameter handling

### 10. Miscellaneous (~40 failures)
- Various edge cases
- Specific feature interactions
- Test infrastructure issues

## Comparison to Baseline

**Before decompose fix:** 297 failures
**After decompose fix:** 236 failures
**Improvement:** 61 tests fixed ✅

## Priority for Investigation

1. **Bytecode Enhancement Issues** (~30-40 failures)
   - Large category affecting enhanced entities
   - May have common root cause

2. **Constraint Violations** (~20 failures)
   - Should be solvable with better ordering
   - Graph queue should handle this

3. **Locking/Versioning** (~15 failures)
   - May be related to action execution order
   - Version updates timing

4. **Connection/Transaction** (~15 failures)
   - May be test infrastructure issues
   - Or action queue lifecycle issues

5. **Custom SQL Mutations** (~6 failures)
   - Related to earlier fixes
   - May need more work

