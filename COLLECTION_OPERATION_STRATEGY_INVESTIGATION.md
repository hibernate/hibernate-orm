# Collection Operation Strategy Investigation

## Status

**Problem**: 12 out of 21 tests in BidirectionalOneToManyBagCollectionEventTest are failing with wrong collection event types.

**Tests Passing**: 9 tests (up from 2 before callback fix)
**Tests Failing**: 12 tests
**Commit Applied**: 6f2b9f8e10 - Fixed POST collection event firing when no SQL operations are generated

## Error Pattern

All failing tests fail at the same assertion locations:
- Line 857: `assertSame(listenerExpected, listeners.getListenersCalled().get(index))`
- Line 862: `assertEquals(ownerExpected.getId(), ...)`

This means the **wrong type of collection event** is being fired.

## Examples of Event Mismatches

Based on earlier test runs, the pattern is:

```
Expected: PostCollectionRemoveListener
Actual:   PreCollectionRecreateListener
```

This indicates that:
- **Legacy ActionQueue**: Uses REMOVE operation for these scenarios
- **Graph-based ActionQueue**: Uses RECREATE operation for the same scenarios

## Root Cause: Collection Operation Strategy Selection

The decision about which collection operation to use (RECREATE vs REMOVE vs UPDATE) happens in:

**File**: `Collections.java:229-271` - `prepareCollectionForUpdate()`

```java
private static void prepareCollectionForUpdate(
        PersistentCollection<?> collection,
        CollectionEntry collectionEntry,
        SessionFactoryImplementor factory) {

    final var loadedPersister = collectionEntry.getLoadedPersister();
    final var currentPersister = collectionEntry.getCurrentPersister();

    if ( loadedPersister != null || currentPersister != null ) {
        // Check if owner changed
        final boolean ownerChanged =
                loadedPersister != currentPersister
                    || wasKeyChanged( collectionEntry, factory, currentPersister );

        if ( ownerChanged ) {
            // Owner changed → RECREATE + REMOVE
            if ( currentPersister != null ) {
                collectionEntry.setDorecreate( true );
            }
            if ( loadedPersister != null ) {
                collectionEntry.setDoremove( true );
            }
        }
        else if ( collection.isDirty() ) {
            // Same owner, dirty elements → UPDATE
            collectionEntry.setDoupdate( true );
        }
    }
}
```

### Decision Logic

1. **Owner Changed** (loadedPersister != currentPersister OR key changed):
   - Sets `dorecreate = true` (if currentPersister exists)
   - Sets `doremove = true` (if loadedPersister exists)
   - Results in: CollectionRecreateAction + CollectionRemoveAction
   - Events: PreCollectionRecreate, PostCollectionRecreate, PreCollectionRemove, PostCollectionRemove

2. **Same Owner, Dirty Collection**:
   - Sets `doupdate = true`
   - Results in: CollectionUpdateAction
   - Events: PreCollectionUpdate, PostCollectionUpdate

3. **Same Owner, Clean Collection**:
   - No flags set
   - No action created
   - No events

## How currentPersister and loadedPersister are Set

**loadedPersister**: Set when collection is loaded from DB
- Represents the collection's state from the database
- Set in `CollectionEntry` constructor when loading

**currentPersister**: Set during flush by `Collections.processReachableCollection()`
- Line 152: `collectionEntry.setCurrentPersister(persister)`
- Line 154: `collectionEntry.setCurrentKey(type.getKeyOfOwner(entity, session))`
- Represents the collection's current owner relationship

## Hypothesis: Why Graph-Based Queue Sees Different Strategy

The graph-based ActionQueue may be affecting when/how `processReachableCollection()` is called, leading to:

1. **Different timing** of when currentPersister/currentKey are set
2. **Different order** of collection processing
3. **State changes** between when collections are marked and when they're processed

Since the graph-based approach:
- Decomposes all actions first
- Then builds dependency graph
- Then executes in topological order

Compared to legacy which:
- Processes entities/collections in FIFO order
- Executes immediately in that order

The timing difference could cause:
- Collections to be dereferenced before being processed
- Parent entity changes to propagate differently to collection entries
- Different snapshot comparison results

## Failing Test Names

```
testUpdateParentNullToOneChildDiffCollection
testUpdateParentNoneToOneChildDiffCollection
testDeleteParentAndChild
testUpdateParentOneChildDiffCollectionSameChild
testDeleteParentWithNoChildren
testUpdateParentOneChildDiffCollectionDiffChild
testMoveChildToDifferentParent
testMoveCollectionToDifferentParent
testDeleteParentWithNullChildren
testMoveCollectionToDifferentParentFlushMoveToDifferentParent
testMoveAllChildrenToDifferentParent
testDeleteParentButNotChild
```

### Pattern Analysis

Common themes in failing tests:
- **DiffCollection**: Parent gets a different collection instance
- **MoveCollection**: Collection moved to different parent
- **Delete**: Parent deletion scenarios
- **Null**: Setting collection to null

These all involve **owner changes** or **dereference** scenarios where:
- Legacy sees it as a simple UPDATE or REMOVE
- Graph-based sees it as RECREATE + REMOVE (owner change)

## Next Investigation Steps

### Step 1: Verify currentPersister/loadedPersister Values

Add logging to `prepareCollectionForUpdate()` to see:
- What values loadedPersister and currentPersister have
- Whether ownerChanged is true/false
- Which flags get set (dorecreate, doremove, doupdate)

Compare logs between:
- Legacy ActionQueue run
- Graph-based ActionQueue run

### Step 2: Check When Collections Are Reached

Add logging to `Collections.processReachableCollection()` to see:
- When it's called
- What entity owns the collection
- What the collection state is

### Step 3: Identify Ordering Differences

The graph-based approach processes operations in different order. This might affect:
- Entity state when collections are processed
- Collection snapshots
- Owner references

### Step 4: Examine Specific Failing Test

Pick one test like `testUpdateParentNullToOneChildDiffCollection` and:
1. Run with legacy ActionQueue
2. Run with graph-based ActionQueue
3. Compare execution traces
4. Identify exact point where divergence happens

## Questions to Answer

1. **Is this a bug or a valid alternative strategy?**
   - Does RECREATE+REMOVE produce the same SQL as UPDATE/REMOVE?
   - Are there semantic differences in the results?

2. **Should we match legacy behavior exactly?**
   - Is the event sequence part of the public API contract?
   - Do applications depend on specific event ordering?

3. **Can we adjust when processReachableCollection is called?**
   - Should it happen before decomposition?
   - Should it happen during decomposition?
   - Does it matter when it happens?

## Potential Solutions

### Option A: Match Legacy Event Order (Complex)
Modify graph-based queue to ensure collections are processed in the same order and timing as legacy, preserving the same dorecreate/doremove/doupdate decisions.

**Pros**: Perfect compatibility with existing tests and applications
**Cons**: May reduce benefits of graph-based approach, complex to implement

### Option B: Update Test Expectations (Simple)
If the SQL and end results are the same, update tests to accept the new event sequences.

**Pros**: Simple, tests validate correct SQL execution
**Cons**: Breaking change if event order is part of public API

### Option C: Normalize Collection Processing (Medium)
Ensure `processReachableCollection()` and `prepareCollectionForUpdate()` are called at consistent times regardless of ActionQueue implementation.

**Pros**: More predictable behavior, easier to reason about
**Cons**: May require refactoring of flush coordination

## Related Files

- **Collections.java:229-271** - Collection operation strategy decision
- **Collections.java:132-181** - processReachableCollection() sets currentPersister
- **AbstractFlushingEventListener.java:265-334** - Creates collection actions based on flags
- **CollectionEntry.java:179-206** - preFlush() resets flags
- **CollectionEntry.java:234-246** - afterAction() updates loadedPersister

## Test Results

```bash
./gradlew :hibernate-core:test --tests "BidirectionalOneToManyBagCollectionEventTest"
# Result: 21 tests completed, 12 failed
```

All failures are assertion errors at checkResult() calls, verifying listener types and event order.
