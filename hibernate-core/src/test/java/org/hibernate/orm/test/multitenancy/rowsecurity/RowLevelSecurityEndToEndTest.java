/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.rowsecurity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel( annotatedClasses = RowLevelSecurityEndToEndTest.Document.class )
@SessionFactory
@ServiceRegistry( settings =
		{@Setting(name = MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.multitenancy.rowsecurity.RowLevelSecurityEndToEndTest$TenantResolver"),
				@Setting( name = "hibernate.dialect.oracle.deep_data_security.tenant_context_name", value = "DEVELOPER.HIBERNATE_TENANCY"),
				@Setting( name ="hibernate.dialect.oracle.deep_data_security.tenant_data_grantee", value = "hibernate_dds_role"),
				@Setting( name = "hibernate.dialect.oracle.deep_data_security.tenant_database_role", value = "hibernate_dds_database_role")})
@RequiresDialectFeature( feature = DialectFeatureChecks.RowLevelSecurity.class )
class RowLevelSecurityEndToEndTest {
	private static final String RLS_TEST_ROLE = "hibernate_rls_test";

	private static final String TENANT_ONE = "tenant-one";
	private static final String TENANT_TWO = "tenant-two";
	private static final String ROOT_TENANT = "root-tenant";
	private static final String TENANT_ONE_DOCUMENT = "tenant-one-document";
	private static final String TENANT_TWO_DOCUMENT = "tenant-two-document";
	private static final String REJECTED_DOCUMENT = "rejected-document";

	public static class TenantResolver implements CurrentTenantIdentifierResolver<String> {
		@Override
		public String resolveCurrentTenantIdentifier() {
			return currentTenant;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}

		@Override
		public boolean isRoot(String tenantId) {
			return ROOT_TENANT.equals( tenantId );
		}
	}

	static String currentTenant;
	private boolean usePostgreSqlRlsTestRole;
	private boolean useSqlServerRlsTestUser;

	@Test
	void rowLevelSecurityIsEnforcedByTheDatabase(SessionFactoryScope scope) {
		prepareRlsRoleIfNeeded( scope );

		currentTenant = TENANT_ONE;
		inRlsTransaction( scope, session -> {
			session.persist( new Document( TENANT_ONE_DOCUMENT, "tenant one" ) );
		} );

		currentTenant = TENANT_TWO;
		inRlsTransaction( scope, session -> {
			session.persist( new Document( TENANT_TWO_DOCUMENT, "tenant two" ) );
		} );

		currentTenant = TENANT_ONE;
		inRlsTransaction( scope, session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant one" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = TENANT_TWO;
		inRlsTransaction( scope, session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant two" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = ROOT_TENANT;
		inRlsTransaction( scope, session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant one", "tenant two" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 2L );
		} );

		currentTenant = TENANT_ONE;
		assertThatThrownBy( () -> inRlsTransaction( scope, session -> {
			session.doWork( connection -> {
				try ( var statement = connection.prepareStatement(
						"insert into rls_document (id, tenant_id, title) values (?, ?, ?)"
				) ) {
					statement.setString( 1, REJECTED_DOCUMENT );
					statement.setString( 2, TENANT_TWO );
					statement.setString( 3, "rejected" );
					statement.executeUpdate();
				}
			} );
		} ) )
				.isInstanceOf( RuntimeException.class );
	}

	private void inRlsTransaction(
			SessionFactoryScope scope,
			java.util.function.Consumer<org.hibernate.Session> action) {
		scope.inTransaction( session -> {
			useRlsTestRoleIfNeeded( session );
			try {
				action.accept( session );
			}
			finally {
				revertRlsTestRoleIfNeeded( session );
			}
		} );
	}

	private void prepareRlsRoleIfNeeded(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		if ( dialect instanceof PostgreSQLDialect ) {
			preparePostgreSqlRlsRoleIfNeeded( scope );
		}
		else if ( dialect instanceof CockroachDialect ) {
			prepareCockroachRlsRoleIfNeeded( scope );
		}
		else if ( dialect instanceof SQLServerDialect ) {
			prepareSqlServerRlsUserIfNeeded( scope );
		}
		else {
			usePostgreSqlRlsTestRole = false;
			useSqlServerRlsTestUser = false;
		}
	}

	private void preparePostgreSqlRlsRoleIfNeeded(SessionFactoryScope scope) {
		currentTenant = ROOT_TENANT;
		scope.inTransaction( session -> {
			assertThat( tableHasRowLevelSecurity( session ) ).isTrue();
			usePostgreSqlRlsTestRole = currentRoleBypassesRowLevelSecurity( session );
			if ( usePostgreSqlRlsTestRole ) {
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "do $$ begin create role " + RLS_TEST_ROLE
								+ "; exception when duplicate_object then null; end $$" );
						statement.execute( "alter role " + RLS_TEST_ROLE + " nosuperuser nobypassrls" );
						statement.execute( "grant usage on schema public to " + RLS_TEST_ROLE );
						statement.execute( "grant select, insert, update, delete on table rls_document to "
								+ RLS_TEST_ROLE );
					}
				} );
			}
		} );
	}

	private void prepareCockroachRlsRoleIfNeeded(SessionFactoryScope scope) {
		currentTenant = ROOT_TENANT;
		scope.inTransaction( session -> {
			assertThat( tableHasRowLevelSecurity( session ) ).isTrue();
			usePostgreSqlRlsTestRole = currentRoleBypassesRowLevelSecurity( session );
			if ( usePostgreSqlRlsTestRole ) {
				session.doWork( connection -> {
					try ( var statement = connection.createStatement() ) {
						statement.execute( "create role if not exists " + RLS_TEST_ROLE );
						statement.execute( "alter role " + RLS_TEST_ROLE + " nobypassrls" );
						statement.execute( "grant usage on schema public to " + RLS_TEST_ROLE );
						statement.execute( "grant select, insert, update, delete on table rls_document to "
								+ RLS_TEST_ROLE );
					}
				} );
			}
		} );
	}

	private void prepareSqlServerRlsUserIfNeeded(SessionFactoryScope scope) {
		currentTenant = ROOT_TENANT;
		scope.inTransaction( session -> {
			assertThat( sqlServerTableHasRowLevelSecurity( session ) ).isTrue();
			useSqlServerRlsTestUser = true;
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "if user_id(N'" + RLS_TEST_ROLE + "') is null create user "
							+ RLS_TEST_ROLE + " without login" );
					statement.execute( "grant select, insert, update, delete on dbo.rls_document to "
							+ RLS_TEST_ROLE );
				}
			} );
		} );
	}

	private static Boolean tableHasRowLevelSecurity(org.hibernate.Session session) {
		return session.createNativeQuery(
				"select relrowsecurity and relforcerowsecurity from pg_class where oid = 'rls_document'::regclass",
				Boolean.class
		).getSingleResult();
	}

	private static Boolean currentRoleBypassesRowLevelSecurity(org.hibernate.Session session) {
		return session.createNativeQuery(
				"select rolsuper or rolbypassrls from pg_roles where rolname = current_user",
				Boolean.class
		).getSingleResult();
	}

	private static Boolean sqlServerTableHasRowLevelSecurity(org.hibernate.Session session) {
		return session.createNativeQuery(
				"select cast(case when exists ("
						+ "select 1 from sys.security_policies p "
						+ "join sys.security_predicates sp on sp.object_id = p.object_id "
						+ "where p.is_enabled = 1 and sp.target_object_id = object_id(N'dbo.rls_document')"
						+ ") then 1 else 0 end as bit)",
				Boolean.class
		).getSingleResult();
	}

	private void useRlsTestRoleIfNeeded(org.hibernate.Session session) {
		if ( usePostgreSqlRlsTestRole ) {
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "set local role " + RLS_TEST_ROLE );
				}
			} );
			assertThat( currentRoleBypassesRowLevelSecurity( session ) ).isFalse();
		}
		else if ( useSqlServerRlsTestUser ) {
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "execute as user = '" + RLS_TEST_ROLE + "'" );
				}
			} );
			assertThat( currentSqlServerUser( session ) ).isEqualTo( RLS_TEST_ROLE );
		}
	}

	private void revertRlsTestRoleIfNeeded(org.hibernate.Session session) {
		if ( useSqlServerRlsTestUser ) {
			session.doWork( connection -> {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "revert" );
				}
			} );
		}
	}

	private static String currentSqlServerUser(org.hibernate.Session session) {
		return session.createNativeQuery( "select user_name()", String.class ).getSingleResult();
	}

	private static List<String> listDocumentTitles(org.hibernate.Session session) {
		return session.createQuery( "select d.title from RlsDocument d order by d.title", String.class )
				.getResultList();
	}

	private static Long countRowsDirectly(org.hibernate.Session session) {
		final Number count = (Number) session.createNativeQuery( "select count(*) from rls_document" )
				.getSingleResult();
		return count.longValue();
	}

	@Entity( name = "RlsDocument" )
	@Table( name = "rls_document" )
	static class Document {
		@Id
		String id;

		@TenantId
		@Column( name = "tenant_id", nullable = false )
		String tenantId;

		String title;

		Document() {
		}

		Document(String id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
