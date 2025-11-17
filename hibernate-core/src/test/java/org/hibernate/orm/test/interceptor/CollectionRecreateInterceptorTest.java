/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				CollectionRecreateInterceptorTest.Employee.class,
				CollectionRecreateInterceptorTest.Project.class
		}
)
@SessionFactory
public class CollectionRecreateInterceptorTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee employee = new Employee( 1L );
					Project project = new Project( 1L );

					employee.projects.add( project );
					project.employees.add( employee );

					session.persist( project );
					session.persist( employee );
				}
		);
	}

	@Test
	@JiraKey("HHH-3129")
	public void testInterceptorNpe(SessionFactoryScope scope) {
		scope.inTransaction(
				(SessionImplementor) scope.getSessionFactory().withOptions()
						.interceptor( new Interceptor() {

							@Override
							public void onCollectionRecreate(Object collection, Object key) throws CallbackException {
								Interceptor.super.onCollectionRecreate( collection, key );
								assertNotNull( ((PersistentCollection<?>) collection).getRole() );
							}

							@Override
							public void onCollectionUpdate(Object collection, Object key) throws CallbackException {
								Interceptor.super.onCollectionUpdate( collection, key );
								assertNotNull( ((PersistentCollection<?>) collection).getRole() );
							}
						} )
						.openSession(),
				session -> {
					Employee employee = session.find( Employee.class, 1L );
					Project newProject = new Project( 2L );

					newProject.employees.add( employee );
					employee.projects.add( newProject );

					session.persist( newProject );
				}
		);
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		Long id;
		String name;

		@ManyToMany(mappedBy = "employees")
		Set<Project> projects = new HashSet<>();

		public Employee() {
		}

		public Employee(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Project")
	public static class Project {
		@Id
		Long id;
		String name;

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(
				name = "employees_to_projects",
				joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"),
				inverseJoinColumns = @JoinColumn(name = "employee_id")
		)
		List<Employee> employees = new ArrayList<>();

		public Project() {
		}

		public Project(Long id) {
			this.id = id;
		}
	}
}
