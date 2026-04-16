/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.QueueType;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.action.queue.decompose.entity.DeleteDecomposerStandard;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.action.queue.decompose.entity.InsertDecomposer;
import org.hibernate.action.queue.decompose.entity.UpdateDecomposer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test combining Decomposer, GraphBuilder, and FlushPlanner
 * to test the complete flow from entity actions to executable flush plan.
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		DecomposerGraphPlannerIntegrationTest.Person.class,
		DecomposerGraphPlannerIntegrationTest.Address.class,
		DecomposerGraphPlannerIntegrationTest.Department.class,
		DecomposerGraphPlannerIntegrationTest.Employee.class
})
@SessionFactory
public class DecomposerGraphPlannerIntegrationTest {
	private final PlanningOptions planningOptions = new PlanningOptions(
			true,
			false,
			true,
			true,
			PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
	);

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSimpleInsertDecomposition(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = (SessionImplementor) session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create entity
			final Person person = new Person();
			person.setName("John Doe");

			// Get persister and decomposer
			final EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor(Person.class);
			final InsertDecomposer decomposer = new InsertDecomposer(persister, factory);

			// Create insert action
			final Object id = 1L;
			final Object[] state = persister.getValues(person);
			final EntityInsertAction action = new EntityInsertAction(
					id, state, person, null, persister, false, (EventSource) sessionImpl
			);

			// Decompose
			final List<PlannedOperation> operations = decomposer.decompose(action, 0, sessionImpl, null);
			final List<PlannedOperationGroup> groups = groupOperations(operations);

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify single table insert
			assertEquals(1, groups.size(), "Simple entity should have 1 group");

			// Verify all operations are in the plan
			int totalOps = 0;
			for ( PlanStep step : plan.steps()) {
				totalOps += step.operations().size();
			}
			assertEquals(1, totalOps, "Plan should contain the insert operation");
		});
	}

	@Test
	public void testInsertWithForeignKeyDependency(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create entities with FK relationship: Person <- Address
			final Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");

			final Address address = new Address();
			address.setId(2L);
			address.setStreet("123 Main St");
			address.setPerson(person);

			// Get persisters and decomposers
			final EntityPersister personPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Person.class);
			final EntityPersister addressPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Address.class);

			final InsertDecomposer personDecomposer = new InsertDecomposer(personPersister, factory);
			final InsertDecomposer addressDecomposer = new InsertDecomposer(addressPersister, factory);

			// Create insert actions
			final Object[] personState = personPersister.getValues(person);
			final EntityInsertAction personAction = new EntityInsertAction(
					person.getId(), personState, person, null, personPersister, false, (EventSource) sessionImpl
			);

			final Object[] addressState = addressPersister.getValues(address);
			final EntityInsertAction addressAction = new EntityInsertAction(
					address.getId(), addressState, address, null, addressPersister, false, (EventSource) sessionImpl
			);

			// Decompose both actions
			final List<PlannedOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(groupOperations(personDecomposer.decompose(personAction, 0, sessionImpl, null)));
			allGroups.addAll(groupOperations(addressDecomposer.decompose(addressAction, 1, sessionImpl, null)));

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(allGroups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify FK ordering: Person must come before Address
			final List<PlannedOperation> allOps = new ArrayList<>();
			for ( PlanStep step : plan.steps()) {
				allOps.addAll(step.operations());
			}

			assertEquals(2, allOps.size(), "Should have 2 insert operations");

			// Find operation indices
			int personIndex = -1;
			int addressIndex = -1;
			for (int i = 0; i < allOps.size(); i++) {
				String table = allOps.get(i).getTableExpression().toLowerCase();
				if (table.contains("person")) {
					personIndex = i;
				} else if (table.contains("address")) {
					addressIndex = i;
				}
			}

			assertTrue(personIndex >= 0, "Person operation should be in plan");
			assertTrue(addressIndex >= 0, "Address operation should be in plan");
			assertTrue(personIndex < addressIndex, "Person should be inserted before Address (FK constraint)");
		});
	}

	@Test
	public void testUpdateDecomposition(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = (SessionImplementor) session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create and "persist" entity
			final Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");

			// Simulate loaded state
			final EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor(Person.class);

			final Object[] previousState = new Object[]{"Jane Doe"};
			final Object[] currentState = persister.getValues(person);

			// Create update action
			final EntityUpdateAction action = new EntityUpdateAction(
					person.getId(),
					currentState,
					new int[]{0}, // dirty field index
					false,
					previousState,
					null, // version
					null, // nextVersion
					person,
					null, // rowId
					persister,
					(EventSource) sessionImpl
			);

			// Get decomposer
			final UpdateDecomposer decomposer = new UpdateDecomposer(persister, factory);

			// Decompose
			final List<PlannedOperation> operations = decomposer.decompose(action, 0, sessionImpl, null);
			final List<PlannedOperationGroup> groups = groupOperations(operations);

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify single table update
			assertEquals(1, groups.size(), "Simple entity should have 1 group");
		});
	}

	@Test
	public void testDeleteDecomposition(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = (SessionImplementor) session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create entity to delete
			final Person person = new Person();
			person.setId(1L);
			person.setName("John Doe");

			final EntityPersister persister = factory.getMappingMetamodel()
					.getEntityDescriptor(Person.class);

			// Create delete action
			final Object[] state = persister.getValues(person);
			final EntityDeleteAction action = new EntityDeleteAction(
					person.getId(),
					state,
					null, // version
					person,
					persister,
					false,
					(EventSource) sessionImpl
			);

			// Get decomposer
			final DeleteDecomposerStandard decomposer = new DeleteDecomposerStandard(persister, factory);

			// Decompose
			final List<PlannedOperation> operations = decomposer.decompose(action, 0, sessionImpl, null);
			final List<PlannedOperationGroup> groups = groupOperations(operations);

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify single table delete
			assertEquals(1, groups.size(), "Simple entity should have 1 group");
		});
	}

	@Test
	public void testMixedOperations(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = (SessionImplementor) session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create multiple entities with different operations
			final Person person1 = new Person();
			person1.setId(1L);
			person1.setName("Person 1");

			final Person person2 = new Person();
			person2.setId(2L);
			person2.setName("Person 2");

			final Address address = new Address();
			address.setId(3L);
			address.setStreet("123 Main St");
			address.setPerson(person1);

			// Get persisters
			final EntityPersister personPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Person.class);
			final EntityPersister addressPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Address.class);

			// Get decomposers
			final InsertDecomposer personInsertDecomposer = new InsertDecomposer(personPersister, factory);
			final InsertDecomposer addressInsertDecomposer = new InsertDecomposer(addressPersister, factory);
			final UpdateDecomposer personUpdateDecomposer = new UpdateDecomposer(personPersister, factory);

			// Create actions
			final EntityInsertAction person1InsertAction = new EntityInsertAction(
					person1.getId(),
					personPersister.getValues(person1),
					person1,
					null,
					personPersister,
					false,
					(EventSource) sessionImpl
			);

			final EntityInsertAction addressInsertAction = new EntityInsertAction(
					address.getId(),
					addressPersister.getValues(address),
					address,
					null,
					addressPersister,
					false,
					(EventSource) sessionImpl
			);

			final Object[] person2PreviousState = new Object[]{"Old Name"};
			final EntityUpdateAction person2UpdateAction = new EntityUpdateAction(
					person2.getId(),
					personPersister.getValues(person2),
					new int[]{0},
					false,
					person2PreviousState,
					null,
					null,
					person2,
					null,
					personPersister,
					(EventSource) sessionImpl
			);

			// Decompose all actions
			final List<PlannedOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(groupOperations(personInsertDecomposer.decompose(person1InsertAction, 0, sessionImpl, null)));
			allGroups.addAll(groupOperations(addressInsertDecomposer.decompose(addressInsertAction, 1, sessionImpl, null)));
			allGroups.addAll(groupOperations(personUpdateDecomposer.decompose(person2UpdateAction, 2, sessionImpl, null)));

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(allGroups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Count total operations
			int totalOps = 0;
			for ( PlanStep step : plan.steps()) {
				totalOps += step.operations().size();
			}
			assertEquals(3, totalOps, "Should have 3 operations total");
		});
	}

	@Test
	public void testCyclicDependencyBreaking(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = (SessionImplementor) session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();

			// Create cyclic relationship: Department <-> Employee
			final Department dept = new Department();
			dept.setId(1L);
			dept.setName("Engineering");

			final Employee emp = new Employee();
			emp.setId(2L);
			emp.setName("John");
			emp.setDepartment(dept);

			// Department references Employee as head
			dept.setHead(emp);

			// Get persisters
			final EntityPersister deptPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Department.class);
			final EntityPersister empPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(Employee.class);

			// Get decomposers
			final InsertDecomposer deptDecomposer = new InsertDecomposer(deptPersister, factory);
			final InsertDecomposer empDecomposer = new InsertDecomposer(empPersister, factory);

			// Create insert actions
			final EntityInsertAction deptAction = new EntityInsertAction(
					dept.getId(),
					deptPersister.getValues(dept),
					dept,
					null,
					deptPersister,
					false,
					(EventSource) sessionImpl
			);

			final EntityInsertAction empAction = new EntityInsertAction(
					emp.getId(),
					empPersister.getValues(emp),
					emp,
					null,
					empPersister,
					false,
					(EventSource) sessionImpl
			);

			// Decompose
			final List<PlannedOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(groupOperations(deptDecomposer.decompose(deptAction, 0, sessionImpl, null)));
			allGroups.addAll(groupOperations(empDecomposer.decompose(empAction, 1, sessionImpl, null)));

			final ConstraintModel constraintModel = ConstraintModelBuilder.buildConstraintModel(factory.getMappingMetamodel(),
					planningOptions );

			// Build graph with planning options
			final PlanningOptions planningOptions = new PlanningOptions(
					true,  // orderByForeignKeys
					true,  // orderByUniqueKeySlots
					false, // avoidBreakingDeferrable
					true,  // ignoreDeferrableForOrdering
					PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
			);
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
					constraintModel,
					planningOptions,
					sessionImpl  // Pass session for Phase 2 value extraction
			);
			final Graph graph = graphBuilder.build(allGroups);

			// Verify there are edges in the graph
			assertFalse(graph.outgoing().isEmpty(), "Graph should have edges for FK relationships");

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify plan was created successfully (cycle was broken)
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify at least one operation has a binding patch (cycle was broken)
			boolean patchFound = false;
			for ( PlanStep step : plan.steps()) {
				for (PlannedOperation op : step.operations()) {
					if (op.getBindingPatch() != null) {
						patchFound = true;
						break;
					}
				}
			}
			assertTrue(patchFound, "Cycle breaking should install a binding patch");

			// Verify all operations are in the plan
			int totalOps = 0;
			for ( PlanStep step : plan.steps()) {
				totalOps += step.operations().size();
			}
			assertEquals(2, totalOps, "Should have 2 insert operations");
		});
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Helper methods
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Helper method to group operations by shape (table + kind + SQL).
	 * Mirrors FlushCoordinator's grouping logic.
	 */
	private List<PlannedOperationGroup> groupOperations(List<PlannedOperation> operations) {
		if (operations.isEmpty()) {
			return List.of();
		}

		// Group by shapeKey only (merge operations from different entities)
		// This mirrors FlushCoordinator behavior for non-self-referential tables
		final java.util.Map<StatementShapeKey, OperationGroupBuilder> builders = new java.util.LinkedHashMap<>();

		for (PlannedOperation operation : operations) {
			final StatementShapeKey shapeKey = computeShapeKey(operation);
			var builder = builders.get(shapeKey);
			if (builder == null) {
				// First operation for this key - create new builder (which adds the operation in constructor)
				builder = new OperationGroupBuilder(operation, shapeKey);
				builders.put(shapeKey, builder);
			} else {
				// Subsequent operation for this key - add to existing builder
				builder.addOperation(operation);
			}
		}

		final List<PlannedOperationGroup> groups = new ArrayList<>(builders.size());
		for (OperationGroupBuilder builder : builders.values()) {
			groups.add(builder.build());
		}

		return groups;
	}

	private StatementShapeKey computeShapeKey(PlannedOperation operation) {
		final String table = operation.getTableExpression();
		final MutationKind kind = operation.getKind();

		return switch (kind) {
			case INSERT -> StatementShapeKey.forInsert(table, operation);
			case UPDATE, UPDATE_ORDER -> StatementShapeKey.forUpdate(table, operation);
			case DELETE -> StatementShapeKey.forDelete(table, operation);
			case NO_OP -> StatementShapeKey.forNoOp(table);
		};
	}

	private static class OperationGroupBuilder {
		private final String tableExpression;
		private final MutationKind kind;
		private final StatementShapeKey shapeKey;
		private int ordinal;
		private final String origin;
		private final List<PlannedOperation> operations = new ArrayList<>();

		OperationGroupBuilder(PlannedOperation firstOperation, StatementShapeKey shapeKey) {
			this.tableExpression = firstOperation.getTableExpression();
			this.kind = firstOperation.getKind();
			this.shapeKey = shapeKey;
			this.ordinal = firstOperation.getOrdinal();
			this.origin = firstOperation.getOrigin();
			this.operations.add(firstOperation);
		}

		void addOperation(PlannedOperation op) {
			this.operations.add(op);
			// Track minimum ordinal when merging operations
			this.ordinal = Math.min(this.ordinal, op.getOrdinal());
		}

		PlannedOperationGroup build() {
			final boolean needsIdPrePhase = false;
			return new PlannedOperationGroup(
					tableExpression,
					kind,
					shapeKey,
					operations,
					needsIdPrePhase,
					false,
					ordinal,
					origin
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Test Entities
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "Person")
	@Table(name = "integration_person")
	public static class Person {
		@Id
		private Long id;
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Address")
	@Table(name = "integration_address")
	public static class Address {
		@Id
		private Long id;
		private String street;

		@ManyToOne
		@JoinColumn(name = "person_id")
		private Person person;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String street() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

	@Entity(name = "Department")
	@Table(name = "integration_department")
	public static class Department {
		@Id
		private Long id;
		private String name;

		@ManyToOne
		@JoinColumn(name = "head_id", nullable = true)
		private Employee head;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Employee getHead() {
			return head;
		}

		public void setHead(Employee head) {
			this.head = head;
		}
	}

	@Entity(name = "Employee")
	@Table(name = "integration_employee")
	public static class Employee {
		@Id
		private Long id;
		private String name;

		@ManyToOne
		@JoinColumn(name = "department_id", nullable = true)
		private Department department;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Department getDepartment() {
			return department;
		}

		public void setDepartment(Department department) {
			this.department = department;
		}
	}

	/**
	 * Composite key for grouping operations by shape and ordinalBase.
	 */
	private static class OperationGroupKey {
		private final StatementShapeKey shapeKey;
		private final int ordinalBase;

		OperationGroupKey(StatementShapeKey shapeKey, int ordinalBase) {
			this.shapeKey = shapeKey;
			this.ordinalBase = ordinalBase;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof OperationGroupKey)) return false;
			OperationGroupKey that = (OperationGroupKey) o;
			return ordinalBase == that.ordinalBase && shapeKey.equals(that.shapeKey);
		}

		@Override
		public int hashCode() {
			return 31 * shapeKey.hashCode() + ordinalBase;
		}
	}
}
