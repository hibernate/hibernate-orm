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
 * Generates table and column annotation types from JDBC metadata.
 */
@DisableCachingByDefault(because = "Schema annotation generation performs JDBC operations and is not cacheable")
public abstract class GenerateSchemaAnnotationsTask extends DefaultTask {

	private static final String[] TABLE_TYPES = { "TABLE" };
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

	@Input
	abstract public Property<String> getJdbcDriver();

	@Input
	abstract public Property<String> getJdbcUrl();

	@Input
	@Optional
	abstract public Property<String> getUsername();

	@Internal
	abstract public Property<String> getPassword();

	@Input
	abstract public Property<String> getPackageName();

	@Input
	@Optional
	abstract public Property<String> getCatalogName();

	@Input
	@Optional
	abstract public Property<String> getSchemaName();

	@Input
	@Optional
	abstract public Property<String> getTableNamePattern();

	@OutputDirectory
	abstract public DirectoryProperty getOutputDirectory();

	@TaskAction
	public void generateSchemaAnnotations() {
		final String packageName = getPackageName().get();
		validatePackageName( packageName );

		getLogger().lifecycle( "Starting schema annotation generation" );
		final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		URLClassLoader classLoader = null;
		Driver registeredDriver = null;
		try {
			classLoader = new URLClassLoader( resolveProjectClassPath(), oldLoader );
			Thread.currentThread().setContextClassLoader( classLoader );
			registeredDriver = registerDriver( classLoader );
			generateSchemaAnnotations( packageName );
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

	private void generateSchemaAnnotations(String packageName) throws SQLException, IOException {
		final String jdbcUrl = getJdbcUrl().get();
		getLogger().lifecycle( "Connecting to database: " + jdbcUrl );
		try ( Connection connection = createConnection( jdbcUrl ) ) {
			final List<Table> tables = readTables( connection );
			writeTables( packageName, tables );
		}
	}

	private Connection createConnection(String jdbcUrl) throws SQLException {
		if ( !getUsername().isPresent() && !getPassword().isPresent() ) {
			return DriverManager.getConnection( jdbcUrl );
		}

		final Properties properties = new Properties();
		if ( getUsername().isPresent() ) {
			properties.put( "user", getUsername().get() );
		}
		if ( getPassword().isPresent() ) {
			properties.put( "password", getPassword().get() );
		}
		return DriverManager.getConnection( jdbcUrl, properties );
	}

	private List<Table> readTables(Connection connection) throws SQLException {
		final DatabaseMetaData metadata = connection.getMetaData();
		final String catalog = getCatalogName().isPresent() ? getCatalogName().get() : determineCatalog( connection );
		final String schema = getSchemaName().isPresent() ? getSchemaName().get() : determineSchema( connection );
		final String tableNamePattern = getTableNamePattern().get();

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

	private Driver registerDriver(ClassLoader classLoader) {
		final String driverClassName = getJdbcDriver().get();
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
