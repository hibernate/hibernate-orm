/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.util.EnumSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.ValidationMode;
import jakarta.validation.constraints.NotEmpty;

/**
 * @author Jan Schatteman
 */
@Jpa( annotatedClasses = {
		LazyCollectionValidationTest.UserWithEnumRoles.class,
		LazyCollectionValidationTest.UserWithStringRoles.class,
		LazyCollectionValidationTest.StringUserRole.class,
		LazyCollectionValidationTest.EnumUserRole.class
}, validationMode = ValidationMode.AUTO )
@JiraKey( value = "HHH-16701" )
public class LazyCollectionValidationTest {
	@AfterAll
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from enumuser" ).executeUpdate();
			entityManager.createQuery( "delete from stringuser" ).executeUpdate();
			entityManager.createQuery( "delete from stringuserrole" ).executeUpdate();
		} );
	}

	@Test
	public void testWithEnumCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			UserWithEnumRoles a = new UserWithEnumRoles();
			a.setUserRoles( EnumSet.of( EnumUserRole.USER ) );
			entityManager.persist( a );
			UserWithEnumRoles user = new UserWithEnumRoles();
			user.setUserRoles( EnumSet.of( EnumUserRole.USER ) );
			user.setCreatedBy( a );
			entityManager.persist( user );
		} );
		scope.inTransaction( entityManager -> {
			for ( UserWithEnumRoles user : entityManager.createQuery(
					"Select u from enumuser u",
					UserWithEnumRoles.class
			).getResultList() ) {
				entityManager.remove( user );
			}
		} );
	}

	@Test
	public void testWithNormalCollection(EntityManagerFactoryScope scope) {
		final Set<StringUserRole> bosses = scope.fromTransaction( entityManager -> {
			StringUserRole role1 = new StringUserRole( 1, "Boss" );
			StringUserRole role2 = new StringUserRole( 2, "SuperBoss" );
			entityManager.persist( role1 );
			entityManager.persist( role2 );
			return Set.of( role1, role2 );
		} );
		final Set<StringUserRole> dorks = scope.fromTransaction( entityManager -> {
			StringUserRole role1 = new StringUserRole( 3, "Dork" );
			StringUserRole role2 = new StringUserRole( 4, "SuperDork" );
			entityManager.persist( role1 );
			entityManager.persist( role2 );
			return Set.of( role1, role2 );
		} );
		scope.inTransaction( entityManager -> {
			UserWithStringRoles a = new UserWithStringRoles();
			a.setUserRoles( bosses );
			entityManager.persist( a );

			UserWithStringRoles userWithEnumRoles = new UserWithStringRoles();
			userWithEnumRoles.setUserRoles( dorks );
			userWithEnumRoles.setCreatedBy( a );
			entityManager.persist( userWithEnumRoles );
		} );
		scope.inTransaction( entityManager -> {
			for ( UserWithStringRoles userWithStringRoles : entityManager.createQuery(
					"Select u from stringuser u",
					UserWithStringRoles.class
			).getResultList() ) {
				entityManager.remove( userWithStringRoles );
			}
		} );
	}

	@Entity( name = "enumuser" )
	@Table( name = "enum_user" )
	public static class UserWithEnumRoles {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		private long id;

		@ManyToOne( fetch = FetchType.LAZY )
		private UserWithEnumRoles createdBy;

		@Enumerated( EnumType.STRING )
		@ElementCollection( fetch = FetchType.LAZY )
		@NotEmpty
		private Set<EnumUserRole> userRoles;

		public UserWithEnumRoles() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public UserWithEnumRoles getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(UserWithEnumRoles createdBy) {
			this.createdBy = createdBy;
		}

		public Set<EnumUserRole> getUserRoles() {
			return userRoles;
		}

		public void setUserRoles(Set<EnumUserRole> userRoles) {
			this.userRoles = userRoles;
		}
	}

	@Entity( name = "stringuser" )
	@Table( name = "string_user" )
	public static class UserWithStringRoles {
		private long id;

		private UserWithStringRoles createdBy;

		private Set<StringUserRole> userRoles;

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		@ManyToOne( fetch = FetchType.LAZY )
		public UserWithStringRoles getCreatedBy() {
			return createdBy;
		}

		public void setCreatedBy(UserWithStringRoles createdBy) {
			this.createdBy = createdBy;
		}

		@OneToMany( fetch = FetchType.LAZY )
		@NotEmpty
		public Set<StringUserRole> getUserRoles() {
			return userRoles;
		}

		public void setUserRoles(Set<StringUserRole> userRoles) {
			this.userRoles = userRoles;
		}
	}

	public enum EnumUserRole {
		USER, ADMIN, OPERATOR
	}

	@Entity( name = "stringuserrole" )
	@Table( name = "string_user_role" )
	public static class StringUserRole {
		@Id
		private long id;
		@Column( name = "role_name" )
		private String role;

		public StringUserRole() {
		}

		public StringUserRole(long id, String role) {
			this.id = id;
			this.role = role;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}
	}
}
