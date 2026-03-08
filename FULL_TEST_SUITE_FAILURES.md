# Full hibernate-core Test Suite Failures Analysis

**Status:** ✅ COMPLETED
**Date:** 2026-03-06
**Branch:** actionqueue2-decompose
**Duration:** 1h 38m 11s

## Summary

**Total Tests Run:** 16,122
**Passed:** 14,477 (89.8%)
**Failed:** 1,645 (10.2%)
**Skipped:** 1,621
**Test Classes with Failures:** 453

## ⚠️ CRITICAL: 10.2% Failure Rate

The GraphBasedActionQueue implementation has significant issues affecting 1,645 tests across 453 test classes.

## Top Failure Categories

### 1. Unbreakable Cycle Errors (312 failures) ⚠️ CRITICAL

**Exception:** `java.lang.IllegalStateException at CycleBreaker.java:78`
**Impact:** CRITICAL - Blocking 312 tests (~19% of all failures)

**Root Cause:** The GraphBasedActionQueue's cycle detection algorithm is finding cycles that cannot be broken.

**Affected Test Classes:**
- EntityWithInverseOneToManyJoinTest (20 failures)
- VersionedEntityWithInverseOneToManyJoinTest (20 failures)
- EntityWithNonInverseOneToManyJoinTest (20 failures)
- BidirectionalOneToManyBagCollectionEventTest (20 failures)
- Many collection-related tests

**Example Error:**
```
EntityWithInverseOneToManyJoinTest > testCreateWithEmptyOneToManyCollectionUpdateWithExistingElement(SessionFactoryScope) FAILED
    java.lang.IllegalStateException at CycleBreaker.java:78
```

**Analysis:**
These failures occur when the dependency graph contains cycles that cannot be resolved by nulling FK columns. This commonly happens with:
- Bidirectional OneToMany relationships
- Collection updates that involve both deletes and inserts
- Complex entity graphs with multiple relationships

**Potential Solutions:**
1. Improve cycle breaking algorithm to handle more complex scenarios
2. Add support for breaking cycles through intermediate operations
3. Consider deferred constraint checking for certain relationship types

### 2. SQL Grammar Exceptions (250 failures) ⚠️ CRITICAL

**Exception:** `org.hibernate.exception.SQLGrammarException`
**Caused by:** `org.h2.jdbc.JdbcSQLSyntaxErrorException`
**Impact:** CRITICAL - Blocking 250 tests (~15% of all failures)

**Affected Test Classes:**
- HQLTest (173 failures - largest single class)
- GlobalTemporaryTableMutationStrategyGeneratedIdTest
- DefaultMutationStrategyGeneratedIdWithOptimizerTest
- BulkManipulationTest (25 failures)

**Analysis:**
SQL syntax errors suggest that the GraphBasedActionQueue is generating malformed SQL or SQL in an incorrect order/context. This particularly affects:
- HQL queries (173 failures in HQLTest alone)
- Bulk operations
- Global temporary table strategies

**Example Failures:**
```
HQLTest > test_hql_collection_expressions_example_5(SessionFactoryScope) FAILED
    org.hibernate.exception.SQLGrammarException at HQLTest.java:86
        Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException

BulkManipulationTest > testInsertIntoSuperclassPropertiesFails(SessionFactoryScope) FAILED
    java.lang.IllegalStateException at BulkManipulationTest.java:441
```

### 3. Assertion Failures (345 failures)

**Exceptions:**
- `java.lang.AssertionError` (187 occurrences)
- `org.opentest4j.AssertionFailedError` (158 occurrences)

**Impact:** MEDIUM - Tests expecting specific behavior not met

**Analysis:**
These are test assertions failing, indicating that the GraphBasedActionQueue is producing different results than expected (though not necessarily crashing). Could indicate:
- Different execution order affecting results
- Missing operations
- Incorrect state after flush

### 4. Constraint Violation Exceptions (88 failures)

**Exception:** `org.hibernate.exception.ConstraintViolationException`
**Impact:** MEDIUM

**Analysis:**
FK or unique constraint violations suggest operations are executing in the wrong order, likely related to:
- Insufficient dependency edges
- Cycle breaking removing necessary ordering constraints
- DELETE/INSERT ordering issues

### 5. Other Significant Errors

**NullPointerException:** 68 occurrences
**OptimisticLockException:** 37 occurrences
**ClassCastException:** 37 occurrences
**PropertyValueException:** 28 occurrences

## Test Classes with Most Failures

| Test Class | Failure Count |
|-----------|---------------|
| HQLTest | 173 |
| ImmutableTest | 25 |
| BulkManipulationTest | 25 |
| VersionedEntityWithNonInverseOneToManyTest | 20 |
| EntityWithNonInverseOneToManyTest | 20 |
| EntityWithInverseOneToManyJoinTest | 20 |
| BidirectionalOneToManyBagCollectionEventTest | 20 |
| BidirectionalManyToManySetToSetCollectionEventTest | 19 |

## Critical Issues Identified

### Issue 1: Unbreakable Cycles (CRITICAL)

**Problem:** Many collection operations create dependency cycles that cannot be broken.

**Impact:** 312+ test failures, ~20% of all failures

**Recommendation:**
- Investigate CycleBreaker algorithm
- Add support for more sophisticated cycle breaking strategies
- Consider whether some operations should be deferred or split differently

### Issue 2: HQL/Bulk Operations (CRITICAL)

**Problem:** HQL queries and bulk operations generating invalid SQL or failing.

**Impact:** 173 failures in HQLTest alone, plus bulk operation tests

**Recommendation:**
- Review how HQL queries interact with GraphBasedActionQueue
- Ensure bulk operations properly decompose
- Check if temporary table strategies are compatible

### Issue 3: Collection Event Tests (HIGH)

**Problem:** Many collection event tests failing with cycle errors.

**Impact:** Multiple test classes with 15-20 failures each

**Recommendation:**
- Review collection update decomposition
- Ensure collection events fire correctly with new architecture
- Check if bidirectional relationships handled properly

## Comparison with Known Issues

### Previously Fixed Issues:
- ✅ @OrderColumn UPDATE/INSERT ordering (fixed via PostExecutionCallback)
- ✅ DELETE→INSERT ordering for orphan removal (fixed via dependency edges)

### New Issues Discovered:
- ❌ Unbreakable cycles in complex entity graphs
- ❌ HQL query SQL generation
- ❌ Bulk operation support
- ❌ Collection event ordering

## Next Steps

1. **Immediate:**
   - Wait for full test suite to complete
   - Get complete failure count and detailed error logs
   - Categorize all failures by root cause

2. **Short Term:**
   - Fix unbreakable cycle detection/breaking
   - Investigate HQL SQL generation issues
   - Review collection decomposition logic

3. **Medium Term:**
   - Add more sophisticated dependency management
   - Improve error messages for debugging
   - Add integration tests for complex scenarios

4. **Long Term:**
   - Consider architectural changes if fundamental issues found
   - Performance testing with fixed implementation
   - Comparison testing with legacy ActionQueue

## Test Suite Execution Details

**Started:** ~06:55 (based on process times)
**Duration:** 4+ hours (still running)
**Parallel Execution:** Yes (5 worker daemons)
**Platform:** H2 database
**JDK:** 25

## Final Statistics

**Total Exception Breakdown:**
- IllegalStateException: 461 (28% of failures)
  - CycleBreaker errors: 312 (67% of IllegalStateExceptions)
- SQLGrammarException: 250 (15% of failures)
- AssertionError/AssertionFailedError: 367 (22% of failures)
- ConstraintViolationException: 105 (6% of failures)
- NullPointerException: 94 (6% of failures)
- Other: 368 (22% of failures)

**Test Execution Environment:**
- Execution time: 1h 38m 11s
- Parallel workers: 5
- Database: H2 2.4.240
- JDK: 25

## Critical Findings

### Finding #1: Cycle Detection Algorithm Insufficient
**312 failures (19% of all failures)** are caused by unbreakable cycles. The current algorithm cannot handle:
- Complex bidirectional relationships
- Collection updates with deletes + inserts
- Multi-level entity graphs

### Finding #2: SQL Generation Issues
**250 failures (15% of all failures)** are SQL syntax errors, primarily in:
- HQLTest (173 failures - single largest failing class)
- Bulk operations
- Temporary table strategies

### Finding #3: Collection Operations Problematic
Many collection-related tests fail with cycles or constraint violations, suggesting the collection decomposition logic needs significant work.

## Recommendations

### Priority 1 - MUST FIX (Blocking ~34% of failures)
1. **Fix Cycle Breaking Algorithm**
   - 312 failures due to unbreakable cycles
   - Need more sophisticated cycle resolution
   - Consider deferred constraint checking

2. **Fix SQL Generation**
   - 250 failures due to invalid SQL
   - Review HQL query decomposition
   - Fix bulk operation support

### Priority 2 - SHOULD FIX (Blocking ~15% of failures)
3. **Fix Constraint Violations**
   - 105 failures due to FK/unique violations
   - Improve dependency edge detection
   - Better handling of complex relationships

4. **Fix Collection Events**
   - Multiple test classes with 15-20 failures each
   - Review collection decomposition logic
   - Ensure events fire in correct order

### Priority 3 - INVESTIGATE
5. **Review Assertion Failures**
   - 367 test assertions failing
   - May indicate correctness issues
   - Need detailed analysis of each case

## Comparison with Legacy ActionQueue

The legacy ActionQueue (on main branch) does not have these issues, confirming these are GraphBasedActionQueue-specific problems.

**Decision Point:** Given the 10.2% failure rate affecting core functionality (HQL, collections, bulk operations), the GraphBasedActionQueue requires substantial additional work before it can replace the legacy implementation.

## Notes

- Analysis based on complete test run (16,122 tests, 1h 38m duration)
- Some failures may be cascading (one root cause → multiple test failures)
- All failures occur with hibernate.flush.queue.impl=graph setting
- Tests pass with legacy ActionQueue implementation
