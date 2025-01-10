/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.lazyonetoone;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
	annotatedClasses = {
		LazyOneToOneWithEntityGraphTest.Company.class,
		LazyOneToOneWithEntityGraphTest.Employee.class,
		LazyOneToOneWithEntityGraphTest.Project.class
	}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class LazyOneToOneWithEntityGraphTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			// Create company
			Company company = new Company();
			company.id = 1L;
			company.name = "Hibernate";
			session.persist(company);

			// Create project
			Project project = new Project();
			project.id = 1L;
			session.persist(project);

			// Create employee
			Employee employee = new Employee();
			employee.id = 1L;
			employee.company = company;
			employee.projects = List.of(project);
			session.persist(employee);
		});
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		});
	}


	@Test
	void reproducerTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
		// Load employee using entity graph
		Employee employee = session.createQuery(
				"select e from Employee e where e.id = :id", Employee.class)
			.setParameter("id", 1L)
			.setHint("javax.persistence.fetchgraph", session.getEntityGraph("employee.projects"))
			.getSingleResult();

		assertTrue(isInitialized(employee.projects));
		assertEquals("Hibernate", employee.company.name);
		});
	}

	@Entity(name = "Company")
	public static class Company {
		@Id
		private Long id;

		private String name;
	}

	@Entity(name = "Employee")
	@NamedEntityGraph(
		name = "employee.projects",
		attributeNodes = @NamedAttributeNode("projects")
	)
	public static class Employee {
		@Id
		private Long id;

		@OneToOne
		@JoinColumn(name = "company_name", referencedColumnName = "name")
		private Company company;

		@OneToMany(fetch = FetchType.LAZY)
		private List<Project> projects;
	}

	@Entity(name = "Project")
	public static class Project {
		@Id
		private Long id;
	}
}
