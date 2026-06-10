/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.rowsecurity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
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
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.jdbc.ConnectionProviderDelegate;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.AUTOCOMMIT;
import static org.hibernate.cfg.JdbcSettings.DRIVER;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.extractIsolation;
import static org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator.getConnectionProperties;
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
		final var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final String tenantOne = tenantOne( dialect );
		final String tenantTwo = tenantTwo( dialect );
		prepareTenantRoles( scope, dialect, tenantOne, tenantTwo );

		currentTenant = tenantOne;
		scope.inTransaction( session ->
				session.persist( new Document( TENANT_ONE_DOCUMENT, "tenant one" ) )
		);

		currentTenant = tenantTwo;
		scope.inTransaction( session ->
				session.persist( new Document( TENANT_TWO_DOCUMENT, "tenant two" ) )
		);

		currentTenant = tenantOne;
		scope.inTransaction( session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( currentDatabaseUser( session ) ).isEqualTo( tenantOne );
			assertThat( currentHibernateTenantSetting( session ) ).isNullOrEmpty();
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant one" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = tenantTwo;
		scope.inTransaction( session -> {
			session.disableFilter( TenantIdBinder.FILTER_NAME );
			assertThat( currentDatabaseUser( session ) ).isEqualTo( tenantTwo );
			assertThat( currentHibernateTenantSetting( session ) ).isNullOrEmpty();
			assertThat( listDocumentTitles( session ) ).containsExactly( "tenant two" );
			assertThat( countRowsDirectly( session ) ).isEqualTo( 1L );
		} );

		currentTenant = tenantOne;
		assertThatThrownBy( () -> scope.inTransaction( session ->
				session.doWork( connection -> {
					try ( var statement = connection.prepareStatement(
							"insert into rls_credentials_document (id, tenant_id, title) values (?, ?, ?)"
					) ) {
						statement.setString( 1, REJECTED_DOCUMENT );
						statement.setString( 2, tenantTwo );
						statement.setString( 3, "rejected" );
						statement.executeUpdate();
					}
				} )
		) )
				.isInstanceOf( RuntimeException.class );
	}

	private static String tenantOne(Dialect dialect) {
		return tenantName( dialect, TENANT_ONE );
	}

	private static String tenantTwo(Dialect dialect) {
		return tenantName( dialect, TENANT_TWO );
	}

	private static String tenantName(Dialect dialect, String tenantName) {
		return dialect instanceof DB2Dialect
				? tenantName.toUpperCase( Locale.ROOT )
				: tenantName;
	}

	private static void prepareTenantRoles(
			SessionFactoryScope scope,
			Dialect dialect,
			String tenantOne,
			String tenantTwo) {
		final var connectionProvider =
				scope.getSessionFactory().getServiceRegistry()
						.requireService( ConnectionProvider.class );
		try {
			final var connection = connectionProvider.getConnection();
			try {
				assertThat( tableHasRowLevelSecurity( connection, dialect ) ).isTrue();
				try ( var statement = connection.createStatement() ) {
					createTenantRole( statement, dialect, tenantOne );
					createTenantRole( statement, dialect, tenantTwo );
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
			throw new RuntimeException( "Unable to prepare tenant roles for RLS test", e );
		}
	}

	private static void createTenantRole(Statement statement, Dialect dialect, String roleName)
			throws SQLException {
		if ( dialect instanceof PostgreSQLDialect ) {
			createPostgreSqlTenantRole( statement, roleName );
		}
		else if ( dialect instanceof CockroachDialect ) {
			createCockroachTenantRole( statement, roleName );
		}
		else if ( dialect instanceof SQLServerDialect ) {
			createSqlServerTenantUser( statement, roleName );
		}
		else if ( dialect instanceof DB2Dialect ) {
			grantDb2TenantUser( statement, roleName );
		}
		else {
			throw new IllegalArgumentException( "Unsupported RLS tenant credentials test dialect: " + dialect );
		}
	}

	private static void createPostgreSqlTenantRole(Statement statement, String roleName)
			throws SQLException {
		statement.execute( "do $$ begin create role " + roleName + " login password '" + roleName
				+ "'; exception when duplicate_object then null; end $$" );
		statement.execute( "alter role " + roleName + " login password '" + roleName
				+ "' nosuperuser nobypassrls" );
		statement.execute( "grant usage on schema public to " + roleName );
		statement.execute( "grant select, insert, update, delete on table rls_credentials_document to " + roleName );
	}

	private static void createCockroachTenantRole(Statement statement, String roleName)
			throws SQLException {
		statement.execute( "create user if not exists " + roleName );
		statement.execute( "alter role " + roleName + " nobypassrls" );
		statement.execute( "grant usage on schema public to " + roleName );
		statement.execute( "grant select, insert, update, delete on table rls_credentials_document to " + roleName );
	}

	private static void createSqlServerTenantUser(Statement statement, String roleName)
			throws SQLException {
		final String userName = sqlServerIdentifier( roleName );
		final String password = sqlServerStringLiteral( roleName );
		statement.execute( "if suser_id(" + password + ") is null exec("
				+ sqlServerStringLiteral(
						"create login " + userName + " with password = " + password
								+ ", check_policy = off, check_expiration = off"
				)
				+ ")" );
		statement.execute( "alter login " + userName + " with password = " + password
				+ ", check_policy = off, check_expiration = off" );
		statement.execute( "if user_id(" + password + ") is null exec("
				+ sqlServerStringLiteral( "create user " + userName + " for login " + userName )
				+ ")" );
		statement.execute( "grant select, insert, update, delete on dbo.rls_credentials_document to " + userName );
	}

	private static void grantDb2TenantUser(Statement statement, String roleName) throws SQLException {
		statement.execute( "grant connect on database to user " + roleName );
		final String tenantName = currentDb2Tenant( statement );
		if ( !"SYSTEM".equals( tenantName ) ) {
			statement.execute( "grant usage on tenant " + tenantName + " to user " + roleName );
		}
		statement.execute( "grant select, insert, update, delete on table rls_credentials_document to user " + roleName );
	}

	private static String currentDb2Tenant(Statement statement) throws SQLException {
		try ( var resultSet = statement.executeQuery( "values current tenant" ) ) {
			assertThat( resultSet.next() ).isTrue();
			return resultSet.getString( 1 ).trim();
		}
	}

	private static boolean tableHasRowLevelSecurity(Connection connection, Dialect dialect) throws SQLException {
		if ( dialect instanceof SQLServerDialect ) {
			return sqlServerTableHasRowLevelSecurity( connection );
		}
		else if ( dialect instanceof DB2Dialect ) {
			return db2TableHasRowLevelSecurity( connection );
		}
		else {
			return postgreSqlTableHasRowLevelSecurity( connection );
		}
	}

	private static boolean postgreSqlTableHasRowLevelSecurity(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement();
				var resultSet = statement.executeQuery(
						"select relrowsecurity and relforcerowsecurity "
								+ "from pg_class where oid = 'rls_credentials_document'::regclass"
				) ) {
			assertThat( resultSet.next() ).isTrue();
			return resultSet.getBoolean( 1 );
		}
	}

	private static boolean sqlServerTableHasRowLevelSecurity(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement();
				var resultSet = statement.executeQuery(
						"select cast(case when exists ("
								+ "select 1 from sys.security_policies p "
								+ "join sys.security_predicates sp on sp.object_id = p.object_id "
								+ "where p.is_enabled = 1 "
								+ "and sp.target_object_id = object_id(N'dbo.rls_credentials_document')"
								+ ") then 1 else 0 end as bit)"
				) ) {
			assertThat( resultSet.next() ).isTrue();
			return resultSet.getBoolean( 1 );
		}
	}

	private static boolean db2TableHasRowLevelSecurity(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement();
				var resultSet = statement.executeQuery(
						"select case when t.control in ('R', 'B') and exists ("
								+ "select 1 from syscat.controls c "
								+ "where c.tabschema = t.tabschema "
								+ "and c.tabname = t.tabname "
								+ "and c.controltype = 'R' "
								+ "and c.enable = 'Y' "
								+ "and c.valid = 'Y'"
								+ ") then 1 else 0 end "
								+ "from syscat.tables t "
								+ "where t.tabschema = current schema and t.tabname = 'RLS_CREDENTIALS_DOCUMENT'"
				) ) {
			assertThat( resultSet.next() ).isTrue();
			return resultSet.getBoolean( 1 );
		}
	}

	private static String currentDatabaseUser(Session session) {
		return session.createNativeQuery( "select current_user", String.class )
				.getSingleResult().trim();
	}

	private static String currentHibernateTenantSetting(Session session) {
		final var dialect =
				session.getSessionFactory()
						.unwrap( SessionFactoryImplementor.class )
						.getJdbcServices()
						.getDialect();
		if ( dialect instanceof SQLServerDialect ) {
			return session.createNativeQuery(
					"select cast(session_context(N'hibernate.tenant_id') as varchar(255))",
					String.class
			).getSingleResult();
		}
		else if ( dialect instanceof CockroachDialect ) {
			return session.createNativeQuery(
					"select nullif(substring(current_setting('application_name', true) "
							+ "from '^hibernate_orm_rls:[^:]*:(.*)$'), '')",
					String.class
			).getSingleResult();
		}
		else if ( dialect instanceof DB2Dialect ) {
			return null;
		}
		else {
			return session.createNativeQuery(
					"select current_setting('hibernate.tenant_id', true)",
					String.class
			).getSingleResult();
		}
	}

	private static String sqlServerIdentifier(String value) {
		return "[" + value.replace( "]", "]]" ) + "]";
	}

	private static String sqlServerStringLiteral(String value) {
		return "N'" + value.replace( "'", "''" ) + "'";
	}

	private static List<String> listDocumentTitles(Session session) {
		return session.createQuery( "select d.title from RlsCredentialsDocument d order by d.title", String.class )
				.getResultList();
	}

	private static Long countRowsDirectly(Session session) {
		return session.createNativeQuery( "select count(*) from rls_credentials_document", Long.class )
				.getSingleResult();
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
			return tenantIdentifier.toLowerCase( Locale.ROOT );
		}

		@Override
		public @Nonnull String password(@Nonnull String tenantIdentifier) {
			return tenantIdentifier.toLowerCase( Locale.ROOT );
		}
	}

	public static class TenantCredentialsConnectionProvider extends ConnectionProviderDelegate {
		private final Set<Connection> tenantConnections =
				synchronizedSet( newSetFromMap( new IdentityHashMap<>() ) );

		private String url;
		private String driverClassName;
		private Properties connectionProperties;
		private String defaultSchema;
		private boolean autoCommit;
		private Integer isolation;

		public TenantCredentialsConnectionProvider() {
			setConnectionProvider( SharedDriverManagerConnectionProvider.getInstance() );
		}

		@Override
		public void configure(@NonNull Map<String, Object> configurationValues) {
			final Map<String, Object> resolvedConfigurationValues =
					new HashMap<>( configurationValues );
			resolveFromSettings( resolvedConfigurationValues );
			super.configure( resolvedConfigurationValues );
			url = (String) resolvedConfigurationValues.get( URL );
			driverClassName = (String) resolvedConfigurationValues.get( DRIVER );
			connectionProperties = getConnectionProperties( resolvedConfigurationValues );
			defaultSchema = defaultSchema( connectionProperties );
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
			final var connection = DriverManager.getConnection( url, tenantConnectionProperties );
			try {
				if ( isolation != null ) {
					connection.setTransactionIsolation( isolation );
				}
				if ( connection.getAutoCommit() != autoCommit ) {
					connection.setAutoCommit( autoCommit );
				}
				setDb2CurrentSchemaIfNeeded( connection );
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

		private void setDb2CurrentSchemaIfNeeded(Connection connection) throws SQLException {
			if ( defaultSchema != null && url != null && url.startsWith( "jdbc:db2:" ) ) {
				try ( var statement = connection.createStatement() ) {
					statement.execute( "set current schema = " + defaultSchema );
				}
			}
		}

		private static String defaultSchema(Properties connectionProperties) {
			final String user = connectionProperties.getProperty( "user" );
			return user == null ? null : user.toUpperCase( Locale.ROOT );
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
