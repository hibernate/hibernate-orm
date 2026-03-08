# Per-Action Callback Design

## Problem

Collection event ordering differs between legacy and graph-based ActionQueue:

**Legacy (expected by tests):**
```
PRE-parent → SQL-parent → POST-parent → PRE-other → SQL-other → POST-other
```

**Graph-based (current):**
```
PRE-parent → PRE-other → (all SQL) → POST-parent → POST-other
```

**Root Cause:**
- PRE events fire during decomposition (before SQL)
- POST events fire via callbacks after ALL SQL executes
- Tests expect PRE/POST to be paired per action

## Solution: Per-Action Callback Firing

Track which operations belong to which action, and fire each action's POST callback immediately after its last operation executes.

### Implementation Plan

1. **Track action source in PlannedOperation**
   - Add `actionIndex` field to PlannedOperation
   - During decomposition, assign sequential action index to each action's operations

2. **Track callback per action**
   - During decomposition, map `actionIndex → PostExecutionCallback`
   - Track which operation is the "last" for each action

3. **Fire callbacks during execution**
   - After executing each operation, check if it's the last for its action
   - If yes, fire that action's callback immediately
   - This maintains PRE → SQL → POST pairing per action

### Edge Cases

1. **Operations reordered by graph**
   - Operations from action A might execute before/after operations from action B
   - That's OK - we fire callback when the LAST operation for each action completes
   - Events might not be strictly sequential, but PRE/POST are paired

2. **Actions with zero operations**
   - Some actions decompose to empty operation list (e.g., inverse one-to-many recreate)
   - Fire callback immediately during decomposition if no operations created

3. **Batching**
   - Operations are batched by shape, so action A's ops and action B's ops might batch together
   - That's fine - we track per-operation and fire callbacks independently

### Alternative: Strict Sequential Execution

If we need strict `PRE-A, SQL-A, POST-A, PRE-B, SQL-B, POST-B` ordering, we'd need to:
- Decompose one action at a time
- Execute its operations
- Fire its callback
- Then move to next action

This would prevent batching across actions and hurt performance. **Not recommended.**

### Decision

Implement per-action callback tracking with "fire on last operation" logic. This:
- Maintains PRE/POST pairing per action
- Allows operations to be reordered by dependency graph
- Preserves batching opportunities
- Minor breaking change: events for different actions might interleave based on dependency order
