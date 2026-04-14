# Cascade DELETE Constraint Violation Issue

## Problem
`CascadeOnUninitializedTest.testDeleteEnhancedEntityWithUninitializedManyToOne` fails with:
```
ConstraintViolationException: FOREIGN KEY(PRIMARY_ADDRESS_ID) REFERENCES TEST_ADDRESS(ID)
delete from TEST_ADDRESS where id=?
```

## Model
```java
@Entity Person {
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "primary_address_id")
    Address primaryAddress;  // FK from Person to Address
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "person_id")
    Set<Address> addresses;  // FK from Address to Person
}
```

## Test Flow
1. Create Person with primaryAddress set (addresses collection is empty)
2. Delete Person
3. Cascade deletes Address (from @ManyToOne)

## Expected DELETE Order
1. DELETE Person (removes FK reference)
2. DELETE Address (can be deleted after Person)

## Actual Behavior
Graph queue tries to DELETE Address before Person, violating FK constraint.

## Analysis
The FK constraint is: `Person.PRIMARY_ADDRESS_ID -> Address.ID`

For DELETEs, the graph builder should create edge: `DELETE Person -> DELETE Address`

But the error shows Address is being deleted first, suggesting:
- Either the edge isn't being created
- Or there's a conflicting edge creating a cycle
- Or the cascaded DELETE isn't being properly tracked with its entity ID

## Stack Trace
```
at org.hibernate.action.queue.decompose.collection.AbstractOneToManyDecomposer$RemoveBindPlan.execute
```

Note: It's in "OneToMany" decomposer, not "ManyToOne" decomposer.

## Questions
1. Is the @OneToMany `addresses` collection somehow interfering?
2. Is the cascaded Address DELETE being created without proper entity ID tracking?
3. Is there a bi-directional cascade issue where both sides try to delete the same Address?

## Next Steps
- Add debugging to see what PlannedOperations are created
- Check if the Address ID is properly tracked in the DELETE operation
- Verify graph edges are created correctly between Person DELETE and Address DELETE
