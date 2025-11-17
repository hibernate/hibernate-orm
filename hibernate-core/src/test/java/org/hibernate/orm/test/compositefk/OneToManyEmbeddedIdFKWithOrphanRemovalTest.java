/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyEmbeddedIdFKWithOrphanRemovalTest.System.class,
				OneToManyEmbeddedIdFKWithOrphanRemovalTest.SystemUser.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15098")
public class OneToManyEmbeddedIdFKWithOrphanRemovalTest {

	@Test
	public void testOrphanRemoval(SessionFactoryScope scope) {
		PK superUserKey = new PK( 2, "Martin" );
		scope.inTransaction(
				session -> {
					SystemUser superUser = new SystemUser( superUserKey, "Martin" );

					PK userKey = new PK( 2, "Dmitrii" );
					SystemUser user = new SystemUser( userKey, "Dmitrii" );

					System system = new System( 2, "sub1" );
					system.addUser( superUser );
					system.addUser( user );
					system = session.merge( system );

					system.getUsers().remove( 0 );

					PK newKey = new PK( 2, "Romana" );
					SystemUser newUser = new SystemUser( newKey, "Romana" );
					system.addUser( newUser );
				}
		);

		scope.inTransaction(
				session -> {
					System system = session.get( System.class, 2 );
					assertThat( system.getUsers().size(), is( 2 ) );
				}
		);

		scope.inTransaction(
				session -> {
					SystemUser user = session.get( SystemUser.class, superUserKey );
					assertNull( user );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();;
	}

	@Entity(name = "System")
	@Table(name = "systems")
	public static class System {
		@Id
		private Integer id;
		private String name;

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "system")
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
			user.system = this;
		}
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		@ManyToOne
		@JoinColumn(name = "system_id")
		@MapsId("subsystem")
		private System system;

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
