/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyEmbeddedIdFKTest.System.class,
				OneToManyEmbeddedIdFKTest.SystemUser.class
		}
)
@SessionFactory
public class OneToManyEmbeddedIdFKTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					PK superUserKey = new PK( 1, "Fab" );
					SystemUser superUser = new SystemUser( superUserKey, "Fab" );

					PK userKey = new PK( 2, "Andrea" );
					SystemUser user = new SystemUser( userKey, "Andrea" );

					System system = new System( 1, "sub1" );
					system.addUser( superUser );
					system.addUser( user );

					session.persist( superUser );
					session.persist( user );
					session.persist( system );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = session.get( System.class, 1 );
					assertThat( system, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testHqlQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = (System) session.createQuery( "from System e where e.id = :id" )
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testHqlJoin(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					System system = session.createQuery( "from System e join e.users where e.id = :id", System.class )
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testHqlJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					System system = session.createQuery(
							"from System e join fetch e.users where e.id = :id",
							System.class
					)
							.setParameter( "id", 1 ).uniqueResult();
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);
	}

	@Test
	public void testEmbeddedIdParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PK superUserKey = new PK( 1, "Fab" );

					System system = session.createQuery(
							"from System e join fetch e.users u where u.id = :id",
							System.class
					).setParameter( "id", superUserKey ).uniqueResult();

					assertThat( system, is( notNullValue() ) );
					assertThat( system.getUsers().size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "System")
	@Table( name = "systems" )
	public static class System {
		@Id
		private Integer id;
		private String name;

		@OneToMany
		List<SystemUser> users = new ArrayList<>();

		public System() {
		}

		public System(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<SystemUser> getUsers() {
			return users;
		}

		public void setUsers(List<SystemUser> users) {
			this.users = users;
		}

		public void addUser(SystemUser user) {
			this.users.add( user );
		}
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		private String name;

		public SystemUser() {
		}

		public SystemUser(PK pk, String name) {
			this.pk = pk;
			this.name = name;
		}

		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class PK implements Serializable {

		private Integer subsystem;

		private String username;

		public PK(Integer subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( subsystem, pk.subsystem ) &&
					Objects.equals( username, pk.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( subsystem, username );
		}
	}
}
