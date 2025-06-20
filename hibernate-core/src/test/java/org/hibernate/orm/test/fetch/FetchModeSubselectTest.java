/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				FetchModeSubselectTest.User.class,
				FetchModeSubselectTest.Role.class,
				FetchModeSubselectTest.Agency.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15453")
public class FetchModeSubselectTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User ann = new User( 1, "Ann" );
					User bob = new User( 2, "Bob" );

					Role manager = new Role( 1, "manager" );
					Role developer = new Role( 2, "developer" );

					ann.addRole( manager );

					Agency agency = new Agency( 100, "Test Agency" );

					agency.addUser( ann );
					agency.addUser( bob );

					session.persist( ann );
					session.persist( bob );
					session.persist( manager );
					session.persist( developer );
					session.persist( agency );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectUserWithRole(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<User> userList = session.createQuery( "FROM User WHERE id = 1", User.class ).list();
					assertThat( userList.size() ).isEqualTo( 1 );
					assertThat( userList.get( 0 ).getRoles().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testGetUserWithRole(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					User user = session.get( User.class, 1 );
					assertThat( user ).isNotNull();
					assertThat( user.getRoles().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testSelectUserWithoutRole(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<User> userList = session.createQuery( "FROM User WHERE id = 2", User.class ).list();
					assertThat( userList.size() ).isEqualTo( 1 );
					assertThat( userList.get( 0 ).getRoles().size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testGetUserWithoutRole(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					User user = session.get( User.class, 2 );
					assertThat( user ).isNotNull();
					assertThat( user.getRoles().size() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User {

		private Integer id;
		private String name;
		private Agency agency;
		private Set<Role> roles;

		public User() {
		}

		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "USER_ID")
		public Integer getId() {
			return id;
		}

		public void setId(Integer userId) {
			this.id = userId;
		}

		@Column(name = "USER_NAME")
		public String getName() {
			return name;
		}

		public void setName(String userName) {
			this.name = userName;
		}

		@ManyToOne
		@JoinColumn(name = "AGENCY_ID")
		public Agency getAgency() {
			return agency;
		}

		public void setAgency(Agency agency) {
			this.agency = agency;
		}

		@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
		@JoinTable(
				name = "USER_ROLE",
				joinColumns = @JoinColumn(name = "USER_ID"),
				inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
		@Fetch(FetchMode.SUBSELECT)
		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}

		public void addRole(Role role) {
			if ( roles == null ) {
				roles = new HashSet<>();
			}
			roles.add( role );
		}

	}

	@Entity(name = "Role")
	@Table(name = "ROLE_TABLE")
	public static class Role {

		private Integer id;
		private String name;

		public Role() {
		}

		public Role(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "ROLE_ID")
		public Integer getId() {
			return id;
		}

		public void setId(Integer roleId) {
			this.id = roleId;
		}

		@Column(name = "ROLE_NAME")
		public String getName() {
			return name;
		}

		public void setName(String roleName) {
			this.name = roleName;
		}

	}

	@Entity(name = "Agency")
	@Table(name = "AGENCY_TABLE")
	public static class Agency {

		private Integer id;
		private String name;
		private Set<User> users;

		public Agency() {
		}

		public Agency(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "AGENCY_ID")
		public Integer getId() {
			return id;
		}

		public void setId(Integer roleId) {
			this.id = roleId;
		}

		@Column(name = "AGENCY_NAME")
		public String getName() {
			return name;
		}

		public void setName(String roleName) {
			this.name = roleName;
		}

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "agency")
		@Fetch(FetchMode.SELECT)
		public Set<User> getUsers() {
			return users;
		}

		public void setUsers(Set<User> users) {
			this.users = users;
		}

		public void addUser(User user) {
			if ( users == null ) {
				users = new HashSet<>();
			}
			users.add( user );
			user.setAgency( this );
		}
	}
}
