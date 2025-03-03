/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory( useCollectingStatementInspector = true )
@DomainModel( annotatedClasses = {
		NestedOneToManyFetchTest.Role.class,
		NestedOneToManyFetchTest.Permission.class,
		NestedOneToManyFetchTest.Operation.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16210" )
public class NestedOneToManyFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Role( "TEST_ROLE" ) );
			session.persist( new Permission( "TEST_ROLE", "scope_1" ) );
			session.persist( new Permission( "TEST_ROLE", "scope_2" ) );
			session.persist( new Operation( "scope_1", "CREATE" ) );
			session.persist( new Operation( "scope_1", "UPDATE" ) );
			session.persist( new Operation( "scope_1", "DELETE" ) );
			session.persist( new Operation( "scope_2", "CREATE" ) );
			session.persist( new Operation( "scope_2", "UPDATE" ) );
			session.persist( new Operation( "scope_2", "DELETE" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Operation" ).executeUpdate();
			session.createMutationQuery( "delete from Permission" ).executeUpdate();
			session.createMutationQuery( "delete from Role" ).executeUpdate();
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Role role = session.find( Role.class, 1L );
			statementInspector.clear();
			assertTestResults( role.getPermissions(), statementInspector, false );
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Role role = session.createQuery( "from Role r", Role.class ).getSingleResult();
			statementInspector.clear();
			assertTestResults( role.getPermissions(), statementInspector, false );
		} );
	}

	@Test
	public void testQueryExplicitJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Role role = session.createQuery(
					"from Role r join fetch r.permissions p join p.operations",
					Role.class
			).getSingleResult();
			assertThat( role.getPermissions() ).hasSize( 6 ); // child elements are duplicated here
			statementInspector.assertNumberOfJoins( 0, 2 );
			statementInspector.assertExecutedCount( 3 );
			for ( Permission p : role.getPermissions() ) {
				assertThat( Hibernate.isInitialized( p.getOperations() ) ).isTrue();
				assertThat( p.getOperations() ).hasSize( 3 );
			}
		} );
	}

	@Test
	public void testChildQuery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final List<Permission> resultList = session.createQuery(
					"from Permission p",
					Permission.class
			).getResultList();
			assertTestResults( resultList, statementInspector, false );
		} );
	}

	@Test
	public void testChildQueryExplicitJoinFetch(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final List<Permission> resultList = session.createQuery(
					"from Permission p join fetch p.operations",
					Permission.class
			).getResultList();
			assertTestResults( resultList, statementInspector, true );
		} );
	}

	private void assertTestResults(
			List<Permission> permissions,
			SQLStatementInspector statementInspector,
			boolean shouldJoin) {
		assertThat( permissions ).hasSize( 2 );
		if ( shouldJoin ) {
			statementInspector.assertNumberOfJoins( 0, 1 );
			statementInspector.assertExecutedCount( 1 );
		}
		else {
			statementInspector.assertNumberOfJoins( 0, 0 );
			statementInspector.assertExecutedCount( 3 ); // 1 for permissions, 2 for each permission.operation
		}
		assertThat( Hibernate.isInitialized( permissions.get( 0 ).getOperations() ) ).isTrue();
		assertThat( permissions.get( 0 ).getOperations() ).hasSize( 3 );
		assertThat( Hibernate.isInitialized( permissions.get( 1 ).getOperations() ) ).isTrue();
		assertThat( permissions.get( 1 ).getOperations() ).hasSize( 3 );
	}

	@Entity( name = "Role" )
	@Table( name = "roles" )
	public static class Role {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		@JoinColumn( name = "roleName", referencedColumnName = "name" )
		private List<Permission> permissions = new ArrayList<>();

		public Role() {
		}

		public Role(String name) {
			this.name = name;
		}

		public List<Permission> getPermissions() {
			return permissions;
		}
	}

	@Entity( name = "Permission" )
	@Table( name = "permissions" )
	public static class Permission {
		@Id
		@GeneratedValue
		private Long id;

		private String roleName;

		private String scopeName;

		@OneToMany( cascade = CascadeType.ALL, fetch = FetchType.EAGER )
		@JoinColumn( name = "scopeName", referencedColumnName = "scopeName" )
		private List<Operation> operations = new ArrayList<>();

		public Permission() {
		}

		public Permission(String roleName, String scopeName) {
			this.roleName = roleName;
			this.scopeName = scopeName;
		}

		public List<Operation> getOperations() {
			return operations;
		}
	}

	@Entity( name = "Operation" )
	@Table( name = "operations" )
	public static class Operation {
		@Id
		@GeneratedValue
		private Long id;

		private String scopeName;

		private String operationName;

		public Operation() {
		}

		public Operation(String scopeName, String operationName) {
			this.scopeName = scopeName;
			this.operationName = operationName;
		}
	}
}
