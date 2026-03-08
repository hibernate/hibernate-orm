# Collection Event Owner ID Bug - ROOT CAUSE FOUND

## Problem

Tests fail with: `expected: <1> but was: <null>`

The collection event's owner ID is null when it should be the parent entity's ID.

## Root Cause

The graph-based ActionQueue uses `PostExecutionCallback` to fire POST collection events **AFTER** all SQL executes. But by that time, the collection's owner reference may have been cleared.

### Code Path Comparison

#### Legacy Path (CollectionRemoveAction)
```java
// CollectionRemoveAction.java:38-53
public CollectionRemoveAction(...) {
    super( persister, collection, id, session );
    // ✅ CAPTURES owner at action creation time (during flushCollections)
    this.affectedOwner = session.getPersistenceContextInternal()
                                .getLoadedCollectionOwnerOrNull( collection );
}

// CollectionRemoveAction.java:152-154
private PostCollectionRemoveEvent newPostCollectionRemoveEvent() {
    // ✅ Uses captured affectedOwner field
    return new PostCollectionRemoveEvent( getPersister(), getCollection(),
                                         eventSource(), affectedOwner );
}
```

**Result**: Owner ID is available because it was captured before collection was cleared.

#### Graph-Based Path (PostCollectionRecreateHandling)
```java
// PostCollectionRecreateHandling.java:78-84
private PostCollectionRecreateEvent newPostCollectionRecreateEvent(SessionImplementor session) {
    // ❌ NO captured owner - passes collection directly
    return new PostCollectionRecreateEvent(
            action.getPersister(),
            action.getCollection(),  // ← Problem!
            (org.hibernate.event.spi.EventSource) session
    );
}

// PostCollectionRecreateEvent.java:19-30
public PostCollectionRecreateEvent(...) {
    super(
        collectionPersister,
        collection,
        source,
        collection.getOwner(),  // ← Gets owner from collection at EVENT time
        getOwnerIdOrNull( collection.getOwner(), source )
    );
}
```

**Result**: By the time the callback fires (after all SQL), `collection.getOwner()` may be null or cleared, so owner ID is null.

## Why CollectionRemoveAction Works But CollectionRecreateAction Doesn't

### CollectionRemoveAction
- **Has** `affectedOwner` field (line 22)
- **Captures** owner in constructor (line 52)
- **Uses** captured owner in event creation (line 153)
- **Works** in both legacy and graph-based modes

### CollectionRecreateAction
- **No** `affectedOwner` field
- **Legacy mode**: Fires events inline during `execute()` → collection.getOwner() is still valid ✅
- **Graph-based mode**: Fires events via callback AFTER execution → collection.getOwner() may be null ❌

## The Fix

### Option 1: Add affectedOwner to CollectionRecreateAction (Recommended)

**File**: `CollectionRecreateAction.java`

```java
public final class CollectionRecreateAction extends CollectionAction {
+   private final Object affectedOwner;  // ← Add field

    public CollectionRecreateAction(...) {
        super( persister, collection, id, session );
+       // Capture owner at action creation time
+       this.affectedOwner = collection.getOwner();
    }

+   public Object getAffectedOwner() {
+       return affectedOwner;
+   }
}
```

**File**: `PostCollectionRecreateHandling.java`

```java
private PostCollectionRecreateEvent newPostCollectionRecreateEvent(SessionImplementor session) {
    return new PostCollectionRecreateEvent(
            action.getPersister(),
            action.getCollection(),
            (org.hibernate.event.spi.EventSource) session,
-           // Uses constructor that calls collection.getOwner()
+           action.getAffectedOwner()  // ← Use captured owner
    );
}
```

**But wait**: PostCollectionRecreateEvent doesn't have a constructor that takes `affectedOwner`!

Need to also add a constructor to `PostCollectionRecreateEvent.java`:

```java
@Internal
public PostCollectionRecreateEvent(
        CollectionPersister collectionPersister,
        PersistentCollection<?> collection,
        EventSource source,
+       Object affectedOwner) {  // ← New constructor
    super(
        collectionPersister,
        collection,
        source,
+       affectedOwner,
+       getOwnerIdOrNull( affectedOwner, source )
    );
}
```

### Option 2: Use LoadedCollectionOwner in Callback

**File**: `PostCollectionRecreateHandling.java`

```java
private PostCollectionRecreateEvent newPostCollectionRecreateEvent(SessionImplementor session) {
+   // Get the owner from persistence context (same as CollectionRemoveAction does)
+   final Object affectedOwner = session.getPersistenceContextInternal()
+                                       .getLoadedCollectionOwnerOrNull(action.getCollection());
+
    return new PostCollectionRecreateEvent(
            action.getPersister(),
            action.getCollection(),
            (org.hibernate.event.spi.EventSource) session,
+           affectedOwner
    );
}
```

But this still requires the new constructor in PostCollectionRecreateEvent.

### Option 3: Capture Owner in Action and Store It

This is really the same as Option 1, which is the cleanest approach.

## Why This Affects Delete Tests

For delete tests:
1. `session.remove(parent)` sets entity status to DELETED
2. During `flushCollections()`, CollectionRemoveAction is created
3. CollectionRemoveAction constructor captures `affectedOwner` from collection
4. Later, during execution, collection owner may be cleared
5. When PostCollectionRemoveHandling fires event, it uses captured `affectedOwner` ✅

But if the test was somehow creating CollectionRecreateAction instead of CollectionRemoveAction (wrong operation strategy), then:
1. CollectionRecreateAction is created (no affectedOwner captured)
2. During execution, collection owner is cleared
3. PostCollectionRecreateHandling tries to get owner from `collection.getOwner()` → null ❌

## Why Collection Replacement Tests Fail

For "DiffCollection" tests:
1. Old collection instance gets dereferenced → should create CollectionRemoveAction
2. New collection instance gets referenced → should create CollectionRecreateAction
3. If both fire correctly, we'd expect both events

But if the wrong action type is created, or if both are RECREATE actions, then:
- We might get RECREATE events for both old and new collections
- Owner ID would be null for events fired after owner is cleared

## Next Steps

1. **Verify which action types are being created**
   - Add logging to see if CollectionRemoveAction or CollectionRecreateAction is created
   - Check if doremove/dorecreate flags are set correctly

2. **Implement the fix**
   - Add `affectedOwner` field to CollectionRecreateAction
   - Add `getAffectedOwner()` method
   - Add constructor to PostCollectionRecreateEvent that takes affectedOwner
   - Update PostCollectionRecreateHandling to use captured owner

3. **Test the fix**
   - Run failing tests to see if owner ID is now correct
   - Check if this resolves the event type mismatches too

## Files to Modify

1. **CollectionRecreateAction.java** - Add affectedOwner field and getter
2. **PostCollectionRecreateEvent.java** - Add constructor that takes affectedOwner
3. **PostCollectionRecreateHandling.java** - Use action.getAffectedOwner()
4. **CollectionUpdateAction.java** - Same fix needed (not investigated yet)
5. **PostCollectionUpdateHandling.java** - Same fix needed (not investigated yet)

## Impact

This should fix:
- Owner ID being null in collection events ✅
- Potentially some of the event type mismatches (if wrong actions were created due to null owner issues)

This won't fix:
- Wrong collection operation strategy selection (dorecreate vs doremove)
- FlushVisitor not running correctly for deleted entities
- Collection reachability issues
Those are separate issues that need investigation.
