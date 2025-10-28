/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Burkhard Graves
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-14475")
@DomainModel(annotatedClasses = {IndicesTest.Project.class, IndicesTest.Role.class, IndicesTest.Person.class})
@SessionFactory
public class IndicesTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Project project = new Project(1);
			Role role = new Role(1);

			session.persist( project );
			session.persist( role );

			Person person = new Person(1, project, role);

			session.persist( person );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	public void testSelectIndices(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			List<?> result = session.createQuery("select indices(p.roles) from Person p" ).list();
			assertThat( result ).hasSize( 1 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		@OneToMany
		@JoinTable(name = "person_to_role",
				joinColumns = @JoinColumn(name = "person_id"),
				inverseJoinColumns = @JoinColumn(name = "role_id")
		)
		@MapKeyJoinColumn(name = "project_id")
		private Map<Project, Role> roles;

		public Person() {
		}

		public Person(Integer id, Project project, Role role) {
			this.id = id;
			roles = new HashMap<>();
			roles.put(project, role);
		}
	}

	@Entity(name = "Project")
	public static class Project {

		@Id
		private Integer id;

		public Project() {
		}

		public Project(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Role")
	@Table(name = "proj_role")
	public static class Role {

		@Id
		private Integer id;

		public Role() {
		}

		public Role(Integer id) {
			this.id = id;
		}
	}
}
