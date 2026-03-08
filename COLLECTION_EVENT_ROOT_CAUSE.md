# Collection Event Root Cause Analysis

## Problem Summary

12 tests in `BidirectionalOneToManyBagCollectionEventTest` fail because they expect **different collection event types** than what the graph-based ActionQueue produces.

**Expected**: PreCollectionRemove, PostCollectionRemove
**Actual**: PreCollectionRecreate, PostCollectionRecreate

## Root Cause: Collection "Reachability" Processing

Hibernate uses a **reachability algorithm** to determine what collection operations to perform:

### The Flow

1. **During flush, entities are visited by `FlushVisitor`** (DefaultFlushEntityEventListener.java:140-153):
   ```java
   if ( entry.getStatus() != Status.DELETED ) {
       if ( persister.hasCollections() ) {
           new FlushVisitor( session, entity )
                   .processEntityPropertyValues( values, persister.getPropertyTypes() );
       }
   }
   ```

   **Key**: Collections of **DELETED** entities are **NOT** visited!

2. **FlushVisitor calls `Collections.processReachableCollection()`** (Collections.java:132-181):
   ```java
   collectionEntry.setCurrentPersister( persister );
   collectionEntry.setCurrentKey( type.getKeyOfOwner( entity, session ) );
   collectionEntry.setReached( true );
   prepareCollectionForUpdate( collection, collectionEntry, factory );
   ```

3. **After visiting, unreachable collections are processed** (AbstractFlushingEventListener.java:347-352):
   ```java
   for ( var entry : identityMap.toArray() ) {
       final var collectionEntry = entry.getValue();
       if ( !collectionEntry.isReached() && !collectionEntry.isIgnore() ) {
           processUnreachableCollection( entry.getKey(), session );
       }
   }
   ```

4. **`processUnreachableCollection()` → `processDereferencedCollection()`** (Collections.java:43-76):
   ```java
   // do the work
   entry.setCurrentPersister( null );  // ← Sets to NULL!
   entry.setCurrentKey( null );
   prepareCollectionForUpdate( collection, entry, session.getFactory() );
   ```

5. **`prepareCollectionForUpdate()` decides the operation** (Collections.java:229-271):
   ```java
   final var loadedPersister = collectionEntry.getLoadedPersister();  // Not null (from DB)
   final var currentPersister = collectionEntry.getCurrentPersister(); // Depends on reachability

   if ( loadedPersister != null || currentPersister != null ) {
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
           // Same owner, dirty → UPDATE
           collectionEntry.setDoupdate( true );
       }
   }
   ```

### The Decision Matrix

| Scenario | loadedPersister | currentPersister | ownerChanged | Result |
|----------|----------------|------------------|--------------|--------|
| **Entity DELETED** | NOT NULL (from DB) | **NULL** (not visited) | **TRUE** | **doremove=true** → REMOVE |
| **Collection replaced** | NOT NULL | NOT NULL (but different) | TRUE | doremove=true + dorecreate=true → REMOVE + RECREATE |
| **Collection modified** | NOT NULL | NOT NULL (same) | FALSE | doupdate=true → UPDATE |
| **Entity normal** | NOT NULL | NOT NULL (same) | FALSE | Based on dirty check |

## Why Tests Are Failing

### Failing Test Pattern #1: DELETE operations

**Tests**:
- testDeleteParentWithNullChildren
- testDeleteParentWithNoChildren
- testDeleteParentAndChild
- testDeleteParentButNotChild

**What they do**:
1. Load parent entity
2. Delete parent: `s.remove(parent)`
3. Flush

**Expected**: PreCollectionRemove, PostCollectionRemove (REMOVE operation)

**What should happen**:
1. Entity status → DELETED
2. FlushVisitor SKIPS collections (entity.getStatus() == DELETED)
3. Collections become "unreachable"
4. `processDereferencedCollection()` sets currentPersister=null
5. `prepareCollectionForUpdate()` sees ownerChanged=true
6. Sets doremove=true → CollectionRemoveAction

**This is CORRECT legacy behavior!**

### Failing Test Pattern #2: Collection Replacement

**Tests**:
- testUpdateParentNullToOneChildDiffCollection
- testUpdateParentNoneToOneChildDiffCollection
- testUpdateParentOneChildDiffCollectionSameChild
- testUpdateParentOneChildDiffCollectionDiffChild
- testMoveChildToDifferentParent
- testMoveCollectionToDifferentParent
- testMoveCollectionToDifferentParentFlushMoveToDifferentParent
- testMoveAllChildrenToDifferentParent

**What they do**:
1. Load parent entity
2. Call `parent.newChildren(new ArrayList())` - creates NEW collection instance
3. Optionally add children to new collection
4. Flush

**Expected**: PreCollectionRemove, PostCollectionRemove (old), PreCollectionRecreate, PostCollectionRecreate (new)

**What should happen**:
1. Old collection instance: Not visited by FlushVisitor (replaced, no longer referenced)
2. New collection instance: Visited by FlushVisitor (current reference)
3. Old collection becomes "unreachable" → doremove=true
4. New collection: currentPersister set → dorecreate=true

**This is ALSO CORRECT legacy behavior!**

## The Actual Problem

If tests are failing, it means the graph-based ActionQueue is NOT seeing the same collection operation decisions (dorecreate/doremove/doupdate flags) as legacy.

### Possible Causes

**Hypothesis 1: FlushVisitor Not Running**
- Graph-based queue might be skipping or deferring FlushVisitor execution
- Collections might not be marked as "reached" correctly

**Hypothesis 2: Collection Processing Order**
- Legacy: FlushVisitor → unreachable processing → action creation → execution
- Graph: May have different timing, affecting which collections are "reached"

**Hypothesis 3: Status Timing**
- Entity status (DELETED) might be set at different times
- FlushVisitor might run before/after status changes

**Hypothesis 4: Inverse One-To-Many Special Case**
- This mapping has `inverse="true"` on the collection
- No collection table exists (FK is on CHILD table)
- For inverse collections:
  - InsertRowsCoordinator returns empty operations
  - But events should still fire

- Graph-based approach may be treating inverse collections differently

## Investigation Next Steps

### Step 1: Verify FlushVisitor Execution
Check if FlushVisitor is running for graph-based queue:
- Is `Collections.processReachableCollection()` being called?
- Are collections marked as `reached=true`?

### Step 2: Check Collection Entry State
Before calling `flushCollections()`, inspect:
- What are dorecreate/doremove/doupdate flags?
- What are loadedPersister/currentPersister values?
- Are they the same between legacy and graph-based?

### Step 3: Trace Deletion Path
For delete tests, verify:
- When is entity status set to DELETED?
- Is FlushVisitor skipping DELETED entities correctly?

### Step 4: Trace Replacement Path
For "DiffCollection" tests, verify:
- Are both old and new collections in CollectionEntry map?
- Is old collection marked as unreachable?
- Is new collection marked as reached?

## Key Files

- **Collections.java**: Reachability algorithm, operation decision logic
- **DefaultFlushEntityEventListener.java**: FlushVisitor invocation (line 150)
- **AbstractFlushingEventListener.java**: unreachable collection processing (line 347), action creation (line 278-328)
- **CollectionEntry.java**: Collection state tracking (reached, doremove, dorecreate, doupdate flags)

## Expected vs Actual SQL

For **inverse one-to-many** (like BidirectionalOneToManyBag):
- Collection has NO table (inverse="true")
- FK is on CHILD table (child.parent_id)
- CollectionRecreateAction → NO SQL (InsertRowsCoordinatorNoOp)
- CollectionRemoveAction → NO SQL (inverse)
- CollectionUpdateAction → NO SQL (inverse)

**But events must still fire!** This is what the callback fix addressed (commit 6f2b9f8e10).

The remaining issue is: **Which action type is created** (RECREATE vs REMOVE vs UPDATE), not whether callbacks execute.

## Debugging Commands

```bash
# Run single failing delete test
./gradlew :hibernate-core:test --tests "BidirectionalOneToManyBagCollectionEventTest.testDeleteParentWithNoChildren" --info

# Run single failing replacement test
./gradlew :hibernate-core:test --tests "BidirectionalOneToManyBagCollectionEventTest.testUpdateParentNullToOneChildDiffCollection" --info

# Look for collection processing logs
grep -i "collection\|reached\|doremove\|dorecreate"
```

## Hypothesis: The Real Issue

Looking at the test results:
- 9 tests pass (including testSaveParentOneChild)
- 12 tests fail (all involving DELETE or collection replacement)

This suggests:
- Basic collection creation works (RECREATE)
- Basic collection updates work (UPDATE)
- **Problem**: REMOVE operations are being replaced by RECREATE+REMOVE or just RECREATE

Most likely cause: **Collections of deleted entities are being visited by FlushVisitor when they shouldn't be**, causing:
- currentPersister to be set (instead of null)
- ownerChanged logic to fire incorrectly
- Wrong combination of dorecreate/doremove flags
