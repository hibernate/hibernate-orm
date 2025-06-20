/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				ManyToManyWithDynamicFilterTest.User.class,
				ManyToManyWithDynamicFilterTest.Role.class
		}
)
@SessionFactory
public class ManyToManyWithDynamicFilterTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Role r1 = new Role( 1, "R1", false );
			final Role r2 = new Role( 2, "R2", false );
			session.persist( r1 );
			session.persist( r2 );

			final User user = new User( 1, "A", true, r1, r2 );
			session.persist( user );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-11410")
	void testManyToManyCollectionWithActiveFilterOnJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "activeUserFilter" );
			session.enableFilter( "activeRoleFilter" );

			final User user = session.get( User.class, 1 );
			assertNotNull( user );
			assertTrue( user.getRoles().isEmpty() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11410")
	void testManyToManyCollectionWithNoFilterOnJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final User user = session.get( User.class, 1 );
			assertNotNull( user );
			assertThat( user.getRoles().size(), is( 2 ) );
		} );
	}

	@MappedSuperclass
	public static abstract class AbstractEntity implements Serializable {
		@Id
		private Integer id;

		AbstractEntity() {
		}

		AbstractEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	@FilterDef(name = "activeUserFilter", defaultCondition = "active = true")
	@Filter(name = "activeUserFilter")
	public static class User extends AbstractEntity {
		private String name;
		private Boolean active;

		@ManyToMany
		@Fetch(FetchMode.JOIN)
		@Filter(name = "activeRoleFilter")
		private Set<Role> roles = new HashSet<>();

		public User() {
		}

		public User(Integer id, String name, Boolean active, Role... roles) {
			super( id );
			this.name = name;
			this.active = active;
			this.roles = new HashSet<>( Arrays.asList( roles ) );
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}

		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}
	}

	@Entity(name = "Role")
	@Table(name="Roles")
	@FilterDef(name = "activeRoleFilter", defaultCondition = "active = true")
	@Filter(name = "activeRoleFilter")
	public static class Role extends AbstractEntity {
		private String name;
		private Boolean active;

		Role() {
		}

		public Role(Integer id, String name, Boolean active) {
			super( id );
			this.name = name;
			this.active = active;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}
	}
}
