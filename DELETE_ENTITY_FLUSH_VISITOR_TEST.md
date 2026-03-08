# FlushVisitor and Deleted Entities Investigation

## Question
Is FlushVisitor being called correctly for deleted entities, and are their collections being processed properly?

## Code Analysis

### FlushVisitor Invocation
From `DefaultFlushEntityEventListener.onFlushEntity()` (lines 140-153):

```java
if ( entry.getStatus() != Status.DELETED ) {
    final var persister = entry.getPersister();
    // now update the object
    // has to be outside the main if block above (because of collections)
    if ( substitute ) {
        persister.setValues( entity, values );
    }
    // Search for collections by reachability, updating their role.
    // We don't want to touch collections reachable from a deleted object
    if ( persister.hasCollections() ) {
        new FlushVisitor( session, entity )
                .processEntityPropertyValues( values, persister.getPropertyTypes() );
    }
}
```

**Finding**: FlushVisitor is **NOT** called for entities with `Status.DELETED`.

Comment says: "We don't want to touch collections reachable from a deleted object"

### Delete Handling
From `DefaultDeleteEventListener.deleteEntity()`:

1. Entity status set to DELETED (line 388)
2. Collections handled in `createDeletedState()` (lines 456-467)
3. Uninitialized lazy collections are loaded if needed for removal or caching
4. EntityDeleteAction is queued

### Collection Removal
From `DefaultDeleteEventListener.deleteOwnedCollections()` (lines 118-132):

```java
private static void deleteOwnedCollections(Type type, Object key, EventSource session) {
    if ( type instanceof CollectionType collectionType ) {
        final var persister =
                session.getFactory().getMappingMetamodel()
                        .getCollectionDescriptor( collectionType.getRole() );
        if ( !persister.isInverse() && !skipRemoval( session, persister, key ) ) {
            session.getActionQueue().addAction( new CollectionRemoveAction( persister, key, session ) );
        }
    }
    else if ( type instanceof ComponentType componentType ) {
        for ( Type subtype : componentType.getSubtypes() ) {
            deleteOwnedCollections( subtype, key, session );
        }
    }
}
```

This is called for unloaded proxies being deleted.

## Potential Issues

### Issue 1: Inverse Collections Not Removed
`deleteOwnedCollections()` skips inverse collections:
```java
if ( !persister.isInverse() && !skipRemoval( session, persister, key ) )
```

For inverse one-to-many (e.g., Parent.children with `mappedBy="parent"`), no CollectionRemoveAction is created.

### Issue 2: Loaded Entity Collections
For loaded entities being deleted:
- FlushVisitor is NOT called
- Collections are not marked as "unreachable" via `processReachableCollection()`
- Collections might not get proper remove actions scheduled

### Issue 3: Collection Reachability
When an entity is deleted:
- Its collections should become "unreachable"
- But FlushVisitor (which normally handles reachability) is skipped
- The collection reachability algorithm might not run

## Questions to Verify

1. Are collections from deleted entities being properly removed?
2. Do inverse one-to-many collections get handled correctly?
3. Are collection events (PRE/POST_COLLECTION_REMOVE) firing for deleted entity collections?
4. Does skipping FlushVisitor cause any missing remove actions?

## Test Scenario

Test case: Delete a parent entity that owns a one-to-many collection
- Setup: Parent with children (inverse one-to-many)
- Action: Delete parent
- Expected: Collection remove action should be created
- Verify: Check if CollectionRemoveAction is in the action queue
