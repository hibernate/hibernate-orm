/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Generates static schema annotation types from JDBC metadata.
 * <p>
 * The Hibernate Gradle plugin registers this task as {@code generateSchemaAnnotations}. The task
 * connects to the configured JDBC database, reads table, column, and imported foreign-key metadata,
 * and writes one Java annotation type per table to the configured package.
 * <p>
 * Basic Groovy DSL usage:
 *
 * <pre>{@code
 * dependencies {
 *     runtimeOnly "com.h2database:h2:<version>"
 * }
 *
 * tasks.named("generateSchemaAnnotations") {
 *     hibernateProperties = "hibernate.properties"
 *     schemaName = "PUBLIC"
 *     tableNamePattern = "%"
 *     packageName = "org.example.schema"
 * }
 * }</pre>
 * <p>
 * The JDBC driver is loaded from the project {@code runtimeClasspath}. Generated sources are written
 * below {@code build/generated/sources/schemaAnnotations} by default, with package directories appended.
 * For example, package {@code org.example.schema} and table {@code BOOK} produce
 * {@code build/generated/sources/schemaAnnotations/org/example/schema/BOOK.java}.
 * <p>
 * The task can read JDBC configuration from a {@code hibernate.properties} file in the main resource
 * set. Direct task properties override values read from {@code hibernate.properties}.
 * <p>
 * For each table, the generated top-level annotation type is meta-annotated with
 * {@code @StaticTable}. For each non-foreign-key column, the generated nested annotation type is
 * meta-annotated with {@code @StaticColumn}. For each foreign-key column, the generated nested
 * annotation type is meta-annotated with {@code @StaticJoinColumn}, including the referenced table
 * and column from the JDBC foreign-key metadata.
 * <p>
 * Table and column names are used as Java annotation type names, so matched table and column names
 * must be legal Java identifiers.
 */
@DisableCachingByDefault(because = "Schema annotation generation performs JDBC operations and is not cacheable")
public abstract class GenerateSchemaAnnotationsTask extends DefaultTask {

	private static final String[] TABLE_TYPES = { "TABLE" };
	private static final String[] JDBC_DRIVER_PROPERTIES = {
			"hibernate.connection.driver_class",
			"jakarta.persistence.jdbc.driver",
			"javax.persistence.jdbc.driver"
	};
	private static final String[] JDBC_URL_PROPERTIES = {
			"hibernate.connection.url",
			"jakarta.persistence.jdbc.url",
			"javax.persistence.jdbc.url"
	};
	private static final String[] JDBC_USERNAME_PROPERTIES = {
			"hibernate.connection.username",
			"jakarta.persistence.jdbc.user",
			"javax.persistence.jdbc.user"
	};
	private static final String[] JDBC_PASSWORD_PROPERTIES = {
			"hibernate.connection.password",
			"jakarta.persistence.jdbc.password",
			"javax.persistence.jdbc.password"
	};
	private static final String[] DEFAULT_CATALOG_PROPERTIES = { "hibernate.default_catalog" };
	private static final String[] DEFAULT_SCHEMA_PROPERTIES = { "hibernate.default_schema" };
	private static final Set<String> JAVA_KEYWORDS = Set.of(
			"abstract",
			"assert",
			"boolean",
			"break",
			"byte",
			"case",
			"catch",
			"char",
			"class",
			"const",
			"continue",
			"default",
			"do",
			"double",
			"else",
			"enum",
			"extends",
			"final",
			"finally",
			"float",
			"for",
			"goto",
			"if",
			"implements",
			"import",
			"instanceof",
			"int",
			"interface",
			"long",
			"native",
			"new",
			"package",
			"private",
			"protected",
			"public",
			"return",
			"short",
			"static",
			"strictfp",
			"super",
			"switch",
			"synchronized",
			"this",
			"throw",
			"throws",
			"transient",
			"try",
			"void",
			"volatile",
			"while",
			"_",
			"var",
			"yield",
			"record",
			"sealed",
			"permits"
	);

	public GenerateSchemaAnnotationsTask() {
		getTableNamePattern().convention( "%" );
		getOutputDirectory().convention(
				getProject().getLayout().getBuildDirectory().dir( "generated/sources/schemaAnnotations" )
		);
		getOutputs().upToDateWhen( task -> false );
	}

	/**
	 * The fully-qualified JDBC driver class name, for example {@code org.h2.Driver}.
	 * <p>
	 * The driver must be available from the project's {@code runtimeClasspath}. If not set,
	 * the task reads {@code hibernate.connection.driver_class}, {@code jakarta.persistence.jdbc.driver},
	 * or {@code javax.persistence.jdbc.driver} from the configured Hibernate properties file.
	 */
	@Input
	@Optional
	abstract public Property<String> getJdbcDriver();

	/**
	 * The JDBC connection URL used to read database metadata.
	 * <p>
	 * If not set, the task reads {@code hibernate.connection.url}, {@code jakarta.persistence.jdbc.url},
	 * or {@code javax.persistence.jdbc.url} from the configured Hibernate properties file.
	 */
	@Input
	@Optional
	abstract public Property<String> getJdbcUrl();

	/**
	 * The optional JDBC user name.
	 * <p>
	 * If not set, the task reads {@code hibernate.connection.username}, {@code jakarta.persistence.jdbc.user},
	 * or {@code javax.persistence.jdbc.user} from the configured Hibernate properties file.
	 */
	@Input
	@Optional
	abstract public Property<String> getUsername();

	/**
	 * The optional JDBC password.
	 * <p>
	 * If not set, the task reads {@code hibernate.connection.password}, {@code jakarta.persistence.jdbc.password},
	 * or {@code javax.persistence.jdbc.password} from the configured Hibernate properties file.
	 */
	@Internal
	abstract public Property<String> getPassword();

	/**
	 * The Hibernate properties file to read from the main resource set.
	 * <p>
	 * Defaults to {@code hibernate.properties}. The task uses the file as defaults for JDBC driver,
	 * URL, user name, password, catalog, and schema. Direct task properties take precedence.
	 */
	@Input
	@Optional
	abstract public Property<String> getHibernateProperties();

	/**
	 * The Java package for generated annotation types, for example {@code org.example.schema}.
	 */
	@Input
	abstract public Property<String> getPackageName();

	/**
	 * The optional catalog name passed to JDBC metadata lookup.
	 * <p>
	 * If not specified, the task reads {@code hibernate.default_catalog} from the configured
	 * Hibernate properties file. If that value is also not specified, the task uses
	 * {@link Connection#getCatalog()} when available.
	 */
	@Input
	@Optional
	abstract public Property<String> getCatalogName();

	/**
	 * The optional schema name passed to JDBC metadata lookup.
	 * <p>
	 * If not specified, the task reads {@code hibernate.default_schema} from the configured
	 * Hibernate properties file. If that value is also not specified, the task uses
	 * {@link Connection#getSchema()} when available.
	 */
	@Input
	@Optional
	abstract public Property<String> getSchemaName();

	/**
	 * The table-name pattern passed to {@link DatabaseMetaData#getTables(String, String, String, String[])}.
	 * <p>
	 * Defaults to {@code %}.
	 */
	@Input
	@Optional
	abstract public Property<String> getTableNamePattern();

	/**
	 * The root output directory for generated sources.
	 * <p>
	 * Defaults to {@code build/generated/sources/schemaAnnotations}.
	 */
	@OutputDirectory
	abstract public DirectoryProperty getOutputDirectory();

	@TaskAction
	public void generateSchemaAnnotations() {
		final var configuration = resolveTaskConfiguration();
		validatePackageName( configuration.packageName );

		getLogger().lifecycle( "Starting schema annotation generation" );
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader classLoader = null;
		Driver registeredDriver = null;
		try {
			classLoader = new URLClassLoader( resolveProjectClassPath(), oldLoader );
			Thread.currentThread().setContextClassLoader( classLoader );
			registeredDriver = registerDriver( classLoader, configuration.jdbcDriver );
			generateSchemaAnnotations( configuration );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to generate schema annotations", e );
		}
		finally {
			deregisterDriver( registeredDriver );
			closeClassLoader( classLoader );
			Thread.currentThread().setContextClassLoader( oldLoader );
			getLogger().lifecycle( "Finished schema annotation generation" );
		}
	}

	private TaskConfiguration resolveTaskConfiguration() {
		final Properties hibernateProperties = loadHibernateProperties();
		return new TaskConfiguration(
				requiredConfiguration(
						getJdbcDriver(),
						hibernateProperties,
						"jdbcDriver",
						JDBC_DRIVER_PROPERTIES
				),
				requiredConfiguration( getJdbcUrl(), hibernateProperties, "jdbcUrl", JDBC_URL_PROPERTIES ),
				optionalConfiguration( getUsername(), hibernateProperties, JDBC_USERNAME_PROPERTIES ),
				optionalConfiguration( getPassword(), hibernateProperties, JDBC_PASSWORD_PROPERTIES ),
				getPackageName().get(),
				optionalNonBlankConfiguration( getCatalogName(), hibernateProperties, DEFAULT_CATALOG_PROPERTIES ),
				optionalNonBlankConfiguration( getSchemaName(), hibernateProperties, DEFAULT_SCHEMA_PROPERTIES ),
				getTableNamePattern().get()
		);
	}

	private Properties loadHibernateProperties() {
		final boolean explicitPropertiesFile = getHibernateProperties().isPresent();
		final String filename = hibernatePropertiesFilename();
		if ( filename == null || filename.isBlank() ) {
			return new Properties();
		}

		final File propertiesFile = RevengFileHelper.findResourceFile( getProject(), filename );
		if ( propertiesFile != null ) {
			return RevengFileHelper.loadPropertiesFile( getLogger(), propertiesFile );
		}
		if ( explicitPropertiesFile ) {
			throw new GradleException( "Hibernate properties file `" + filename + "` could not be found" );
		}
		return new Properties();
	}

	private String hibernatePropertiesFilename() {
		return getHibernateProperties().getOrElse( RevengSpec.DEFAULT_HIBERNATE_PROPERTIES );
	}

	private String requiredConfiguration(
			Property<String> taskProperty,
			Properties hibernateProperties,
			String propertyName,
			String... hibernatePropertyNames) {
		final String result = optionalNonBlankConfiguration( taskProperty, hibernateProperties, hibernatePropertyNames );
		if ( result != null ) {
			return result;
		}

		throw new GradleException(
				"Schema annotation generation requires `" + propertyName
						+ "` or one of the following properties in `" + hibernatePropertiesFilename()
						+ "`: " + String.join( ", ", hibernatePropertyNames )
		);
	}

	private String optionalNonBlankConfiguration(
			Property<String> taskProperty,
			Properties hibernateProperties,
			String... hibernatePropertyNames) {
		final String result = optionalConfiguration( taskProperty, hibernateProperties, hibernatePropertyNames );
		return result == null || result.isBlank() ? null : result;
	}

	private String optionalConfiguration(
			Property<String> taskProperty,
			Properties hibernateProperties,
			String... hibernatePropertyNames) {
		if ( taskProperty.isPresent() ) {
			return taskProperty.get();
		}
		for ( String propertyName : hibernatePropertyNames ) {
			if ( hibernateProperties.containsKey( propertyName ) ) {
				return hibernateProperties.getProperty( propertyName );
			}
		}
		return null;
	}

	private void generateSchemaAnnotations(TaskConfiguration configuration) throws SQLException, IOException {
		getLogger().lifecycle( "Connecting to database: " + configuration.jdbcUrl );
		try ( Connection connection = createConnection( configuration ) ) {
			final List<Table> tables = readTables( connection, configuration );
			writeTables( configuration.packageName, tables );
		}
	}

	private Connection createConnection(TaskConfiguration configuration) throws SQLException {
		if ( configuration.username == null && configuration.password == null ) {
			return DriverManager.getConnection( configuration.jdbcUrl );
		}

		final Properties properties = new Properties();
		if ( configuration.username != null ) {
			properties.put( "user", configuration.username );
		}
		if ( configuration.password != null ) {
			properties.put( "password", configuration.password );
		}
		return DriverManager.getConnection( configuration.jdbcUrl, properties );
	}

	private List<Table> readTables(Connection connection, TaskConfiguration configuration) throws SQLException {
		final DatabaseMetaData metadata = connection.getMetaData();
		final String catalog = configuration.catalogName == null ? determineCatalog( connection ) : configuration.catalogName;
		final String schema = configuration.schemaName == null ? determineSchema( connection ) : configuration.schemaName;
		final String tableNamePattern = configuration.tableNamePattern;

		final List<Table> tables = new ArrayList<>();
		try ( ResultSet resultSet = metadata.getTables( catalog, schema, tableNamePattern, TABLE_TYPES ) ) {
			while ( resultSet.next() ) {
				final String tableName = resultSet.getString( "TABLE_NAME" );
				validateJavaIdentifier( tableName, "table" );
				tables.add(
						new Table(
								resultSet.getString( "TABLE_CAT" ),
								resultSet.getString( "TABLE_SCHEM" ),
								tableName
						)
				);
			}
		}

		tables.sort( Comparator.comparing( table -> table.name.toLowerCase( Locale.ROOT ) ) );
		validateNoDuplicateTableNames( tables );
		for ( Table table : tables ) {
			readColumns( metadata, table );
		}
		return tables;
	}

	private String determineCatalog(Connection connection) {
		try {
			return connection.getCatalog();
		}
		catch (SQLException | AbstractMethodError e) {
			return null;
		}
	}

	private String determineSchema(Connection connection) {
		try {
			return connection.getSchema();
		}
		catch (SQLException | AbstractMethodError e) {
			return null;
		}
	}

	private void readColumns(DatabaseMetaData metadata, Table table) throws SQLException {
		final Set<String> columnNames = new HashSet<>();
		final Map<String, ForeignKeyColumn> foreignKeyColumns = readForeignKeyColumns( metadata, table );
		try ( ResultSet resultSet = metadata.getColumns( table.catalog, table.schema, table.name, "%" ) ) {
			while ( resultSet.next() ) {
				final String columnName = resultSet.getString( "COLUMN_NAME" );
				validateJavaIdentifier( columnName, "column" );
				if ( !columnNames.add( columnName ) ) {
					throw new GradleException(
							"Table `" + table.name + "` has multiple columns named `" + columnName + "`"
					);
				}
				final JDBCType jdbcType = resolveJdbcType( table.name, columnName, resultSet.getInt( "DATA_TYPE" ) );
				table.columns.add(
						new Column(
								columnName,
								jdbcType,
								isNullable( resultSet ),
								length( resultSet, jdbcType ),
								precision( resultSet, jdbcType ),
								scale( resultSet, jdbcType ),
								foreignKeyColumns.get( columnName ),
								resultSet.getInt( "ORDINAL_POSITION" )
						)
				);
			}
		}
		table.columns.sort( Comparator.comparingInt( column -> column.position ) );
	}

	private Map<String, ForeignKeyColumn> readForeignKeyColumns(DatabaseMetaData metadata, Table table)
			throws SQLException {
		final Map<String, ForeignKeyColumn> foreignKeyColumns = new HashMap<>();
		try ( ResultSet resultSet = metadata.getImportedKeys( table.catalog, table.schema, table.name ) ) {
			while ( resultSet.next() ) {
				final String columnName = resultSet.getString( "FKCOLUMN_NAME" );
				final var foreignKeyColumn = new ForeignKeyColumn(
						resultSet.getString( "PKTABLE_NAME" ),
						resultSet.getString( "PKCOLUMN_NAME" )
				);
				final var previous = foreignKeyColumns.putIfAbsent( columnName, foreignKeyColumn );
				if ( previous != null && !previous.equals( foreignKeyColumn ) ) {
					throw new GradleException(
							"Column `" + table.name + "." + columnName
									+ "` is part of multiple foreign keys with different referenced columns"
					);
				}
			}
		}
		return foreignKeyColumns;
	}

	private boolean isNullable(ResultSet resultSet) throws SQLException {
		return resultSet.getInt( "NULLABLE" ) != DatabaseMetaData.columnNoNulls;
	}

	private int length(ResultSet resultSet, JDBCType type) throws SQLException {
		return isLengthType( type ) ? getInt( resultSet, "COLUMN_SIZE" ) : 255;
	}

	private int precision(ResultSet resultSet, JDBCType type) throws SQLException {
		return isNumericType( type ) ? getInt( resultSet, "COLUMN_SIZE" ) : 0;
	}

	private int scale(ResultSet resultSet, JDBCType type) throws SQLException {
		return isNumericType( type ) ? getInt( resultSet, "DECIMAL_DIGITS" ) : 0;
	}

	private int getInt(ResultSet resultSet, String columnName) throws SQLException {
		final int result = resultSet.getInt( columnName );
		return resultSet.wasNull() ? 0 : result;
	}

	private boolean isLengthType(JDBCType type) {
		return switch ( type ) {
			case CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR,
					BINARY, VARBINARY, LONGVARBINARY -> true;
			default -> false;
		};
	}

	private boolean isNumericType(JDBCType type) {
		return switch ( type ) {
			case BIT, TINYINT, SMALLINT, INTEGER, BIGINT, FLOAT, REAL, DOUBLE, NUMERIC, DECIMAL -> true;
			default -> false;
		};
	}

	private JDBCType resolveJdbcType(String tableName, String columnName, int typeCode) {
		try {
			return JDBCType.valueOf( typeCode );
		}
		catch (IllegalArgumentException e) {
			throw new GradleException(
					"Column `" + tableName + "." + columnName + "` uses JDBC type code `"
							+ typeCode + "`, which is not defined by java.sql.JDBCType",
					e
			);
		}
	}

	private void writeTables(String packageName, List<Table> tables) throws IOException {
		final File outputDirectory = getOutputDirectory().get().getAsFile();
		final Path packageDirectory = packageDirectory( outputDirectory.toPath(), packageName );
		Files.createDirectories( packageDirectory );
		for ( Table table : tables ) {
			final Path outputFile = packageDirectory.resolve( table.name + ".java" );
			Files.writeString( outputFile, renderTable( packageName, table ), StandardCharsets.UTF_8 );
		}
		getLogger().lifecycle( "Generated " + tables.size() + " schema annotation type(s) into " + packageDirectory );
	}

	private Path packageDirectory(Path outputDirectory, String packageName) {
		Path result = outputDirectory;
		for ( String namePart : packageName.split( "\\." ) ) {
			result = result.resolve( namePart );
		}
		return result;
	}

	private String renderTable(String packageName, Table table) {
		final StringBuilder result = new StringBuilder();
		result.append( "package " ).append( packageName ).append( ";" ).append( System.lineSeparator() )
				.append( System.lineSeparator() )
				.append( "import java.lang.annotation.Retention;" ).append( System.lineSeparator() )
				.append( "import java.sql.JDBCType;" ).append( System.lineSeparator() )
				.append( System.lineSeparator() )
				.append( "import org.hibernate.annotations.schema.StaticColumn;" ).append( System.lineSeparator() )
				.append( "import org.hibernate.annotations.schema.StaticJoinColumn;" ).append( System.lineSeparator() )
				.append( "import org.hibernate.annotations.schema.StaticTable;" ).append( System.lineSeparator() )
				.append( System.lineSeparator() )
				.append( "import static java.lang.annotation.RetentionPolicy.RUNTIME;" ).append( System.lineSeparator() )
				.append( System.lineSeparator() )
				.append( "@Retention(RUNTIME)" ).append( System.lineSeparator() )
				.append( "@StaticTable(name = " ).append( javaStringLiteral( table.name ) ).append( ")" )
				.append( System.lineSeparator() )
				.append( "public @interface " ).append( table.name ).append( " {" ).append( System.lineSeparator() );

		for ( Column column : table.columns ) {
			result.append( System.lineSeparator() )
					.append( "\t@Retention(RUNTIME)" ).append( System.lineSeparator() )
					.append( renderColumnAnnotation( column ) )
					.append( System.lineSeparator() )
					.append( "\tpublic @interface " ).append( column.name ).append( " {" )
					.append( System.lineSeparator() )
					.append( "\t}" ).append( System.lineSeparator() );
		}

		result.append( "}" ).append( System.lineSeparator() );
		return result.toString();
	}

	private String renderColumnAnnotation(Column column) {
		if ( column.foreignKeyColumn != null ) {
			return new StringBuilder()
					.append( "\t@StaticJoinColumn(name = " ).append( javaStringLiteral( column.name ) )
					.append( ", referencedTableName = " )
					.append( javaStringLiteral( column.foreignKeyColumn.referencedTableName ) )
					.append( ", referencedColumnName = " )
					.append( javaStringLiteral( column.foreignKeyColumn.referencedColumnName ) )
					.append( ", type = JDBCType." ).append( column.type.name() )
					.append( ", nullable = " ).append( column.nullable )
					.append( ")" )
					.toString();
		}
		else {
			return new StringBuilder()
					.append( "\t@StaticColumn(name = " ).append( javaStringLiteral( column.name ) )
					.append( ", type = JDBCType." ).append( column.type.name() )
					.append( ", nullable = " ).append( column.nullable )
					.append( ", length = " ).append( column.length )
					.append( ", precision = " ).append( column.precision )
					.append( ", scale = " ).append( column.scale )
					.append( ")" )
					.toString();
		}
	}

	private String javaStringLiteral(String value) {
		final StringBuilder result = new StringBuilder( "\"" );
		for ( int i = 0; i < value.length(); i++ ) {
			final char character = value.charAt( i );
			switch ( character ) {
				case '\b':
					result.append( "\\b" );
					break;
				case '\t':
					result.append( "\\t" );
					break;
				case '\n':
					result.append( "\\n" );
					break;
				case '\f':
					result.append( "\\f" );
					break;
				case '\r':
					result.append( "\\r" );
					break;
				case '"':
					result.append( "\\\"" );
					break;
				case '\\':
					result.append( "\\\\" );
					break;
				default:
					if ( character < ' ' ) {
						appendUnicodeEscape( result, character );
					}
					else {
						result.append( character );
					}
			}
		}
		return result.append( '"' ).toString();
	}

	private void appendUnicodeEscape(StringBuilder result, char character) {
		result.append( "\\u" );
		final String hex = Integer.toHexString( character );
		for ( int i = hex.length(); i < 4; i++ ) {
			result.append( '0' );
		}
		result.append( hex );
	}

	private void validatePackageName(String packageName) {
		if ( packageName == null || packageName.isBlank() ) {
			throw new GradleException( "A package name must be specified" );
		}
		for ( String namePart : packageName.split( "\\.", -1 ) ) {
			validateJavaIdentifier( namePart, "package name part" );
		}
	}

	private void validateNoDuplicateTableNames(List<Table> tables) {
		final Set<String> tableNames = new HashSet<>();
		for ( Table table : tables ) {
			if ( !tableNames.add( table.name ) ) {
				throw new GradleException(
						"Multiple tables named `" + table.name + "` match the configured catalog and schema"
				);
			}
		}
	}

	private void validateJavaIdentifier(String identifier, String role) {
		if ( identifier == null || identifier.isEmpty() ) {
			throw new GradleException( "A " + role + " name must not be empty" );
		}
		if ( JAVA_KEYWORDS.contains( identifier ) ) {
			throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
		}
		if ( !Character.isJavaIdentifierStart( identifier.charAt( 0 ) ) ) {
			throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
		}
		for ( int i = 1; i < identifier.length(); i++ ) {
			if ( !Character.isJavaIdentifierPart( identifier.charAt( i ) ) ) {
				throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
			}
		}
	}

	URL[] resolveProjectClassPath() {
		try {
			final ConfigurationContainer configurations = getProject().getConfigurations();
			final Configuration runtimeClasspath = configurations.getByName( "runtimeClasspath" );
			final ResolvedConfiguration resolvedConfiguration = runtimeClasspath.getResolvedConfiguration();
			final Set<ResolvedArtifact> artifacts = resolvedConfiguration.getResolvedArtifacts();
			final URL[] urls = new URL[artifacts.size()];
			int index = 0;
			for ( ResolvedArtifact artifact : artifacts ) {
				urls[index++] = artifact.getFile().toURI().toURL();
			}
			return urls;
		}
		catch (MalformedURLException e) {
			getLogger().error( "MalformedURLException while resolving project runtime classpath" );
			throw new BuildException( e );
		}
	}

	private Driver registerDriver(ClassLoader classLoader, String driverClassName) {
		getLogger().lifecycle( "Registering the database driver: " + driverClassName );
		try {
			final Class<?> driverClass = classLoader.loadClass( driverClassName );
			final Constructor<?> constructor = driverClass.getDeclaredConstructor();
			final Driver driver = createDelegatingDriver( (Driver) constructor.newInstance() );
			DriverManager.registerDriver( driver );
			return driver;
		}
		catch (Exception e) {
			getLogger().error( "Exception while registering the database driver: " + e.getMessage() );
			throw new RuntimeException( e );
		}
	}

	private Driver createDelegatingDriver(Driver driver) {
		return (Driver) Proxy.newProxyInstance(
				DriverManager.class.getClassLoader(),
				new Class[] { Driver.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return method.invoke( driver, args );
					}
				}
		);
	}

	private void deregisterDriver(Driver driver) {
		if ( driver == null ) {
			return;
		}
		try {
			DriverManager.deregisterDriver( driver );
		}
		catch (SQLException e) {
			getLogger().warn( "Unable to deregister JDBC driver", e );
		}
	}

	private void closeClassLoader(URLClassLoader classLoader) {
		if ( classLoader == null ) {
			return;
		}
		try {
			classLoader.close();
		}
		catch (IOException e) {
			getLogger().warn( "Unable to close JDBC classloader", e );
		}
	}

	private record TaskConfiguration(
			String jdbcDriver,
			String jdbcUrl,
			String username,
			String password,
			String packageName,
			String catalogName,
			String schemaName,
			String tableNamePattern) {
	}

	private static final class Table {
		private final String catalog;
		private final String schema;
		private final String name;
		private final List<Column> columns = new ArrayList<>();

		private Table(String catalog, String schema, String name) {
			this.catalog = catalog;
			this.schema = schema;
			this.name = name;
		}
	}

	private static final class Column {
		private final String name;
		private final JDBCType type;
		private final boolean nullable;
		private final int length;
		private final int precision;
		private final int scale;
		private final ForeignKeyColumn foreignKeyColumn;
		private final int position;

		private Column(
				String name,
				JDBCType type,
				boolean nullable,
				int length,
				int precision,
				int scale,
				ForeignKeyColumn foreignKeyColumn,
				int position) {
			this.name = name;
			this.type = type;
			this.nullable = nullable;
			this.length = length;
			this.precision = precision;
			this.scale = scale;
			this.foreignKeyColumn = foreignKeyColumn;
			this.position = position;
		}
	}

	private record ForeignKeyColumn(String referencedTableName, String referencedColumnName) {
	}
}
