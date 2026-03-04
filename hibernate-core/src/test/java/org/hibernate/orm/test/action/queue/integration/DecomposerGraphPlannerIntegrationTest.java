/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import jakarta.persistence.*;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.fk.ForeignKeyModel;
import org.hibernate.action.queue.graph.Graph;
import org.hibernate.action.queue.graph.StandardGraphBuilder;
import org.hibernate.action.queue.plan.FlushPlan;
import org.hibernate.action.queue.plan.PlanStep;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.queue.plan.StandardFlushPlanner;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.DeleteDecomposer;
import org.hibernate.persister.entity.mutation.InsertDecomposer;
import org.hibernate.persister.entity.mutation.UpdateDecomposer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSimpleInsertDecomposition(SessionFactoryScope scope) {
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
			final List<PlannedOperationGroup> groups = decomposer.decompose(action, 0, callback -> {}, sessionImpl);

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
			allGroups.addAll(personDecomposer.decompose(personAction, 0, callback -> {}, sessionImpl));
			allGroups.addAll(addressDecomposer.decompose(addressAction, 1, callback -> {}, sessionImpl));

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(allGroups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
			final List<PlannedOperationGroup> groups = decomposer.decompose(action, 0, callback -> {}, sessionImpl);

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
			final DeleteDecomposer decomposer = new DeleteDecomposer(persister, factory);

			// Decompose
			final List<PlannedOperationGroup> groups = decomposer.decompose(action, 0, callback -> {}, sessionImpl);

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(groups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
			allGroups.addAll(personInsertDecomposer.decompose(person1InsertAction, 0, callback -> {}, sessionImpl));
			allGroups.addAll(addressInsertDecomposer.decompose(addressInsertAction, 1, callback -> {}, sessionImpl));
			allGroups.addAll(personUpdateDecomposer.decompose(person2UpdateAction, 2, callback -> {}, sessionImpl));

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(allGroups);

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
			allGroups.addAll(deptDecomposer.decompose(deptAction, 0, callback -> {}, sessionImpl));
			allGroups.addAll(empDecomposer.decompose(empAction, 1, callback -> {}, sessionImpl));

			final ForeignKeyModel fkModel = factory.getForeignKeyModel();

			// Build graph
			final StandardGraphBuilder graphBuilder = new StandardGraphBuilder(fkModel, true, false);
			final Graph graph = graphBuilder.build(allGroups);

			// Verify there are edges in the graph
			assertFalse(graph.outgoing().isEmpty(), "Graph should have edges for FK relationships");

			// Create plan
			final StandardFlushPlanner planner = new StandardFlushPlanner();
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
}
