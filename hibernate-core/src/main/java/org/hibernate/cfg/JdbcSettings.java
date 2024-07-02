/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

import java.util.Calendar;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.query.Query;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * Settings related to JDBC, Connections, pools, Dialects, etc
 *
 * @author Steve Ebersole
 */
public interface JdbcSettings extends C3p0Settings, ProxoolSettings, AgroalSettings, HikariCPSettings {

	/**
	 * Specifies a JTA {@link javax.sql.DataSource} to use for Connections.
	 * Hibernate allows either
	 * <ul>
	 *     <li>
	 *         an instance of {@link javax.sql.DataSource}
	 *     </li>
	 *     <li>
	 *         a JNDI name under which to obtain the {@link javax.sql.DataSource};
	 *         see also {@link EnvironmentSettings#JNDI_URL}, {@link EnvironmentSettings#JNDI_CLASS},
	 *         {@link EnvironmentSettings#JNDI_PREFIX}
	 *     </li>
	 * </ul>
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JAKARTA_JTA_DATASOURCE = "jakarta.persistence.jtaDataSource";

	/**
	 * Specifies a non-JTA {@link javax.sql.DataSource} to use for Connections.
	 * Hibernate allows either
	 * <ul>
	 *     <li>
	 *         an instance of {@link javax.sql.DataSource}
	 *     </li>
	 *     <li>
	 *         a JNDI name under which to obtain the {@link javax.sql.DataSource};
	 *         see also {@link EnvironmentSettings#JNDI_URL}, {@link EnvironmentSettings#JNDI_CLASS},
	 *         {@link EnvironmentSettings#JNDI_PREFIX}
	 *     </li>
	 * </ul>
	 * <p>
	 * See JPA 2 sections 9.4.3 and 8.2.1.5
	 */
	String JAKARTA_NON_JTA_DATASOURCE = "jakarta.persistence.nonJtaDataSource";

	/**
	 * Specifies the name of a JDBC driver to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JAKARTA_JDBC_URL}, {@link #JAKARTA_JDBC_USER}
	 * and {@link #JAKARTA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use
	 * either {@link #JAKARTA_JTA_DATASOURCE} or {@link #JAKARTA_NON_JTA_DATASOURCE}
	 * instead.
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_DRIVER = "jakarta.persistence.jdbc.driver";

	/**
	 * Specifies the JDBC connection URL to use to connect to the database.
	 * <p>
	 * Used in conjunction with {@link #JAKARTA_JDBC_DRIVER}, {@link #JAKARTA_JDBC_USER}
	 * and {@link #JAKARTA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * When connections are obtained from a {@link javax.sql.DataSource}, use
	 * either {@link #JAKARTA_JTA_DATASOURCE} or {@link #JAKARTA_NON_JTA_DATASOURCE}
	 * instead.
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_URL = "jakarta.persistence.jdbc.url";

	/**
	 * Specifies the database user to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JAKARTA_JDBC_DRIVER}, {@link #JAKARTA_JDBC_URL}
	 * and {@link #JAKARTA_JDBC_PASSWORD} to specify how to connect to the database.
	 * <p>
	 * Depending on the configured {@link ConnectionProvider}, the specified username might be used to:
	 * <ul>
	 *     <li>
	 *         create a JDBC connection using
	 *         {@link java.sql.DriverManager#getConnection(String,java.util.Properties)}
	 *         or {@link java.sql.Driver#connect(String,java.util.Properties)}, or
	 *     </li>
	 *     <li>
	 *         obtain a JDBC connection from a datasource, using
	 *         {@link javax.sql.DataSource#getConnection(String, String)}.
	 *     </li>
	 * </ul>
	 * <p>
	 * See section 8.2.1.9
	 */
	String JAKARTA_JDBC_USER = "jakarta.persistence.jdbc.user";

	/**
	 * Specifies the password to use when connecting via JDBC.
	 * <p>
	 * Used in conjunction with {@link #JAKARTA_JDBC_DRIVER}, {@link #JAKARTA_JDBC_URL}
	 * and {@link #JAKARTA_JDBC_USER} to specify how to connect to the database.
	 * <p>
	 * See JPA 2 section 8.2.1.9
	 */
	String JAKARTA_JDBC_PASSWORD = "jakarta.persistence.jdbc.password";

	/**
	 * Allows passing a specific {@link java.sql.Connection} instance to be used by
	 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect}, and for performing
	 * {@link SchemaToolingSettings#JAKARTA_HBM2DDL_DATABASE_ACTION database actions} if requested.
	 * <p>
	 * For {@code Dialect} resolution, {@value #JAKARTA_HBM2DDL_DB_NAME} and, optionally,
	 * {@value JAKARTA_HBM2DDL_DB_VERSION}, {@value #JAKARTA_HBM2DDL_DB_MAJOR_VERSION},
	 * and {@value #JAKARTA_HBM2DDL_DB_MINOR_VERSION} can be used instead
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 * @see #JAKARTA_HBM2DDL_DB_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MAJOR_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MINOR_VERSION
	 */
	String JAKARTA_HBM2DDL_CONNECTION = "jakarta.persistence.schema-generation-connection";

	/**
	 * Specifies the name of the database vendor (as would be reported by
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductName}) for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use.
	 * <p>
	 * For cases when the name of the database vendor is not enough alone, a combination
	 * of {@value JAKARTA_HBM2DDL_DB_VERSION}, {@value #JAKARTA_HBM2DDL_DB_MAJOR_VERSION}
	 * {@value #JAKARTA_HBM2DDL_DB_MINOR_VERSION} can be used instead
	 *
	 * @see #JAKARTA_HBM2DDL_DB_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MAJOR_VERSION
	 * @see #JAKARTA_HBM2DDL_DB_MINOR_VERSION
	 *
	 * @implSpec {@link SchemaToolingSettings#JAKARTA_HBM2DDL_DATABASE_ACTION database actions} are not
	 * available when supplying just the name and versions
	 */
	String JAKARTA_HBM2DDL_DB_NAME = "jakarta.persistence.database-product-name";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseProductVersion()}) for the
	 * underlying database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_VERSION = "jakarta.persistence.database-product-version";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()}) for the underlying
	 * database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_MAJOR_VERSION = "jakarta.persistence.database-major-version";

	/**
	 * Used in conjunction with {@value #JAKARTA_HBM2DDL_DB_NAME} for the purpose of
	 * determining the {@link org.hibernate.dialect.Dialect} to use when the name does
	 * not provide enough detail.
	 * <p>
	 * The value is expected to match what would be returned from
	 * {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()}) for the underlying
	 * database.
	 *
	 * @see #JAKARTA_HBM2DDL_DB_NAME
	 */
	String JAKARTA_HBM2DDL_DB_MINOR_VERSION = "jakarta.persistence.database-minor-version";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies the Hibernate {@linkplain org.hibernate.dialect.Dialect SQL dialect}, either
	 * <ul>
	 *     <li>an instance of {@link org.hibernate.dialect.Dialect},
	 *     <li>a {@link Class} representing a class that extends {@code Dialect}, or
	 *     <li>the name of a class that extends {@code Dialect}.
	 * </ul>
	 * <p>
	 * By default, Hibernate will attempt to automatically determine the dialect from the
	 * {@linkplain #URL JDBC URL} and JDBC metadata, so this setting is not usually necessary.
	 *
	 * @apiNote As of Hibernate 6, this property should not be explicitly specified,
	 *          except when using a custom user-written implementation of {@code Dialect}.
	 *          Instead, applications should allow Hibernate to select the {@code Dialect}
	 *          automatically.
	 *
	 * @see org.hibernate.dialect.Dialect
	 */
	String DIALECT = "hibernate.dialect";

	/**
	 * Specifies additional {@link org.hibernate.engine.jdbc.dialect.spi.DialectResolver}
	 * implementations to register with the standard
	 * {@link org.hibernate.engine.jdbc.dialect.spi.DialectFactory}.
	 */
	String DIALECT_RESOLVERS = "hibernate.dialect_resolvers";

	/**
	 * Specifies a {@link ConnectionProvider}
	 * to use for obtaining JDBC connections, either:
	 * <ul>
	 *     <li>an instance of {@code ConnectionProvider},
	 *     <li>a {@link Class} representing a class that implements
	 *         {@code ConnectionProvider}, or
	 *     <li>the name of a class that implements {@code ConnectionProvider}.
	 * </ul>
	 * <p>
	 * The term {@code "class"} appears in the setting name due to legacy reasons;
	 * however it can accept instances.
	 */
	String CONNECTION_PROVIDER = "hibernate.connection.provider_class";

	/**
	 * Specifies the maximum number of inactive connections for the built-in
	 * {@linkplain org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl
	 * connection pool}.
	 *
	 * @settingDefault 20
	 */
	String POOL_SIZE = "hibernate.connection.pool_size";

	/**
	 * Specified the JDBC transaction isolation level.
	 */
	String ISOLATION = "hibernate.connection.isolation";

	/**
	 * Controls the autocommit mode of JDBC connections obtained from any
	 * {@link ConnectionProvider} implementation
	 * which respects this setting, which the built-in implementations do, except for
	 * {@link org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl}.
	 */
	String AUTOCOMMIT = "hibernate.connection.autocommit";

	/**
	 * Indicates that Connections obtained from the configured {@link ConnectionProvider} have
	 * auto-commit already disabled when they are acquired.
	 * <p>
	 * It is inappropriate to set this value to {@code true} when the Connections returned by
	 * the provider do not, in fact, have auto-commit disabled.  Doing so may lead to Hibernate
	 * executing SQL operations outside the scope of any transaction.
	 *
	 * @apiNote By default, Hibernate calls {@link java.sql.Connection#setAutoCommit(boolean)} on
	 * newly-obtained connections.  This setting allows to circumvent that call (as well as other
	 * operations) in the interest of performance.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyConnectionProviderDisablesAutoCommit(boolean)
	 *
	 * @since 5.2.10
	 */
	String CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT= "hibernate.connection.provider_disables_autocommit";

	/**
	 * A prefix for properties specifying arbitrary JDBC connection properties. These
	 * properties are simply passed along to the provider when creating a connection.
	 * <p>
	 * For example, declaring {@code hibernate.connection.foo=bar} tells Hibernate to
	 * append {@code foo=bar} to the JDBC connection URL.
	 */
	String CONNECTION_PREFIX = "hibernate.connection";

	/**
	 * Specifies a {@link org.hibernate.resource.jdbc.spi.StatementInspector}
	 * implementation associated with the {@link org.hibernate.SessionFactory},
	 * either:
	 * <ul>
	 *     <li>an instance of {@code StatementInspector},
	 *     <li>a {@link Class} representing an class that implements {@code StatementInspector}, or
	 *     <li>the name of a class that implements {@code StatementInspector}.
	 * </ul>
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyStatementInspector(StatementInspector)
	 *
	 * @since 5.0
	 */
	String STATEMENT_INSPECTOR = "hibernate.session_factory.statement_inspector";

	/**
	 * Enables logging of generated SQL to the console.
	 *
	 * @settingDefault {@code false}
	 */
	String SHOW_SQL = "hibernate.show_sql";

	/**
	 * Enables formatting of SQL logged to the console.
	 *
	 * @settingDefault {@code false}
	 */
	String FORMAT_SQL = "hibernate.format_sql";

	/**
	 * Enables highlighting of SQL logged to the console using ANSI escape codes.
	 *
	 * @settingDefault {@code false}
	 */
	String HIGHLIGHT_SQL = "hibernate.highlight_sql";

	/**
	 * Specifies a duration in milliseconds defining the minimum query execution time that
	 * characterizes a "slow" query. Any SQL query which takes longer than this amount of
	 * time to execute will be logged.
	 * <p>
	 * A value of {@code 0}, the default, disables logging of "slow" queries.
	 *
	 * @see org.hibernate.stat.Statistics#getSlowQueries()
	 */
	String LOG_SLOW_QUERY = "hibernate.log_slow_query";

	/**
	 * Specifies that comments should be added to the generated SQL.
	 *
	 * @settingDefault {@code false}
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applySqlComments(boolean)
	 */
	String USE_SQL_COMMENTS = "hibernate.use_sql_comments";

	/**
	 * Gives the JDBC driver a hint as to the number of rows that should be fetched
	 * from the database when more rows are needed. If {@code 0}, the JDBC driver's
	 * default settings will be used.
	 *
	 * @see java.sql.PreparedStatement#setFetchSize(int)
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcFetchSize(int)
	 * @see org.hibernate.ScrollableResults#setFetchSize(int)
	 *
	 * @settingDefault {@code 0}
	 */
	String STATEMENT_FETCH_SIZE = "hibernate.jdbc.fetch_size";

	/**
	 * Controls how Hibernate should handle scrollable results - <ul>
	 * 	 <li>
	 * 	     {@code true} indicates that {@linkplain java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE insensitive} scrolling can be used
	 * 	 </li>
	 * 	 <li>
	 * 	     {@code false} indicates that {@linkplain java.sql.ResultSet#TYPE_SCROLL_SENSITIVE sensitive} scrolling must be used
	 * 	 </li>
	 * </ul>
	 *
	 * @settingDefault {@code true} if the underlying driver supports scrollable results
	 *
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyScrollableResultsSupport(boolean)
	 * @see Query#scroll
	 * @see ExtractedDatabaseMetaData#supportsScrollableResults()
	 */
	String USE_SCROLLABLE_RESULTSET = "hibernate.jdbc.use_scrollable_resultset";

	/**
	 * Controls whether to use JDBC markers (`?`) or dialect native markers for parameters
	 * within {@linkplain java.sql.PreparedStatement preparable} SQL statements.
	 *
	 * @implNote {@code False} by default, indicating standard JDBC parameter markers (`?`)
	 * are used.  Set to {@code true} to use the Dialect's native markers, if any.  For
	 * Dialects without native markers, the standard JDBC strategy is used.
	 *
	 * @see ParameterMarkerStrategy
	 * @see org.hibernate.dialect.Dialect#getNativeParameterMarkerStrategy()
	 *
	 * @since 6.2
	 */
	@Incubating
	String DIALECT_NATIVE_PARAM_MARKERS = "hibernate.dialect.native_param_markers";

	/**
	 * When enabled, specifies that Hibernate should not use contextual LOB creation.
	 *
	 * @see org.hibernate.engine.jdbc.LobCreator
	 * @see org.hibernate.engine.jdbc.LobCreationContext
	 */
	String NON_CONTEXTUAL_LOB_CREATION = "hibernate.jdbc.lob.non_contextual_creation";

	/**
	 * When enabled, specifies that JDBC statement warnings should be logged.
	 * <p>
	 * The default is determined by
	 * {@link org.hibernate.dialect.Dialect#isJdbcLogWarningsEnabledByDefault()}.
	 *
	 * @see java.sql.Statement#getWarnings()
	 *
	 * @since 5.1
	 */
	String LOG_JDBC_WARNINGS = "hibernate.jdbc.log.warnings";

	/**
	 * Specifies the {@linkplain java.util.TimeZone time zone} to use in the JDBC driver,
	 * which is supposed to match the database timezone.
	 * <p>
	 * This is the timezone what will be passed to
	 * {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)}
	 * {@link java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)},
	 * {@link java.sql.ResultSet#getTimestamp(int, Calendar)}, and
	 * {@link java.sql.ResultSet#getTime(int, Calendar)} when binding parameters.
	 * <p>
	 * The time zone may be given as:
	 * <ul>
	 *     <li>an instance of {@link java.util.TimeZone},
	 *     <li>an instance of {@link java.time.ZoneId}, or
	 *     <li>a time zone ID string to be passed to {@link java.time.ZoneId#of(String)}.
	 * </ul>
	 * <p>
	 * By default, the {@linkplain java.util.TimeZone#getDefault() JVM default time zone}
	 * is assumed by the JDBC driver.
	 *
	 * @since 5.2.3
	 */
	String JDBC_TIME_ZONE = "hibernate.jdbc.time_zone";

	/**
	 * Specifies that generated primary keys may be retrieved using the JDBC 3
	 * {@link java.sql.PreparedStatement#getGeneratedKeys()} operation.
	 * <p>
	 * Usually, performance will be improved if this behavior is enabled, assuming
	 * the JDBC driver supports {@code getGeneratedKeys()}.
	 *
	 * @see java.sql.PreparedStatement#getGeneratedKeys
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyGetGeneratedKeysSupport(boolean)
	 */
	String USE_GET_GENERATED_KEYS = "hibernate.jdbc.use_get_generated_keys";

	/**
	 * Specifies how Hibernate should manage JDBC connections in terms of acquisition
	 * and release, either:
	 * <ul>
	 *     <li>an instance of the enumeration
	 *         {@link org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode},
	 *         or
	 *     <li>the name of one of its instances.
	 * </ul>
	 * <p>
	 * The default is {@code DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION}.
	 *
	 * @see org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyConnectionHandlingMode(PhysicalConnectionHandlingMode)
	 *
	 * @since 5.2
	 */
	String CONNECTION_HANDLING = "hibernate.connection.handling_mode";

	/**
	 * Whether access to JDBC {@linkplain java.sql.DatabaseMetaData metadata} is allowed during bootstrap.
	 * <p/>
	 * Typically, Hibernate accesses this metadata to understand the capabilities of the underlying
	 * database to help minimize needed configuration.  Disabling this access means that only explicit
	 * settings are used.  At a minimum, the Dialect to use must be specified using either the {@value #DIALECT}
	 * or {@value JdbcSettings#JAKARTA_HBM2DDL_DB_NAME} setting.  When the Dialect to use is specified in
	 * this manner it is generally a good idea to specify the
	 * {@linkplain JdbcSettings#JAKARTA_HBM2DDL_DB_VERSION database version} as well - Dialects use the version
	 * to configure themselves.
	 *
	 * @apiNote The specified Dialect may also provide defaults into the "explicit" settings.
	 *
	 * @settingDefault {@code true}
	 *
	 * @since 6.5
	 */
	String ALLOW_METADATA_ON_BOOT = "hibernate.boot.allow_jdbc_metadata_access";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecated Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated The JPA-standard setting {@link #JAKARTA_JDBC_DRIVER} is now preferred.
	 */
	@Deprecated
	String DRIVER = "hibernate.connection.driver_class";

	/**
	 * @deprecated The JPA-standard setting {@link #JAKARTA_JDBC_URL} is now preferred.
	 */
	@Deprecated
	String URL = "hibernate.connection.url";

	/**
	 * @see #PASS
	 *
	 * @deprecated The JPA-standard setting {@link #JAKARTA_JDBC_USER} is now preferred.
	 */
	@Deprecated
	String USER = "hibernate.connection.username";

	/**
	 * @see #USER
	 *
	 * @deprecated The JPA-standard setting {@link #JAKARTA_JDBC_USER} is now preferred.
	 */
	@Deprecated
	String PASS = "hibernate.connection.password";

	/**
	 * @see javax.sql.DataSource
	 *
	 * @deprecated The JPA-standard {@link #JAKARTA_JTA_DATASOURCE} or {@link #JAKARTA_JTA_DATASOURCE} setting
	 * is now preferred.
	 */
	@Deprecated
	String DATASOURCE = "hibernate.connection.datasource";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #JAKARTA_JTA_DATASOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JTA_DATASOURCE = "javax.persistence.jtaDataSource";

	/**
	 * @deprecated Use {@link #JAKARTA_NON_JTA_DATASOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_NON_JTA_DATASOURCE = "javax.persistence.nonJtaDataSource";

	/**
	 * @deprecated Use {@link #JAKARTA_JDBC_DRIVER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_DRIVER = "javax.persistence.jdbc.driver";

	/**
	 * @deprecated Use {@link #JAKARTA_JDBC_URL} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_URL = "javax.persistence.jdbc.url";

	/**
	 * @deprecated Use {@link #JAKARTA_JDBC_USER} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_USER = "javax.persistence.jdbc.user";

	/**
	 * @deprecated Use {@link #JAKARTA_JDBC_PASSWORD} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String JPA_JDBC_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_CONNECTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CONNECTION = "javax.persistence.schema-generation-connection";

	/**
	 * @see #DIALECT_DB_VERSION
	 * @see #DIALECT_DB_MAJOR_VERSION
	 * @see #DIALECT_DB_MINOR_VERSION
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_NAME} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_NAME = "javax.persistence.database-product-name";

	/**
	 * @see #DIALECT_DB_NAME
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_VERSION = "javax.persistence.database-product-version";

	/**
	 * @see #DIALECT_DB_NAME
	 * @see #DIALECT_DB_MINOR_VERSION
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_MAJOR_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_MAJOR_VERSION = "javax.persistence.database-major-version";

	/**
	 * @see #DIALECT_DB_NAME
	 * @see #DIALECT_DB_MAJOR_VERSION
	 * @see org.hibernate.engine.jdbc.dialect.spi.DialectResolver
	 *
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DB_MINOR_VERSION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String DIALECT_DB_MINOR_VERSION = "javax.persistence.database-minor-version";
}
