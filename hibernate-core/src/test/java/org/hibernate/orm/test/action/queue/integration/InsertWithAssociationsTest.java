/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.action.queue.integration;

import org.hibernate.action.queue.QueueType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for insert operations with associations.
 * Tests dependency ordering and FK constraint handling.
 *
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		InsertWithAssociationsTest.Company.class,
		InsertWithAssociationsTest.Department.class,
		InsertWithAssociationsTest.Employee.class,
		InsertWithAssociationsTest.Project.class,
		InsertWithAssociationsTest.Task.class
})
public class InsertWithAssociationsTest {

	@Test
	public void testInsertWithManyToOne(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			Company company = new Company();
			company.name = "ACME Corp";
			em.persist( company );

			Department dept = new Department();
			dept.name = "Engineering";
			dept.company = company;
			em.persist( dept );

			em.flush();

			assertNotNull( company.id );
			assertNotNull( dept.id );
			assertEquals( company.id, dept.company.id );
		} );
	}

	@Test
	public void testInsertWithOneToMany(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			Company company = new Company();
			company.name = "ACME Corp";
			company.departments = new ArrayList<>();

			Department dept1 = new Department();
			dept1.name = "Engineering";
			dept1.company = company;
			company.departments.add( dept1 );

			Department dept2 = new Department();
			dept2.name = "Sales";
			dept2.company = company;
			company.departments.add( dept2 );

			em.persist( company );
			em.persist( dept1 );
			em.persist( dept2 );

			em.flush();

			assertNotNull( company.id );
			assertNotNull( dept1.id );
			assertNotNull( dept2.id );
			assertEquals( 2, company.departments.size() );
		} );
	}

	@Test
	public void testInsertWithBidirectional(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
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

			// Verify bidirectional relationship
			assertNotNull( dept.id );
			assertNotNull( emp1.id );
			assertNotNull( emp2.id );
			assertEquals( dept.id, emp1.department.id );
			assertEquals( dept.id, emp2.department.id );
			assertEquals( 2, dept.employees.size() );
		} );
	}

	@Test
	public void testInsertMultipleLevels(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Company → Department → Employee (3 levels)
			Company company = new Company();
			company.name = "ACME Corp";

			Department dept = new Department();
			dept.name = "Engineering";
			dept.company = company;

			Employee emp = new Employee();
			emp.name = "Alice";
			emp.department = dept;

			em.persist( company );
			em.persist( dept );
			em.persist( emp );

			em.flush();

			assertNotNull( company.id );
			assertNotNull( dept.id );
			assertNotNull( emp.id );

			// Verify FKs
			assertEquals( company.id, dept.company.id );
			assertEquals( dept.id, emp.department.id );
		} );
	}

	@Test
	public void testInsertInCorrectOrder(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Insert in correct order: Company → Department → Employee
			Company company = new Company();
			company.name = "ACME Corp";
			em.persist( company );
			em.flush();

			Department dept = new Department();
			dept.name = "Engineering";
			dept.company = company;
			em.persist( dept );
			em.flush();

			Employee emp = new Employee();
			emp.name = "Alice";
			emp.department = dept;
			em.persist( emp );
			em.flush();

			assertNotNull( company.id );
			assertNotNull( dept.id );
			assertNotNull( emp.id );
		} );
	}

	@Test
	public void testInsertInReverseOrder(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Insert in reverse order: Employee → Department → Company
			// Graph decomposer should handle correct ordering

			Company company = new Company();
			company.name = "ACME Corp";

			Department dept = new Department();
			dept.name = "Engineering";
			dept.company = company;

			Employee emp = new Employee();
			emp.name = "Alice";
			emp.department = dept;

			// Persist in reverse order
			em.persist( emp );
			em.persist( dept );
			em.persist( company );

			em.flush();

			// All should be inserted with correct FKs
			assertNotNull( company.id );
			assertNotNull( dept.id );
			assertNotNull( emp.id );
			assertEquals( company.id, dept.company.id );
			assertEquals( dept.id, emp.department.id );
		} );
	}

	@Test
	public void testInsertWithNullableFK(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Employee with no department (nullable FK)
			Employee emp = new Employee();
			emp.name = "Contractor";
			emp.department = null;

			em.persist( emp );
			em.flush();

			assertNotNull( emp.id );
			assertNull( emp.department );
		} );
	}

	@Test
	public void testInsertComplexGraph(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Complex graph: Project has multiple Tasks, each Task assigned to Employee
			Project project = new Project();
			project.name = "Website Redesign";
			project.tasks = new ArrayList<>();

			Department dept = new Department();
			dept.name = "Engineering";

			Employee emp1 = new Employee();
			emp1.name = "Alice";
			emp1.department = dept;

			Employee emp2 = new Employee();
			emp2.name = "Bob";
			emp2.department = dept;

			Task task1 = new Task();
			task1.name = "Design";
			task1.project = project;
			task1.assignee = emp1;
			project.tasks.add( task1 );

			Task task2 = new Task();
			task2.name = "Implementation";
			task2.project = project;
			task2.assignee = emp2;
			project.tasks.add( task2 );

			// Persist all (order shouldn't matter)
			em.persist( task1 );
			em.persist( task2 );
			em.persist( project );
			em.persist( emp1 );
			em.persist( emp2 );
			em.persist( dept );

			em.flush();

			// Verify all relationships
			assertNotNull( project.id );
			assertNotNull( dept.id );
			assertNotNull( emp1.id );
			assertNotNull( emp2.id );
			assertNotNull( task1.id );
			assertNotNull( task2.id );

			assertEquals( 2, project.tasks.size() );
			assertEquals( project.id, task1.project.id );
			assertEquals( project.id, task2.project.id );
			assertEquals( emp1.id, task1.assignee.id );
			assertEquals( emp2.id, task2.assignee.id );
		} );
	}

	@Test
	public void testInsertDiamondDependency(EntityManagerFactoryScope scope) {
		var sfi = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class );
		if ( sfi.getActionQueueFactory().getConfiguredQueueType() != QueueType.GRAPH ) {
			Assumptions.abort("Skipping GRAPH test with non-GRAPH queue type");
		}

		scope.inTransaction( em -> {
			// Diamond: Company → Dept1 → Employee, Company → Dept2 → Employee
			Company company = new Company();
			company.name = "ACME Corp";

			Department dept1 = new Department();
			dept1.name = "Engineering";
			dept1.company = company;

			Department dept2 = new Department();
			dept2.name = "Sales";
			dept2.company = company;

			Employee emp = new Employee();
			emp.name = "Alice";
			emp.department = dept1; // Could also reference dept2

			em.persist( emp );
			em.persist( dept1 );
			em.persist( dept2 );
			em.persist( company );

			em.flush();

			assertNotNull( company.id );
			assertNotNull( dept1.id );
			assertNotNull( dept2.id );
			assertNotNull( emp.id );
		} );
	}

	// Test entities

	@Entity(name = "Company")
	@Table(name = "insert_assoc_company")
	public static class Company {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(mappedBy = "company")
		List<Department> departments;
	}

	@Entity(name = "Department")
	@Table(name = "insert_assoc_department")
	public static class Department {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "company_id")
		Company company;

		@OneToMany(mappedBy = "department")
		List<Employee> employees;
	}

	@Entity(name = "Employee")
	@Table(name = "insert_assoc_employee")
	public static class Employee {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "department_id")
		Department department;
	}

	@Entity(name = "Project")
	@Table(name = "insert_assoc_project")
	public static class Project {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@OneToMany(mappedBy = "project")
		List<Task> tasks;
	}

	@Entity(name = "Task")
	@Table(name = "insert_assoc_task")
	public static class Task {
		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		@JoinColumn(name = "project_id")
		Project project;

		@ManyToOne
		@JoinColumn(name = "assignee_id")
		Employee assignee;
	}
}
