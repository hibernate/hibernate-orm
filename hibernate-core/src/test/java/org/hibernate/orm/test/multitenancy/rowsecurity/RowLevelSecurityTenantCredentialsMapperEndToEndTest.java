/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.rowsecurity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.cfg.JdbcSettings.JAKARTA_JDBC_URL;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractIsolation;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.resolveFromSettings;

@DomainModel( annotatedClasses = RowLevelSecurityTenantCredentialsMapperEndToEndTest.Document.class )
@SessionFactory
@ServiceRegistry( settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.multitenancy.rowsecurity.RowLevelSecurityTenantCredentialsMapperEndToEndTest$TenantResolver"),
		@Setting(name = MULTI_TENANT_CREDENTIALS_MAPPER,
				value = "org.hibernate.orm.test.multitenancy.rowsecurity.RowLevelSecurityTenantCredentialsMapperEndToEndTest$CredentialsMapper"),
		@Setting(name = CONNECTION_PROVIDER,
				value = "org.hibernate.orm.test.multitenancy.rowsecurity.RowLevelSecurityTenantCredentialsMapperEndToEndTest$TenantCredentialsConnectionProvider")
} )
@RequiresDialect( PostgreSQLDialect.class )
@RequiresDialectFeature( feature = DialectFeatureChecks.RowLevelSecurity.class )
class RowLevelSecurityTenantCredentialsMapperEndToEndTest {
	private static final String TENANT_ONE = "hibernate_rls_tenant_one";
	private static final String TENANT_TWO = "hibernate_rls_tenant_two";
	private static final String TENANT_ONE_DOCUMENT = "tenant-one-document";
	private static final String TENANT_TWO_DOCUMENT = "tenant-two-document";
	private static final String REJECTED_DOCUMENT = "rejected-document";

	static String currentTenant;

	@Test
	void rowLevelSecurityUsesCurrentUserWithTenantCredentialsMapper(SessionFactoryScope scope) {
		prepareTenantRoles( scope );

		currentTenant = TENANT_ONE;
		scope.inTransaction( session ->
				session.persist( new Document( TENANT_ONE_DOCUMENT, "tenant one" ) )
		);

		currentTenant = TENANT_TWO;
		scope.inTransaction( session ->
				session.persist( new Document( TENANT_TWO_DOCUMENT, "tenant two" ) )
		);

		currentTenant = TENANT_ONE;
		scope.inTransaction( session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( currentDatabaseUser( session ) ).isEqualTo( TENANT_ONE );
			assertThat( currentHibernateTenantSetting( session ) ).isNullOrEmpty();
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant one" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = TENANT_TWO;
		scope.inTransaction( session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( currentDatabaseUser( session ) ).isEqualTo( TENANT_TWO );
			assertThat( currentHibernateTenantSetting( session ) ).isNullOrEmpty();
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant two" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = TENANT_ONE;
		assertThatThrownBy( () -> scope.inTransaction( session ->
				session.doWork( connection -> {
					try ( var statement = connection.prepareStatement(
							"insert into rls_credentials_document (id, tenant_id, title) values (?, ?, ?)"
					) ) {
						statement.setString( 1, REJECTED_DOCUMENT );
						statement.setString( 2, TENANT_TWO );
						statement.setString( 3, "rejected" );
						statement.executeUpdate();
					}
				} )
		) )
				.isInstanceOf( RuntimeException.class );
	}

	private static void prepareTenantRoles(SessionFactoryScope scope) {
		final ConnectionProvider connectionProvider =
				scope.getSessionFactory().getServiceRegistry().requireService( ConnectionProvider.class );
		try {
			final Connection connection = connectionProvider.getConnection();
			try {
				assertThat( tableHasRowLevelSecurity( connection ) ).isTrue();
				try ( var statement = connection.createStatement() ) {
					createTenantRole( statement, TENANT_ONE );
					createTenantRole( statement, TENANT_TWO );
				}
				connection.commit();
			}
			catch (Exception e) {
				connection.rollback();
				throw e;
			}
			finally {
				connectionProvider.closeConnection( connection );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to prepare PostgreSQL tenant roles for RLS test", e );
		}
	}

	private static void createTenantRole(Statement statement, String roleName) throws SQLException {
		statement.execute( "do $$ begin create role " + roleName + " login password '" + roleName
				+ "'; exception when duplicate_object then null; end $$" );
		statement.execute( "alter role " + roleName + " login password '" + roleName
				+ "' nosuperuser nobypassrls" );
		statement.execute( "grant usage on schema public to " + roleName );
		statement.execute( "grant select, insert, update, delete on table rls_credentials_document to " + roleName );
	}

	private static boolean tableHasRowLevelSecurity(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement();
				var resultSet = statement.executeQuery(
						"select relrowsecurity and relforcerowsecurity "
								+ "from pg_class where oid = 'rls_credentials_document'::regclass"
				) ) {
			assertThat( resultSet.next() ).isTrue();
			return resultSet.getBoolean( 1 );
		}
	}

	private static String currentDatabaseUser(Session session) {
		return session.createNativeQuery( "select current_user", String.class ).getSingleResult();
	}

	private static String currentHibernateTenantSetting(Session session) {
		return session.createNativeQuery(
				"select current_setting('hibernate.tenant_id', true)",
				String.class
		).getSingleResult();
	}

	private static List<String> listDocumentTitles(Session session) {
		return session.createQuery( "select d.title from RlsCredentialsDocument d order by d.title", String.class )
				.getResultList();
	}

	private static Long countRowsDirectly(Session session) {
		final Number count = (Number) session.createNativeQuery( "select count(*) from rls_credentials_document" )
				.getSingleResult();
		return count.longValue();
	}

	public static class TenantResolver implements CurrentTenantIdentifierResolver<String> {
		@Override
		public @Nonnull String resolveCurrentTenantIdentifier() {
			return currentTenant;
		}

		@Override
		public boolean validateExistingCurrentSessions() {
			return false;
		}
	}

	public static class CredentialsMapper implements TenantCredentialsMapper<String> {
		@Override
		public @Nonnull String user(@Nonnull String tenantIdentifier) {
			return tenantIdentifier;
		}

		@Override
		public @Nonnull String password(@Nonnull String tenantIdentifier) {
			return tenantIdentifier;
		}
	}

	public static class TenantCredentialsConnectionProvider extends ConnectionProviderDelegate {
		private final Set<Connection> tenantConnections =
				Collections.synchronizedSet( Collections.newSetFromMap( new IdentityHashMap<>() ) );

		private String url;
		private String driverClassName;
		private Properties connectionProperties;
		private boolean autoCommit;
		private Integer isolation;

		public TenantCredentialsConnectionProvider() {
			setConnectionProvider( SharedDriverManagerConnectionProvider.getInstance() );
		}

		@Override
		public void configure(@NonNull Map<String, Object> configurationValues) {
			final Map<String, Object> resolvedConfigurationValues = new HashMap<>( configurationValues );
			resolveFromSettings( resolvedConfigurationValues );
			super.configure( resolvedConfigurationValues );
			url = (String) resolvedConfigurationValues.get( URL );
			if ( url == null ) {
				url = (String) resolvedConfigurationValues.get( JAKARTA_JDBC_URL );
			}
			driverClassName = (String) resolvedConfigurationValues.get( DRIVER );
			connectionProperties = ConnectionProviderInitiator.getConnectionProperties( resolvedConfigurationValues );
			autoCommit = getBoolean( AUTOCOMMIT, resolvedConfigurationValues );
			isolation = extractIsolation( resolvedConfigurationValues );
		}

		@Override
		public Connection getConnection(String user, String password) throws SQLException {
			final var tenantConnectionProperties = new Properties();
			tenantConnectionProperties.putAll( connectionProperties );
			tenantConnectionProperties.setProperty( "user", user );
			tenantConnectionProperties.setProperty( "password", password );
			loadDriverIfNeeded();
			final Connection connection = DriverManager.getConnection( url, tenantConnectionProperties );
			try {
				if ( isolation != null ) {
					connection.setTransactionIsolation( isolation );
				}
				if ( connection.getAutoCommit() != autoCommit ) {
					connection.setAutoCommit( autoCommit );
				}
				tenantConnections.add( connection );
				return connection;
			}
			catch (SQLException | RuntimeException e) {
				connection.close();
				throw e;
			}
		}

		@Override
		public void closeConnection(Connection connection) throws SQLException {
			if ( tenantConnections.remove( connection ) ) {
				connection.close();
			}
			else {
				super.closeConnection( connection );
			}
		}

		private void loadDriverIfNeeded() {
			if ( driverClassName != null ) {
				try {
					Class.forName( driverClassName );
				}
				catch (ClassNotFoundException e) {
					throw new IllegalStateException( "Unable to load JDBC driver " + driverClassName, e );
				}
			}
		}
	}

	@Entity( name = "RlsCredentialsDocument" )
	@Table( name = "rls_credentials_document" )
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
