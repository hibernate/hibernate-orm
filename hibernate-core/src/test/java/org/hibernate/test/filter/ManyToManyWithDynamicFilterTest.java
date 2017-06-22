/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
public class ManyToManyWithDynamicFilterTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Role.class };
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			final Role r1 = new Role( 1, "R1", false );
			final Role r2 = new Role( 2, "R2", false );
			session.save( r1 );
			session.save( r2 );

			final User user = new User( 1, "A", true, r1, r2 );
			session.save( user );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "DELETE FROM User" ).executeUpdate();
			session.createQuery( "DELETE FROM Role" ).executeUpdate();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11410")
	public void testManyToManyCollectionWithActiveFilterOnJoin() {
		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "activeUserFilter" );
			session.enableFilter( "activeRoleFilter" );

			final User user = session.get( User.class, 1 );
			assertNotNull( user );
			assertTrue( user.getRoles().isEmpty() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11410")
	public void testManyToManyCollectionWithNoFilterOnJoin() {
		doInHibernate( this::sessionFactory, session -> {
			final User user = session.get( User.class, 1 );
			assertNotNull( user );
			assertEquals( 2, user.getRoles().size() );
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
