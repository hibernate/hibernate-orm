/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.QueueType;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.decompose.entity.DeleteDecomposerStandard;
import org.hibernate.action.queue.decompose.entity.EntityActionDecomposer;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.GraphEdge;
import org.hibernate.action.queue.graph.GraphEdgeKind;
import org.hibernate.action.queue.graph.GroupNode;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.spi.Executable;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.FlushOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.action.queue.decompose.entity.InsertDecomposer;
import org.hibernate.action.queue.decompose.entity.UpdateDecomposer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

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
		DecomposerGraphPlannerIntegrationTest.Employee.class,
		DecomposerGraphPlannerIntegrationTest.SwapDepartment.class,
		DecomposerGraphPlannerIntegrationTest.SwapEmployee.class,
		DecomposerGraphPlannerIntegrationTest.StrictSwapDepartment.class,
		DecomposerGraphPlannerIntegrationTest.StrictSwapEmployee.class,
		DecomposerGraphPlannerIntegrationTest.UniqueProduct.class,
		DecomposerGraphPlannerIntegrationTest.MultiUniqueProduct.class
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
	public void testSimpleInsertDecomposition(DomainModelScope bootModelScope, SessionFactoryScope scope) {
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
			final List<FlushOperation> operations = new ArrayList<>();
			decomposer.decompose(action, 0, sessionImpl, null, operations::add);
			final List<FlushOperationGroup> groups = groupOperations(operations);

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(groups, DeferrableConstraintMode.DEFAULT);

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
			final List<FlushOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(decomposeAndGroup(personDecomposer, personAction, 0, sessionImpl));
			allGroups.addAll(decomposeAndGroup(addressDecomposer, addressAction, 1, sessionImpl));

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(allGroups, DeferrableConstraintMode.DEFAULT);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner( planningOptions );
			final FlushPlan plan = planner.plan(graph);

			// Verify
			assertNotNull(plan);
			assertFalse(plan.steps().isEmpty(), "Plan should have steps");

			// Verify FK ordering: Person must come before Address
			final List<FlushOperation> allOps = new ArrayList<>();
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
			final List<FlushOperation> operations = new ArrayList<>();
			decomposer.decompose(action, 0, sessionImpl, null, operations::add);
			final List<FlushOperationGroup> groups = groupOperations(operations);

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(groups, DeferrableConstraintMode.DEFAULT);

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
			final List<FlushOperation> operations = new ArrayList<>();
			decomposer.decompose(action, 0, sessionImpl, null, operations::add);
			final List<FlushOperationGroup> groups = groupOperations(operations);

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(groups, DeferrableConstraintMode.DEFAULT);

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
			final List<FlushOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(decomposeAndGroup(personInsertDecomposer, person1InsertAction, 0, sessionImpl));
			allGroups.addAll(decomposeAndGroup(addressInsertDecomposer, addressInsertAction, 1, sessionImpl));
			allGroups.addAll(decomposeAndGroup(personUpdateDecomposer, person2UpdateAction, 2, sessionImpl));

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(allGroups, DeferrableConstraintMode.DEFAULT);

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
			final List<FlushOperationGroup> allGroups = new ArrayList<>();
			allGroups.addAll(decomposeAndGroup(deptDecomposer, deptAction, 0, sessionImpl));
			allGroups.addAll(decomposeAndGroup(empDecomposer, empAction, 1, sessionImpl));

			var constraintModel = scope.getSessionFactory().getMappingMetamodel().getConstraintModel();

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
			final Graph graph = graphBuilder.build(allGroups, DeferrableConstraintMode.DEFAULT);

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
				for (FlushOperation op : step.operations()) {
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

	@Test
	public void testNullableUniqueSwapEmitsExplicitNullPatchableUniqueEdges(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( this::persistNullableSwapFixture );

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;

			final SwapEmployee emp1 = session.find(SwapEmployee.class, 1L);
			final SwapEmployee emp2 = session.find(SwapEmployee.class, 2L);
			final SwapDepartment dept1 = session.find(SwapDepartment.class, 1L);
			final SwapDepartment dept2 = session.find(SwapDepartment.class, 2L);

			final Graph graph = buildUniqueSwapGraph(
					scope,
					sessionImpl,
					SwapEmployee.class,
					emp1,
					emp2,
					dept1,
					dept2,
					(employee, department) -> ((SwapEmployee) employee).setDepartment( (SwapDepartment) department )
			);

			assertUniqueSwapEdgeKinds(graph, true);
		});
	}

	@Test
	public void testNonNullableUniqueSwapEmitsRequiredUniqueEdges(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( this::persistStrictSwapFixture );

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;

			final StrictSwapEmployee emp1 = session.find(StrictSwapEmployee.class, 1L);
			final StrictSwapEmployee emp2 = session.find(StrictSwapEmployee.class, 2L);
			final StrictSwapDepartment dept1 = session.find(StrictSwapDepartment.class, 1L);
			final StrictSwapDepartment dept2 = session.find(StrictSwapDepartment.class, 2L);

			final Graph graph = buildUniqueSwapGraph(
					scope,
					sessionImpl,
					StrictSwapEmployee.class,
					emp1,
					emp2,
					dept1,
					dept2,
					(employee, department) -> ((StrictSwapEmployee) employee).setDepartment( (StrictSwapDepartment) department )
			);

			assertUniqueSwapEdgeKinds(graph, false);

			// This test only inspects graph construction. Restore the managed entities
			// before commit so the impossible non-nullable unique swap is not flushed.
			emp1.setDepartment(dept1);
			emp2.setDepartment(dept2);
		});
	}

	@Test
	public void testUpdateReleaseOrdersBeforeInsertOccupyingSameUniqueSlot(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final UniqueProduct product = new UniqueProduct();
			product.setId(1L);
			product.setSku("SKU-001");
			product.setName("Original Product");
			session.persist(product);
		});

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();
			final EntityPersister productPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(UniqueProduct.class);

			final UniqueProduct existingProduct = session.find(UniqueProduct.class, 1L);
			final Object[] previousState = productPersister.getValues(existingProduct);
			existingProduct.setSku("SKU-002");
			final Object[] currentState = productPersister.getValues(existingProduct);

			final EntityUpdateAction updateAction = new EntityUpdateAction(
					existingProduct.getId(),
					currentState,
					productPersister.findDirty(currentState, previousState, existingProduct, sessionImpl),
					false,
					previousState,
					null,
					null,
					existingProduct,
					null,
					productPersister,
					(EventSource) sessionImpl
			);

			final UniqueProduct replacementProduct = new UniqueProduct();
			replacementProduct.setId(2L);
			replacementProduct.setSku("SKU-001");
			replacementProduct.setName("Replacement Product");
			final EntityInsertAction insertAction = new EntityInsertAction(
					replacementProduct.getId(),
					productPersister.getValues(replacementProduct),
					replacementProduct,
					null,
					productPersister,
					false,
					(EventSource) sessionImpl
			);

			final UpdateDecomposer updateDecomposer = new UpdateDecomposer(productPersister, factory);
			final InsertDecomposer insertDecomposer = new InsertDecomposer(productPersister, factory);
			final List<FlushOperationGroup> groups = new ArrayList<>();
			groups.addAll(decomposeAndGroup(updateDecomposer, updateAction, 0, sessionImpl));
			groups.addAll(decomposeAndGroup(insertDecomposer, insertAction, 1, sessionImpl));

			final Graph graph = buildGraph(scope, sessionImpl, groups);

			assertUpdateReleaseToInsertOccupyEdge(graph);

			// The graph assertion is synthetic; leave the managed row unchanged at commit.
			existingProduct.setSku("SKU-001");
		});
	}

	@Test
	public void testUpdateReleasePairsMultipleUniqueSlotsByConstraint(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final MultiUniqueProduct product = new MultiUniqueProduct();
			product.setId(1L);
			product.setSku("SKU-001");
			product.setCatalogCode("CAT-001");
			session.persist(product);
		});

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();
			final EntityPersister productPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(MultiUniqueProduct.class);

			final MultiUniqueProduct existingProduct = session.find(MultiUniqueProduct.class, 1L);
			final Object[] previousState = productPersister.getValues(existingProduct);
			existingProduct.setCatalogCode("CAT-002");
			final Object[] currentState = productPersister.getValues(existingProduct);

			final EntityUpdateAction updateAction = new EntityUpdateAction(
					existingProduct.getId(),
					currentState,
					productPersister.findDirty(currentState, previousState, existingProduct, sessionImpl),
					false,
					previousState,
					null,
					null,
					existingProduct,
					null,
					productPersister,
					(EventSource) sessionImpl
			);

			final MultiUniqueProduct replacementProduct = new MultiUniqueProduct();
			replacementProduct.setId(2L);
			replacementProduct.setSku("SKU-002");
			replacementProduct.setCatalogCode("CAT-001");
			final EntityInsertAction insertAction = new EntityInsertAction(
					replacementProduct.getId(),
					productPersister.getValues(replacementProduct),
					replacementProduct,
					null,
					productPersister,
					false,
					(EventSource) sessionImpl
			);

			final UpdateDecomposer updateDecomposer = new UpdateDecomposer(productPersister, factory);
			final InsertDecomposer insertDecomposer = new InsertDecomposer(productPersister, factory);
			final List<FlushOperationGroup> groups = new ArrayList<>();
			groups.addAll(decomposeAndGroup(updateDecomposer, updateAction, 0, sessionImpl));
			groups.addAll(decomposeAndGroup(insertDecomposer, insertAction, 1, sessionImpl));

			final Graph graph = buildGraph(scope, sessionImpl, groups);

			assertUpdateReleaseToInsertOccupyEdge(graph);

			// The graph assertion is synthetic; leave the managed row unchanged at commit.
			existingProduct.setCatalogCode("CAT-001");
		});
	}

	@Test
	public void testMultiUniqueUpdateSwapSplitsGroupedUpdatesByConstraint(SessionFactoryScope scope) {
		var sfi = scope.getSessionFactory();
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction(session -> {
			final MultiUniqueProduct product1 = new MultiUniqueProduct();
			product1.setId(1L);
			product1.setSku("SKU-001");
			product1.setCatalogCode("CAT-001");
			session.persist(product1);

			final MultiUniqueProduct product2 = new MultiUniqueProduct();
			product2.setId(2L);
			product2.setSku("SKU-002");
			product2.setCatalogCode("CAT-002");
			session.persist(product2);
		});

		scope.inTransaction(session -> {
			final SessionImplementor sessionImpl = session;
			final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();
			final EntityPersister productPersister = factory.getMappingMetamodel()
					.getEntityDescriptor(MultiUniqueProduct.class);

			final MultiUniqueProduct product1 = session.find(MultiUniqueProduct.class, 1L);
			final MultiUniqueProduct product2 = session.find(MultiUniqueProduct.class, 2L);

			final EntityUpdateAction product1Action = createMultiUniqueCatalogUpdateAction(
					sessionImpl,
					productPersister,
					product1,
					"CAT-002"
			);
			final EntityUpdateAction product2Action = createMultiUniqueCatalogUpdateAction(
					sessionImpl,
					productPersister,
					product2,
					"CAT-001"
			);

			final UpdateDecomposer updateDecomposer = new UpdateDecomposer(productPersister, factory);
			final List<FlushOperationGroup> groups = new ArrayList<>();
			groups.addAll(decomposeAndGroup(updateDecomposer, product1Action, 0, sessionImpl));
			groups.addAll(decomposeAndGroup(updateDecomposer, product2Action, 1, sessionImpl));

			final Graph graph = buildGraph(scope, sessionImpl, groups);

			int updateNodeCount = 0;
			boolean foundUniqueUpdateEdge = false;
			for ( GroupNode node : graph.nodes() ) {
				if ( node.group().kind() == MutationKind.UPDATE ) {
					updateNodeCount++;
					for ( GraphEdge edge : graph.outgoing().get(node) ) {
						if ( edge.getTo().group().kind() == MutationKind.UPDATE
								&& edge.isUniqueConstraintEdge() ) {
							foundUniqueUpdateEdge = true;
						}
					}
				}
			}

			assertEquals(
					2,
					updateNodeCount,
					"Grouped UPDATEs with a row-to-row unique-slot cycle should be split into per-row graph nodes"
			);
			assertTrue(
					foundUniqueUpdateEdge,
					"Split UPDATE nodes should expose the unique-slot cycle as graph edges"
			);

			// The graph assertion is synthetic; leave the managed rows unchanged at commit.
			product1.setCatalogCode("CAT-001");
			product2.setCatalogCode("CAT-002");
		});
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Helper methods
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void persistNullableSwapFixture(SessionImplementor session) {
		final SwapDepartment dept1 = new SwapDepartment();
		dept1.setId(1L);
		dept1.setName("Sales");

		final SwapDepartment dept2 = new SwapDepartment();
		dept2.setId(2L);
		dept2.setName("Engineering");

		final SwapEmployee emp1 = new SwapEmployee();
		emp1.setId(1L);
		emp1.setName("Alice");
		emp1.setDepartment(dept1);

		final SwapEmployee emp2 = new SwapEmployee();
		emp2.setId(2L);
		emp2.setName("Bob");
		emp2.setDepartment(dept2);

		session.persist(dept1);
		session.persist(dept2);
		session.persist(emp1);
		session.persist(emp2);
	}

	private void persistStrictSwapFixture(SessionImplementor session) {
		final StrictSwapDepartment dept1 = new StrictSwapDepartment();
		dept1.setId(1L);
		dept1.setName("Sales");

		final StrictSwapDepartment dept2 = new StrictSwapDepartment();
		dept2.setId(2L);
		dept2.setName("Engineering");

		final StrictSwapEmployee emp1 = new StrictSwapEmployee();
		emp1.setId(1L);
		emp1.setName("Alice");
		emp1.setDepartment(dept1);

		final StrictSwapEmployee emp2 = new StrictSwapEmployee();
		emp2.setId(2L);
		emp2.setName("Bob");
		emp2.setDepartment(dept2);

		session.persist(dept1);
		session.persist(dept2);
		session.persist(emp1);
		session.persist(emp2);
	}

	private Graph buildUniqueSwapGraph(
			SessionFactoryScope scope,
			SessionImplementor sessionImpl,
			Class<?> employeeClass,
			Object emp1,
			Object emp2,
			Object dept1,
			Object dept2,
			BiConsumer<Object, Object> departmentSetter) {
		final SessionFactoryImplementor factory = sessionImpl.getSessionFactory();
		final EntityPersister employeePersister = factory.getMappingMetamodel()
				.getEntityDescriptor(employeeClass);
		final UpdateDecomposer updateDecomposer = new UpdateDecomposer(employeePersister, factory);

		final EntityUpdateAction emp1Action = createSwapUpdateAction(
				sessionImpl,
				employeePersister,
				emp1,
				dept2,
				departmentSetter
		);
		final EntityUpdateAction emp2Action = createSwapUpdateAction(
				sessionImpl,
				employeePersister,
				emp2,
				dept1,
				departmentSetter
		);

		final List<FlushOperationGroup> groups = new ArrayList<>();
		groups.addAll(decomposeAndGroup(updateDecomposer, emp1Action, 0, sessionImpl));
		groups.addAll(decomposeAndGroup(updateDecomposer, emp2Action, 1, sessionImpl));

		return buildGraph(scope, sessionImpl, groups);
	}

	private Graph buildGraph(
			SessionFactoryScope scope,
			SessionImplementor sessionImpl,
			List<FlushOperationGroup> groups) {
		final PlanningOptions planningOptions = new PlanningOptions(
				true,
				true,
				false,
				true,
				PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
		);
		final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(
				scope.getSessionFactory().getMappingMetamodel().getConstraintModel(),
				planningOptions,
				sessionImpl
		);
		return graphBuilder.build(groups, DeferrableConstraintMode.DEFAULT);
	}

	private EntityUpdateAction createSwapUpdateAction(
			SessionImplementor sessionImpl,
			EntityPersister employeePersister,
			Object employee,
			Object newDepartment,
			BiConsumer<Object, Object> departmentSetter) {
		final Object[] previousState = employeePersister.getValues(employee);
		departmentSetter.accept(employee, newDepartment);
		final Object[] currentState = employeePersister.getValues(employee);

		return new EntityUpdateAction(
				employeePersister.getIdentifier(employee, sessionImpl),
				currentState,
				employeePersister.findDirty(currentState, previousState, employee, sessionImpl),
				false,
				previousState,
				null,
				null,
				employee,
				null,
				employeePersister,
				(EventSource) sessionImpl
		);
	}

	private EntityUpdateAction createMultiUniqueCatalogUpdateAction(
			SessionImplementor sessionImpl,
			EntityPersister productPersister,
			MultiUniqueProduct product,
			String newCatalogCode) {
		final Object[] previousState = productPersister.getValues(product);
		product.setCatalogCode(newCatalogCode);
		final Object[] currentState = productPersister.getValues(product);

		return new EntityUpdateAction(
				product.getId(),
				currentState,
				productPersister.findDirty(currentState, previousState, product, sessionImpl),
				false,
				previousState,
				null,
				null,
				product,
				null,
				productPersister,
				(EventSource) sessionImpl
		);
	}

	private void assertUpdateReleaseToInsertOccupyEdge(Graph graph) {
		GroupNode updateNode = null;
		GroupNode insertNode = null;
		for ( GroupNode node : graph.nodes() ) {
			if ( node.group().kind() == MutationKind.UPDATE ) {
				updateNode = node;
			}
			else if ( node.group().kind() == MutationKind.INSERT ) {
				insertNode = node;
			}
		}

		assertNotNull(updateNode, "Graph should contain the UPDATE group");
		assertNotNull(insertNode, "Graph should contain the INSERT group");

		boolean foundReleaseToOccupyEdge = false;
		for ( GraphEdge edge : graph.outgoing().get(updateNode) ) {
			if ( edge.getTo() == insertNode && edge.isUniqueConstraintEdge() ) {
				foundReleaseToOccupyEdge = true;
				assertEquals(
						GraphEdgeKind.PREFERRED_ORDER,
						edge.getKind(),
						"UPDATE release to INSERT occupy edges should be preferred unique ordering edges"
				);
			}
		}
		assertTrue(
				foundReleaseToOccupyEdge,
				"UPDATE releasing a unique slot should order before INSERT occupying that same slot"
		);
	}

	private void assertUniqueSwapEdgeKinds(Graph graph, boolean nullableUnique) {
		boolean foundRequiredUnique = false;
		boolean foundNullPatchableUnique = false;
		boolean foundRequiredUniquePatch = false;
		for ( List<GraphEdge> edges : graph.outgoing().values() ) {
			for ( GraphEdge edge : edges ) {
				if ( edge.isRequiredOrder() && edge.isUniqueConstraintEdge() ) {
					foundRequiredUnique = true;
					if ( edge.getPatchCycleType() != null ) {
						foundRequiredUniquePatch = true;
					}
				}
				if ( edge.getKind() == GraphEdgeKind.NULL_PATCHABLE_UNIQUE ) {
					foundNullPatchableUnique = true;
				}
			}
		}

		assertEquals(
				nullableUnique,
				foundNullPatchableUnique,
				"Only nullable unique swaps should be represented as NULL_PATCHABLE_UNIQUE edges"
		);
		assertEquals(
				!nullableUnique,
				foundRequiredUnique,
				"Non-nullable unique swaps should be represented as required unique edges"
		);
		assertFalse(
				foundRequiredUniquePatch,
				"Required unique edges should not advertise a patch type"
		);
	}

	/**
	 * Helper method to group operations by shape (table + kind + SQL).
	 * Mirrors FlushCoordinator's grouping logic.
	 */
	private <A extends Executable> List<FlushOperationGroup> decomposeAndGroup(
			EntityActionDecomposer<A> decomposer,
			A action,
			int ordinalBase,
			SessionImplementor session) {
		final List<FlushOperation> operations = new ArrayList<>();
		decomposer.decompose(action, ordinalBase, session, null, operations::add);
		return groupOperations(operations);
	}

	private List<FlushOperationGroup> groupOperations(List<FlushOperation> operations) {
		if (operations.isEmpty()) {
			return List.of();
		}

		// Group by shapeKey only (merge operations from different entities)
		// This mirrors FlushCoordinator behavior for non-self-referential tables
		final java.util.Map<StatementShapeKey, OperationGroupBuilder> builders = new java.util.LinkedHashMap<>();

		for (FlushOperation operation : operations) {
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

		final List<FlushOperationGroup> groups = new ArrayList<>(builders.size());
		for (OperationGroupBuilder builder : builders.values()) {
			groups.add(builder.build());
		}

		return groups;
	}

	private StatementShapeKey computeShapeKey(FlushOperation operation) {
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
		private final List<FlushOperation> operations = new ArrayList<>();

		OperationGroupBuilder(FlushOperation firstOperation, StatementShapeKey shapeKey) {
			this.tableExpression = firstOperation.getTableExpression();
			this.kind = firstOperation.getKind();
			this.shapeKey = shapeKey;
			this.ordinal = firstOperation.getOrdinal();
			this.origin = firstOperation.getOrigin();
			this.operations.add(firstOperation);
		}

		void addOperation(FlushOperation op) {
			this.operations.add(op);
			// Track minimum ordinal when merging operations
			this.ordinal = Math.min(this.ordinal, op.getOrdinal());
		}

		FlushOperationGroup build() {
			final boolean needsIdPrePhase = false;
			return new FlushOperationGroup(
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

	@Entity(name = "SwapDepartment")
	@Table(name = "integration_swap_department")
	public static class SwapDepartment {
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

	@Entity(name = "SwapEmployee")
	@Table(name = "integration_swap_employee")
	public static class SwapEmployee {
		@Id
		private Long id;
		private String name;

		@OneToOne
		@JoinColumn(name = "department_id", unique = true, nullable = true)
		private SwapDepartment department;

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

		public SwapDepartment getDepartment() {
			return department;
		}

		public void setDepartment(SwapDepartment department) {
			this.department = department;
		}
	}

	@Entity(name = "StrictSwapDepartment")
	@Table(name = "integration_strict_swap_department")
	public static class StrictSwapDepartment {
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

	@Entity(name = "StrictSwapEmployee")
	@Table(name = "integration_strict_swap_employee")
	public static class StrictSwapEmployee {
		@Id
		private Long id;
		private String name;

		@OneToOne
		@JoinColumn(name = "department_id", unique = true, nullable = false)
		private StrictSwapDepartment department;

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

		public StrictSwapDepartment getDepartment() {
			return department;
		}

		public void setDepartment(StrictSwapDepartment department) {
			this.department = department;
		}
	}

	@Entity(name = "UniqueProduct")
	@Table(name = "integration_unique_product")
	public static class UniqueProduct {
		@Id
		private Long id;

		@Column(unique = true, nullable = false)
		private String sku;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "MultiUniqueProduct")
	@Table(name = "integration_multi_unique_product")
	public static class MultiUniqueProduct {
		@Id
		private Long id;

		@Column(unique = true, nullable = false)
		private String sku;

		@Column(unique = true, nullable = false)
		private String catalogCode;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getSku() {
			return sku;
		}

		public void setSku(String sku) {
			this.sku = sku;
		}

		public String getCatalogCode() {
			return catalogCode;
		}

		public void setCatalogCode(String catalogCode) {
			this.catalogCode = catalogCode;
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
