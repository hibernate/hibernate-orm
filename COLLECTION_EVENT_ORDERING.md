# Collection Event Ordering in Graph-Based vs Legacy ActionQueue

## Overview

The graph-based ActionQueue implementation fires collection events in a different order than the legacy implementation. This is a deliberate architectural difference due to how the graph-based approach separates action decomposition from execution.

## Event Ordering Differences

### Legacy ActionQueue
Events are paired per action - each action fires PRE, executes SQL, then fires POST:
```
Action A: PRE-A → SQL-A → POST-A
Action B: PRE-B → SQL-B → POST-B
Result: PRE-A, POST-A, PRE-B, POST-B
```

### Graph-Based ActionQueue
All actions are decomposed first (firing PRE events), then all SQL executes, then all POST events fire:
```
Decompose A: PRE-A
Decompose B: PRE-B
Execute all SQL: SQL-A, SQL-B (dependency-ordered)
Finalize all: POST-A, POST-B
Result: PRE-A, PRE-B, POST-A, POST-B
```

## Why This Ordering?

The graph-based approach provides several benefits:

1. **Better Database Consistency**: POST events see ALL SQL changes committed
2. **Cleaner Architecture**: Clear separation of phases (decompose → execute → finalize)
3. **Dependency-Aware Execution**: SQL operations execute in dependency order, not action registration order

## Impact

**User Event Listeners**: This change only affects the relative ordering of events across different collection actions within the same flush. Individual PRE/POST pairs are still fired for each action, just grouped differently.

**No Functional Impact**: The change does not affect Hibernate's internal processing or correctness - it's purely an event ordering difference.

## Test Updates

Tests in `AbstractCollectionEventTest` now check which ActionQueue implementation is active and adjust expectations accordingly using the `isGraphBasedActionQueue()` helper method.

## Example

Consider moving a child from parent1 to parent2:

**Legacy Order:**
1. PRE-UPDATE parent1
2. POST-UPDATE parent1
3. PRE-UPDATE parent2
4. POST-UPDATE parent2

**Graph-Based Order:**
1. PRE-UPDATE parent1
2. PRE-UPDATE parent2
3. POST-UPDATE parent1
4. POST-UPDATE parent2

Both orderings are correct - they just group the events differently.
