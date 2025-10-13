/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jan Schatteman
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				LazySqlResultMappingTest.User.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazySqlResultMappingTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					session.persist( new User( 1L, (byte) 1 ) );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testGetIdAndPrincipalUsingFieldResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					List<User> users = session.createNamedQuery( "getIdAndPrincipalUsingFieldResults", User.class )
							.getResultList();
					Assertions.assertTrue( Hibernate.isPropertyInitialized( users.get( 0 ), "principal" ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-953")
	public void testGetIdAndPrincipalWithoutUsingFieldResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					List<User> users = session.createNamedQuery( "getIdAndPrincipalWithoutUsingFieldResults",
							User.class ).getResultList();
					Assertions.assertTrue( Hibernate.isPropertyInitialized( users.get( 0 ), "principal" ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15667")
	@FailureExpected(jiraKey = "HHH-15667",
			reason = "SQLGrammarException: Unable to find column position by name: principal")
	public void testGetIdWithoutUsingFieldResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					List<User> users = session.createNamedQuery( "getIdWithoutUsingFieldResults", User.class )
							.getResultList();
					Assertions.assertFalse( Hibernate.isPropertyInitialized( users.get( 0 ), "principal" ) );
				}
		);
	}

	@Test
	public void testGetIdUsingFieldResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					List<User> users = session.createNamedQuery( "getIdUsingFieldResults", User.class ).getResultList();
					Assertions.assertFalse( Hibernate.isPropertyInitialized( users.get( 0 ), "principal" ) );
				}
		);
	}

	@NamedNativeQuery(name = "getIdAndPrincipalUsingFieldResults",
			query = "select u.id as id, u.principal as principal from user_tbl u",
			resultSetMapping = "id_and_principal_with_fields")
	@NamedNativeQuery(name = "getIdUsingFieldResults", query = "select u.id as id from user_tbl u",
			resultSetMapping = "id_with_fields")
	@NamedNativeQuery(name = "getIdAndPrincipalWithoutUsingFieldResults",
			query = "select u.id as id, u.principal as principal from user_tbl u", resultSetMapping = "without_fields")
	@NamedNativeQuery(name = "getIdWithoutUsingFieldResults", query = "select u.id as id from user_tbl u",
			resultSetMapping = "without_fields")

	@SqlResultSetMapping(name = "id_and_principal_with_fields",
			entities = @EntityResult(entityClass = User.class,
					fields = {@FieldResult(name = "id", column = "id"), @FieldResult(name = "principal",
							column = "principal")})
	)
	@SqlResultSetMapping(name = "id_with_fields",
			entities = @EntityResult(entityClass = User.class, fields = {@FieldResult(name = "id", column = "id")})
	)
	@SqlResultSetMapping(name = "without_fields",
			entities = @EntityResult(entityClass = User.class)
	)

	@Entity(name = "User")
	@Table(name = "user_tbl")
	static class User {
		@Id
		private Long id;
		@Basic(fetch = FetchType.LAZY)
		private Byte principal;

		public User() {
		}

		public User(Long id, Byte principal) {
			this.id = id;
			this.principal = principal;
		}
	}

}
