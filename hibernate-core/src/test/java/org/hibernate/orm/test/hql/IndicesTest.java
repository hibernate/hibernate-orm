/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import static org.hamcrest.core.Is.is;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;

/**
 * @author Burkhard Graves
 */
@JiraKey(value = "HHH-14475")
public class IndicesTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {Project.class, Role.class, Person.class};
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {

			Project project = new Project(1);
			Role role = new Role(1);

			session.persist( project );
			session.persist( role );

			Person person = new Person(1, project, role);

			session.persist( person );
		});
	}

	@Test
	public void testSelectIndices() {
		doInHibernate( this::sessionFactory, session -> {

			List<Object> result = session.createQuery(
					"select indices(p.roles) from Person p"
			).list();

			assertThat( result.size(), is( 1 ) );
		});
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
