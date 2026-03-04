/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for delete operations with cascade and orphan removal.
 * Tests cascade delete behavior and orphan removal semantics.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		DeleteCascadeTest.Parent.class,
		DeleteCascadeTest.Child.class,
		DeleteCascadeTest.Order.class,
		DeleteCascadeTest.OrderLine.class,
		DeleteCascadeTest.Department.class,
		DeleteCascadeTest.Employee.class
})
public class DeleteCascadeTest {

	@Test
	public void testDeleteWithCascadeAll(EntityManagerFactoryScope scope) {
		Long parentId = scope.fromTransaction( em -> {
			Parent parent = new Parent();
			parent.name = "Parent";
			parent.children = new ArrayList<>();

			Child child1 = new Child();
			child1.name = "Child1";
			child1.parent = parent;
			parent.children.add( child1 );

			Child child2 = new Child();
			child2.name = "Child2";
			child2.parent = parent;
			parent.children.add( child2 );

			em.persist( parent );
			em.persist( child1 );
			em.persist( child2 );
			em.flush();

			return parent.id;
		} );

		Long child1Id = parentId + 1;
		Long child2Id = parentId + 2;

		// Delete parent - should cascade to children
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );
			em.remove( parent );
			em.flush();
		} );

		// Verify all are deleted
		scope.inTransaction( em -> {
			assertNull( em.find( Parent.class, parentId ),
				"Parent should be deleted" );
			assertNull( em.find( Child.class, child1Id ),
				"Child1 should be cascade deleted" );
			assertNull( em.find( Child.class, child2Id ),
				"Child2 should be cascade deleted" );
		} );
	}

	@Test
	public void testDeleteWithOrphanRemoval(EntityManagerFactoryScope scope) {
		Long orderId = scope.fromTransaction( em -> {
			Order order = new Order();
			order.orderNumber = "ORD-001";
			order.lines = new ArrayList<>();

			OrderLine line1 = new OrderLine();
			line1.product = "Product A";
			line1.quantity = 2;
			line1.order = order;
			order.lines.add( line1 );

			OrderLine line2 = new OrderLine();
			line2.product = "Product B";
			line2.quantity = 3;
			line2.order = order;
			order.lines.add( line2 );

			em.persist( order );
			em.persist( line1 );
			em.persist( line2 );
			em.flush();

			return order.id;
		} );

		// Remove line from collection - should trigger orphan removal
		scope.inTransaction( em -> {
			Order order = em.find( Order.class, orderId );
			assertEquals( 2, order.lines.size() );

			// Remove first line from collection
			OrderLine removedLine = order.lines.remove( 0 );
			Long removedLineId = removedLine.id;

			em.flush();

			// Removed line should be deleted due to orphan removal
			assertNull( em.find( OrderLine.class, removedLineId ),
				"Orphaned line should be deleted" );

			// Other line should still exist
			assertEquals( 1, order.lines.size() );
		} );
	}

	@Test
	public void testDeleteParentWithCascade(EntityManagerFactoryScope scope) {
		Long parentId = scope.fromTransaction( em -> {
			Parent parent = new Parent();
			parent.name = "Parent";
			parent.children = new ArrayList<>();

			for ( int i = 0; i < 5; i++ ) {
				Child child = new Child();
				child.name = "Child" + i;
				child.parent = parent;
				parent.children.add( child );
				em.persist( child );
			}

			em.persist( parent );
			em.flush();

			return parent.id;
		} );

		// Delete parent
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );
			assertEquals( 5, parent.children.size() );

			em.remove( parent );
			em.flush();
		} );

		// Verify parent and all children deleted
		scope.inTransaction( em -> {
			assertNull( em.find( Parent.class, parentId ) );

			// All children should be cascade deleted
			for ( long i = parentId + 1; i <= parentId + 5; i++ ) {
				assertNull( em.find( Child.class, i ),
					"Child with id " + i + " should be cascade deleted" );
			}
		} );
	}

	@Test
	public void testDeleteWithoutCascade(EntityManagerFactoryScope scope) {
		Long deptId = scope.fromTransaction( em -> {
			Department dept = new Department();
			dept.name = "Engineering";
			dept.employees = new ArrayList<>();

			Employee emp1 = new Employee();
			emp1.name = "Alice";
			emp1.department = dept;
			dept.employees.add( emp1 );

			Employee emp2 = new Employee();
			emp2.name = "Bob";
			emp2.department = dept;
			dept.employees.add( emp2 );

			em.persist( dept );
			em.persist( emp1 );
			em.persist( emp2 );
			em.flush();

			return dept.id;
		} );

		// Delete department without cascade - should fail or nullify FK
		scope.inTransaction( em -> {
			Department dept = em.find( Department.class, deptId );

			// Clear employees to avoid FK constraint violation
			for ( Employee emp : dept.employees ) {
				emp.department = null;
			}
			dept.employees.clear();

			em.flush();

			// Now can delete department
			em.remove( dept );
			em.flush();
		} );

		// Verify department deleted but employees still exist
		scope.inTransaction( em -> {
			assertNull( em.find( Department.class, deptId ),
				"Department should be deleted" );

			// Employees should still exist
			List<Employee> employees = em.createQuery(
				"SELECT e FROM Employee e", Employee.class ).getResultList();
			assertEquals( 2, employees.size() );

			// Employees should have null department
			for ( Employee emp : employees ) {
				assertNull( emp.department,
					"Employee department should be null after department delete" );
			}
		} );
	}

	@Test
	public void testMultipleCascadeDeletes(EntityManagerFactoryScope scope) {
		List<Long> parentIds = scope.fromTransaction( em -> {
			List<Long> ids = new ArrayList<>();

			for ( int i = 0; i < 3; i++ ) {
				Parent parent = new Parent();
				parent.name = "Parent" + i;
				parent.children = new ArrayList<>();

				for ( int j = 0; j < 2; j++ ) {
					Child child = new Child();
					child.name = "Child" + i + "_" + j;
					child.parent = parent;
					parent.children.add( child );
					em.persist( child );
				}

				em.persist( parent );
				em.flush();
				ids.add( parent.id );
			}

			return ids;
		} );

		// Delete all parents - should cascade to all children
		scope.inTransaction( em -> {
			for ( Long parentId : parentIds ) {
				Parent parent = em.find( Parent.class, parentId );
				em.remove( parent );
			}
			em.flush();
		} );

		// Verify all deleted
		scope.inTransaction( em -> {
			for ( Long parentId : parentIds ) {
				assertNull( em.find( Parent.class, parentId ) );
			}

			// All children should be deleted
			List<Child> children = em.createQuery(
				"SELECT c FROM Child c", Child.class ).getResultList();
			assertTrue( children.isEmpty(), "All children should be cascade deleted" );
		} );
	}

	@Test
	public void testOrphanRemovalClearsCollection(EntityManagerFactoryScope scope) {
		Long orderId = scope.fromTransaction( em -> {
			Order order = new Order();
			order.orderNumber = "ORD-001";
			order.lines = new ArrayList<>();

			OrderLine line1 = new OrderLine();
			line1.product = "Product A";
			line1.order = order;
			order.lines.add( line1 );

			OrderLine line2 = new OrderLine();
			line2.product = "Product B";
			line2.order = order;
			order.lines.add( line2 );

			em.persist( order );
			em.persist( line1 );
			em.persist( line2 );
			em.flush();

			return order.id;
		} );

		// Clear all lines - should trigger orphan removal for all
		scope.inTransaction( em -> {
			Order order = em.find( Order.class, orderId );
			assertEquals( 2, order.lines.size() );

			order.lines.clear();
			em.flush();
		} );

		// Verify all lines deleted
		scope.inTransaction( em -> {
			Order order = em.find( Order.class, orderId );
			assertTrue( order.lines.isEmpty() );

			List<OrderLine> allLines = em.createQuery(
				"SELECT ol FROM OrderLine ol", OrderLine.class ).getResultList();
			assertTrue( allLines.isEmpty(), "All lines should be orphan removed" );
		} );
	}

	// Test entities

	@Entity(name = "Parent")
	@Table(name = "delete_cascade_parent")
	public static class Parent {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		List<Child> children;
	}

	@Entity(name = "Child")
	@Table(name = "delete_cascade_child")
	public static class Child {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		Parent parent;
	}

	@Entity(name = "Order")
	@Table(name = "delete_cascade_order")
	public static class Order {
		@Id
		@GeneratedValue
		Long id;

		String orderNumber;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
		List<OrderLine> lines;
	}

	@Entity(name = "OrderLine")
	@Table(name = "delete_cascade_order_line")
	public static class OrderLine {
		@Id
		@GeneratedValue
		Long id;

		String product;

		Integer quantity;

		@ManyToOne
		@JoinColumn(name = "order_id")
		Order order;
	}

	@Entity(name = "Department")
	@Table(name = "delete_cascade_department")
	public static class Department {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(mappedBy = "department")
		List<Employee> employees;
	}

	@Entity(name = "Employee")
	@Table(name = "delete_cascade_employee")
	public static class Employee {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "department_id")
		Department department;
	}
}
