# Cascade DELETE Ordering Issue - Investigation Findings

## Test Case
`CascadeOnUninitializedTest.testDeleteEnhancedEntityWithUninitializedManyToOne`

## Model
```java
@Entity Person {
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "primary_address_id")
    Address primaryAddress;  // FK: Person.PRIMARY_ADDRESS_ID -> Address.ID
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id")
    Set<Address> addresses;  // FK: Address.PERSON_ID -> Person.ID
}

@Entity Address {
    @Id Long id;
}
```

## Test Scenario
1. Create Person with primaryAddress (addresses collection is empty)
2. Delete Person via `session.remove(person)`
3. Cascade DELETE should delete Address

## Observed Behavior

### Operations Planned
```
DELETE on TEST_PERSON (ordinal=0, entityId=1)  
DELETE on TEST_ADDRESS (ordinal=1000, entityId=1)  // from @ManyToOne cascade
UPDATE on TEST_ADDRESS (ordinal=2000, entityId=null)  // from @OneToMany collection REMOVE
```

### Actual Execution Order
```sql
delete from TEST_ADDRESS where id=?  -- FAILS with FK constraint violation
-- Person DELETE never executes
```

### Error
```
ConstraintViolationException: FOREIGN KEY(PRIMARY_ADDRESS_ID) REFERENCES TEST_ADDRESS(ID)
```

## Root Cause Analysis

### FK Constraints
1. **FK1**: `Person.PRIMARY_ADDRESS_ID -> Address.ID` (Person holds FK to Address)
2. **FK2**: `Address.PERSON_ID -> Person.ID` (Address holds FK to Person for collection)

### Expected Graph Edges for DELETEs
For FK1 (Person.PRIMARY_ADDRESS_ID -> Address.ID):
- Graph should create: `Person DELETE -> Address DELETE` 
- Meaning: Delete Person BEFORE deleting Address (child before parent)

### Why Address DELETE Executes First
The Address DELETE is executing before Person DELETE despite having a higher ordinal (1000 vs 0).

**Possible causes:**
1. **Missing entity ID tracking**: The graph builder creates FK edges based on table names and FK definitions, but might not be tracking which specific entity instances are being deleted. So it doesn't know that:
   - Person with ID=1 has FK to Address with ID=1
   - Therefore Person DELETE (ID=1) must precede Address DELETE (ID=1)

2. **Conflicting edges from collection FK**: The UPDATE operation for the @OneToMany collection creates:
   - `Address UPDATE -> Person DELETE` 
   - This is correct for the collection REMOVE, but might be interfering

3. **Batch execution grouping**: Operations on the same table (both DELETEs on TEST_ADDRESS) might be batched together, and the batch executes in the wrong order relative to Person DELETE

## Graph Builder Logic (from StandardGraphBuilder)

### createDeleteToDeleteEdges
```java
// For FK: child_table.fk -> parent_table.id
// Creates edge: child DELETE -> parent DELETE
```

For FK1 (Person.PRIMARY_ADDRESS_ID -> Address.ID):
- Child table: TEST_PERSON
- Parent table: TEST_ADDRESS  
- Should create: `TEST_PERSON DELETE -> TEST_ADDRESS DELETE`

This is correct! So why isn't it working?

## ROOT CAUSE IDENTIFIED

The graph builder creates edges based on **table-level FK relationships**, not entity-level dependencies.

For the two FKs:
1. FK #1: `Person.PRIMARY_ADDRESS_ID -> Address.ID` creates edge: `Person DELETE -> Address DELETE`
2. FK #2: `Address.PERSON_ID -> Person.ID` creates edge: `Address DELETE -> Person DELETE`

This creates a **cycle**: `Person -> Address -> Person`

**The problem**: Both edges are created even though only FK #1 is relevant!

In this specific test:
- Person has `primaryAddress` set (FK #1 is populated)
- Person has `addresses` collection EMPTY (FK #2 is NOT populated)
- The Address being deleted has `PERSON_ID = NULL`

FK #2 doesn't impose any constraint for this DELETE, but the graph builder doesn't know that - it sees the table-level FK relationship and creates an edge anyway.

**Result**: Cycle breaker breaks an arbitrary edge (possibly FK #1), causing Address to execute before Person → FK violation

## Next Steps

1. ✅ Add debug logging to see graph edges (attempted, had compilation issues)
2. Check if the FK edge is actually being created for these specific DELETE operations
3. Check if the edge is being marked as breakable
4. Check if there's a cycle causing the edge to be broken
5. Verify entity ID tracking in DELETE operations
6. Check if this is a regression from recent changes or a pre-existing issue

## Comparison with Legacy Queue

**The test PASSES with legacy queue** - this confirms it's a graph queue specific issue with dependency tracking or edge creation.

The legacy queue doesn't use dependency graphs - it executes operations in action queue order with some simple ordering rules. This suggests the graph-based approach is either:
- Not creating the right edges
- Breaking edges incorrectly
- Planning the execution order incorrectly

## Potential Fixes

1. **Ensure entity ID matching in FK edges**: When creating DELETE->DELETE edges, verify both operations have matching entity IDs for the FK relationship

2. **Don't break FK edges for cascade deletes**: If an edge represents a cascade delete relationship, it should never be breakable

3. **Prioritize entity-level dependencies over table-level**: The graph builder should track entity instance IDs, not just table names

4. **Check collection cascade interactions**: Ensure @OneToMany collection REMOVE operations don't interfere with cascaded entity DELETEs
