/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import org.apache.tools.ant.BuildException;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.reveng.api.core.RevengSettings;
import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.RevengStrategyFactory;
import org.hibernate.tool.reveng.api.core.TableIdentifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparingInt;
import static org.hibernate.orm.tooling.gradle.reveng.RevengFileHelper.findRequiredResourceFile;
import static org.hibernate.orm.tooling.gradle.reveng.RevengFileHelper.loadPropertiesFile;

/**
 * Generates static schema annotation types from JDBC metadata.
 * <p>
 * The Hibernate Gradle plugin registers this task as {@code generateSchemaAnnotations}. The task
 * connects to the configured JDBC database, reads table, column, and imported foreign key metadata,
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
 *     revengFile = "hibernate.reveng.xml"
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
 * A Hibernate Tools reverse-engineering file can be used for schema selection, table filters,
 * table exclusions, column exclusions, and user-defined foreign keys.
 * <ul>
 * <li>For each table, the generated top-level annotation type is meta-annotated with {@code @TableMapping}
 *     holding a {@code @Table}.
 * <li>For each non-foreign key column, the generated nested annotation type is meta-annotated with
 *     {@code @ColumnMapping} holding a {@code @Column}.
 * <li>For each foreign key column, the generated nested annotation type is meta-annotated with
 *     {@code @JoinColumnMapping} holding a {@code @JoinColumn}.
 * </ul>
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
	 * The optional Hibernate Tools reverse-engineering XML file to read from the main resource set.
	 * <p>
	 * The task supports schema selection, table filters, table exclusions, column exclusions, and
	 * user-defined foreign keys from this file. Annotation type names are still derived from physical
	 * table and column names.
	 */
	@Input
	@Optional
	abstract public Property<String> getRevengFile();

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
		final var hibernateProperties = loadHibernateProperties();
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
				getTableNamePattern().get(),
				optionalTaskConfiguration( getRevengFile() )
		);
	}

	private Properties loadHibernateProperties() {
		final boolean explicitPropertiesFile = getHibernateProperties().isPresent();
		final String filename = hibernatePropertiesFilename();
		if ( filename.isBlank() ) {
			return new Properties();
		}

		final var propertiesFile = RevengFileHelper.findResourceFile( getProject(), filename );
		if ( propertiesFile != null ) {
			return loadPropertiesFile( getLogger(), propertiesFile );
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
		else {
			for ( String propertyName : hibernatePropertyNames ) {
				if ( hibernateProperties.containsKey( propertyName ) ) {
					return hibernateProperties.getProperty( propertyName );
				}
			}
			return null;
		}
	}

	private String optionalTaskConfiguration(Property<String> taskProperty) {
		return !taskProperty.isPresent() || taskProperty.get().isBlank() ? null : taskProperty.get();
	}

	private void generateSchemaAnnotations(TaskConfiguration configuration) throws SQLException, IOException {
		getLogger().lifecycle( "Connecting to database: " + configuration.jdbcUrl );
		final var revengStrategy = createReverseEngineeringStrategy( configuration );
		try ( var connection = createConnection( configuration ) ) {
			final var tables = readTables( connection, configuration, revengStrategy );
			writeTables( configuration.packageName, tables );
		}
		finally {
			if ( revengStrategy != null ) {
				revengStrategy.close();
			}
		}
	}

	private RevengStrategy createReverseEngineeringStrategy(TaskConfiguration configuration) {
		if ( configuration.revengFile == null ) {
			return null;
		}
		else {
			final var revengFile = findRequiredResourceFile( getProject(), configuration.revengFile );
			final var strategy = RevengStrategyFactory.createReverseEngineeringStrategy(
					null,
					new File[] {revengFile}
			);
			final var settings = new RevengSettings( strategy );
			settings.setDefaultPackageName( configuration.packageName );
			strategy.setSettings( settings );
			return strategy;
		}
	}

	private Connection createConnection(TaskConfiguration configuration) throws SQLException {
		if ( configuration.username == null && configuration.password == null ) {
			return DriverManager.getConnection( configuration.jdbcUrl );
		}
		else {
			final var properties = new Properties();
			if ( configuration.username != null ) {
				properties.put( "user", configuration.username );
			}
			if ( configuration.password != null ) {
				properties.put( "password", configuration.password );
			}
			return DriverManager.getConnection( configuration.jdbcUrl, properties );
		}
	}

	private List<Table> readTables(
			Connection connection,
			TaskConfiguration configuration,
			RevengStrategy revengStrategy) throws SQLException {
		final var metadata = connection.getMetaData();
		final String catalog = configuration.catalogName == null ? determineCatalog( connection ) : configuration.catalogName;
		final String schema = configuration.schemaName == null ? determineSchema( connection ) : configuration.schemaName;
		final String tableNamePattern = configuration.tableNamePattern;

		final List<Table> tables = new ArrayList<>();
		final var schemaSelections = revengStrategy == null ? null : revengStrategy.getSchemaSelections();
		if ( schemaSelections == null ) {
			readTables( metadata, catalog, schema, tableNamePattern, revengStrategy, tables );
		}
		else {
			for ( var schemaSelection : schemaSelections ) {
				readTables(
						metadata,
						toJdbcPattern( schemaSelection.getMatchCatalog() ),
						toJdbcPattern( schemaSelection.getMatchSchema() ),
						toJdbcPattern( schemaSelection.getMatchTable() ),
						revengStrategy,
						tables
				);
			}
		}

		tables.sort( Comparator.comparing( table -> table.name.toLowerCase( Locale.ROOT ) ) );
		validateNoDuplicateTableNames( tables );
		final var userForeignKeyColumns = readUserForeignKeyColumns( tables, revengStrategy );
		for ( var table : tables ) {
			readColumns( metadata, table, revengStrategy, userForeignKeyColumns.get( table.identifier() ) );
		}
		return tables;
	}

	private void readTables(
			DatabaseMetaData metadata,
			String catalog,
			String schema,
			String tableNamePattern,
			RevengStrategy revengStrategy,
			List<Table> tables) throws SQLException {
		try ( var resultSet = metadata.getTables( catalog, schema, tableNamePattern, TABLE_TYPES ) ) {
			while ( resultSet.next() ) {
				final String tableName = resultSet.getString( "TABLE_NAME" );
				final var table = new Table(
						resultSet.getString( "TABLE_CAT" ),
						resultSet.getString( "TABLE_SCHEM" ),
						tableName
				);
				if ( !isExcludedTable( revengStrategy, table ) ) {
					validateJavaIdentifier( javaAnnotationName( tableName ), "table" );
					if ( !tables.contains( table ) ) {
						tables.add( table );
					}
				}
			}
		}
	}

	private String toJdbcPattern(String value) {
		return value == null ? null : value.replace( ".*", "%" );
	}

	private boolean isExcludedTable(RevengStrategy revengStrategy, Table table) {
		if ( revengStrategy == null ) {
			return false;
		}
		else {
			for ( var identifier : table.identifiers() ) {
				if ( revengStrategy.excludeTable( identifier ) ) {
					return true;
				}
			}
			return false;
		}
	}

	private Map<TableIdentifier, Map<String, ForeignKeyColumn>> readUserForeignKeyColumns(
			List<Table> tables,
			RevengStrategy revengStrategy) {
		final Map<TableIdentifier, Map<String, ForeignKeyColumn>> result = new HashMap<>();
		if ( revengStrategy != null ) {
			for ( var referencedTable : tables ) {
				for ( var identifier : referencedTable.identifiers() ) {
					final var foreignKeys = revengStrategy.getForeignKeys( identifier );
					if ( foreignKeys != null ) {
						for ( var foreignKey : foreignKeys ) {
							addUserForeignKey( tables, result, foreignKey );
						}
					}
				}
			}
		}
		return result;
	}

	private void addUserForeignKey(
			List<Table> tables,
			Map<TableIdentifier, Map<String, ForeignKeyColumn>> userForeignKeyColumns,
			ForeignKey foreignKey) {
		final var dependentTable = findTable( tables, TableIdentifier.create( foreignKey.getTable() ) );
		if ( dependentTable != null ) {
			final String referencedTableName = foreignKey.getReferencedTable().getName();
			final var columns = foreignKey.getColumns();
			final var referencedColumns = foreignKey.getReferencedColumns();
			if ( columns.size() != referencedColumns.size() ) {
				throw new GradleException(
						"Foreign key `" + foreignKey.getName() + "` in reverse-engineering file has "
						+ columns.size() + " local column(s) and " + referencedColumns.size()
						+ " referenced column(s)"
				);
			}

			final var tableForeignKeyColumns =
					userForeignKeyColumns.computeIfAbsent( dependentTable.identifier(), key -> new HashMap<>() );
			for ( int i = 0; i < columns.size(); i++ ) {
				final var column = columns.get( i );
				final var referencedColumn = referencedColumns.get( i );
				putForeignKeyColumn(
						dependentTable.name,
						tableForeignKeyColumns,
						column.getName(),
						new ForeignKeyColumn( referencedTableName, referencedColumn.getName() )
				);
			}
		}
	}

	private Table findTable(List<Table> tables, TableIdentifier identifier) {
		for ( var table : tables ) {
			if ( table.matches( identifier ) ) {
				return table;
			}
		}
		return null;
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

	private void readColumns(
			DatabaseMetaData metadata,
			Table table,
			RevengStrategy revengStrategy,
			Map<String, ForeignKeyColumn> userForeignKeyColumns) throws SQLException {
		final Set<String> columnNames = new HashSet<>();
		final Set<String> columnAnnotationNames = new HashSet<>();
		final var foreignKeyColumns = readForeignKeyColumns( metadata, table );
		if ( userForeignKeyColumns != null ) {
			for ( var entry : userForeignKeyColumns.entrySet() ) {
				putForeignKeyColumn( table.name, foreignKeyColumns, entry.getKey(), entry.getValue() );
			}
		}
		final var uniqueColumnNames = readUniqueColumnNames( metadata, table );
		try ( var resultSet = metadata.getColumns( table.catalog, table.schema, table.name, "%" ) ) {
			while ( resultSet.next() ) {
				final String columnName = resultSet.getString( "COLUMN_NAME" );
				if ( !isExcludedColumn( revengStrategy, table, columnName ) ) {
					final String columnAnnotationName = javaAnnotationName( columnName );
					validateJavaIdentifier( columnAnnotationName, "column" );
					if ( !columnNames.add( columnName ) ) {
						throw new GradleException(
								"Table `" + table.name + "` has multiple columns named `" + columnName + "`"
						);
					}
					if ( !columnAnnotationNames.add( columnAnnotationName ) ) {
						throw new GradleException(
								"Table `" + table.name + "` has multiple columns with names that map to generated "
										+ "annotation `" + columnAnnotationName + "`"
						);
					}
					final var jdbcType =
							resolveJdbcType( table.name, columnName,
									resultSet.getInt( "DATA_TYPE" ) );
					table.columns.add(
							new Column(
									columnName,
									isNullable( resultSet ),
									uniqueColumnNames.contains( columnName ),
									length( resultSet, jdbcType ),
									precision( resultSet, jdbcType ),
									scale( resultSet, jdbcType ),
									foreignKeyColumns.get( columnName ),
									resultSet.getInt( "ORDINAL_POSITION" )
							)
					);
				}
			}
		}
		table.columns.sort( comparingInt( column -> column.position ) );
	}

	private Set<String> readUniqueColumnNames(DatabaseMetaData metadata, Table table) throws SQLException {
		final var primaryKeyColumnNames = readPrimaryKeyColumnNames( metadata, table );
		final Map<String, Set<String>> uniqueIndexColumns = new HashMap<>();
		try ( var resultSet = metadata.getIndexInfo( table.catalog, table.schema, table.name, true, false ) ) {
			while ( resultSet.next() ) {
				if ( resultSet.getShort( "TYPE" ) == DatabaseMetaData.tableIndexStatistic
						|| resultSet.getBoolean( "NON_UNIQUE" ) ) {
					continue;
				}
				final String indexName = resultSet.getString( "INDEX_NAME" );
				final String columnName = resultSet.getString( "COLUMN_NAME" );
				if ( indexName != null && columnName != null ) {
					uniqueIndexColumns.computeIfAbsent( indexName, key -> new HashSet<>() )
							.add( columnName );
				}
			}
		}

		final Set<String> result = new HashSet<>();
		for ( var columnNames : uniqueIndexColumns.values() ) {
			if ( columnNames.size() == 1 ) {
				final var columnName = columnNames.iterator().next();
				if ( !primaryKeyColumnNames.contains( columnName ) ) {
					result.add( columnName );
				}
			}
		}
		return result;
	}

	private Set<String> readPrimaryKeyColumnNames(DatabaseMetaData metadata, Table table) throws SQLException {
		final Set<String> primaryKeyColumnNames = new HashSet<>();
		try ( var resultSet = metadata.getPrimaryKeys( table.catalog, table.schema, table.name ) ) {
			while ( resultSet.next() ) {
				primaryKeyColumnNames.add( resultSet.getString( "COLUMN_NAME" ) );
			}
		}
		return primaryKeyColumnNames;
	}

	private Map<String, ForeignKeyColumn> readForeignKeyColumns(DatabaseMetaData metadata, Table table)
			throws SQLException {
		final Map<String, ForeignKeyColumn> foreignKeyColumns = new HashMap<>();
		try ( var resultSet = metadata.getImportedKeys( table.catalog, table.schema, table.name ) ) {
			while ( resultSet.next() ) {
				final String columnName = resultSet.getString( "FKCOLUMN_NAME" );
				final var foreignKeyColumn = new ForeignKeyColumn(
						resultSet.getString( "PKTABLE_NAME" ),
						resultSet.getString( "PKCOLUMN_NAME" )
				);
				putForeignKeyColumn( table.name, foreignKeyColumns, columnName, foreignKeyColumn );
			}
		}
		return foreignKeyColumns;
	}

	private void putForeignKeyColumn(
			String tableName,
			Map<String, ForeignKeyColumn> foreignKeyColumns,
			String columnName,
			ForeignKeyColumn foreignKeyColumn) {
		final var previous = foreignKeyColumns.putIfAbsent( columnName, foreignKeyColumn );
		if ( previous != null && !previous.equals( foreignKeyColumn ) ) {
			throw new GradleException(
					"Column `" + tableName + "." + columnName
							+ "` is part of multiple foreign keys with different referenced columns"
			);
		}
	}

	private boolean isExcludedColumn(RevengStrategy revengStrategy, Table table, String columnName) {
		if ( revengStrategy == null ) {
			return false;
		}
		else {
			for ( var identifier : table.identifiers() ) {
				if ( revengStrategy.excludeColumn( identifier, columnName ) ) {
					return true;
				}
			}
			return false;
		}
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
		final var outputDirectory = getOutputDirectory().get().getAsFile();
		final var packageDirectory = packageDirectory( outputDirectory.toPath(), packageName );
		Files.createDirectories( packageDirectory );
		for ( var table : tables ) {
			final var outputFile = packageDirectory.resolve( javaAnnotationName( table.name ) + ".java" );
			Files.writeString( outputFile, renderTable( packageName, table ), UTF_8 );
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
		final var result = new StringBuilder();
		final String tableAnnotationName = javaAnnotationName( table.name );
		result.append( "package " ).append( packageName ).append( ";" ).append( lineSeparator() )
				.append( lineSeparator() )
				.append( "import java.lang.annotation.Retention;" ).append( lineSeparator() )
				.append( lineSeparator() )
				.append( "import org.hibernate.annotations.schema.ColumnMapping;" ).append( lineSeparator() )
				.append( "import org.hibernate.annotations.schema.JoinColumnMapping;" ).append( lineSeparator() )
				.append( "import org.hibernate.annotations.schema.TableMapping;" ).append( lineSeparator() )
				.append( lineSeparator() )
				.append( "import jakarta.persistence.Column;" ).append( lineSeparator() )
				.append( "import jakarta.persistence.JoinColumn;" ).append( lineSeparator() )
				.append( "import jakarta.persistence.Table;" ).append( lineSeparator() )
				.append( lineSeparator() )
				.append( "import static java.lang.annotation.RetentionPolicy.RUNTIME;" ).append( lineSeparator() )
				.append( lineSeparator() )
				.append( "@Retention(RUNTIME)" ).append( lineSeparator() )
				.append( "@TableMapping(@Table(name = " ).append( javaStringLiteral( table.name ) ).append( "))" )
				.append( lineSeparator() )
				.append( "public @interface " ).append( tableAnnotationName ).append( " {" ).append( lineSeparator() );

		for ( var column : table.columns ) {
			result.append( lineSeparator() )
					.append( "\t@Retention(RUNTIME)" ).append( lineSeparator() )
					.append( renderColumnAnnotation( column ) )
					.append( lineSeparator() )
					.append( "\t@interface " ).append( javaAnnotationName( column.name ) ).append( " {" )
					.append( lineSeparator() )
					.append( "\t}" ).append( lineSeparator() );
		}

		result.append( "}" ).append( lineSeparator() );
		return result.toString();
	}

	private String renderColumnAnnotation(Column column) {
		if ( column.foreignKeyColumn != null ) {
			return new StringBuilder()
					.append( "\t@JoinColumnMapping(@JoinColumn(name = " ).append( javaStringLiteral( column.name ) )
					.append( ", referencedColumnName = " )
					.append( javaStringLiteral( column.foreignKeyColumn.referencedColumnName ) )
					.append( ", nullable = " ).append( column.nullable )
					.append( "))" )
					.toString();
		}
		else {
			return new StringBuilder()
					.append( "\t@ColumnMapping(@Column(name = " ).append( javaStringLiteral( column.name ) )
					.append( ", nullable = " ).append( column.nullable )
					.append( ", unique = " ).append( column.unique )
					.append( ", length = " ).append( column.length )
					.append( ", precision = " ).append( column.precision )
					.append( ", scale = " ).append( column.scale )
					.append( "))" )
					.toString();
		}
	}

	private String javaStringLiteral(String value) {
		final var result = new StringBuilder( "\"" );
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
		final Set<String> tableAnnotationNames = new HashSet<>();
		for ( var table : tables ) {
			final String tableAnnotationName = javaAnnotationName( table.name );
			if ( !tableAnnotationNames.add( tableAnnotationName ) ) {
				throw new GradleException(
						"Multiple tables match generated annotation `" + tableAnnotationName + "`"
				);
			}
		}
	}

	private String javaAnnotationName(String name) {
		return name.toUpperCase( Locale.ROOT );
	}

	private void validateJavaIdentifier(String identifier, String role) {
		if ( identifier == null || identifier.isEmpty() ) {
			throw new GradleException( "A " + role + " name must not be empty" );
		}
		if ( JAVA_KEYWORDS.contains( identifier ) ) {
			throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
		}
		if ( !isJavaIdentifierStart( identifier.charAt( 0 ) ) ) {
			throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
		}
		for ( int i = 1; i < identifier.length(); i++ ) {
			if ( !isJavaIdentifierPart( identifier.charAt( i ) ) ) {
				throw new GradleException( "`" + identifier + "` is not a legal Java identifier for a " + role );
			}
		}
	}

	URL[] resolveProjectClassPath() {
		try {
			final var configurations = getProject().getConfigurations();
			final var runtimeClasspath = configurations.getByName( "runtimeClasspath" );
			final var resolvedConfiguration = runtimeClasspath.getResolvedConfiguration();
			final var artifacts = resolvedConfiguration.getResolvedArtifacts();
			final var urls = new URL[artifacts.size()];
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
			final var driverClass = classLoader.loadClass( driverClassName );
			final var constructor = driverClass.getDeclaredConstructor();
			final var driver = createDelegatingDriver( (Driver) constructor.newInstance() );
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
				(proxy, method, args) -> method.invoke( driver, args )
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
		if ( classLoader != null ) {
			try {
				classLoader.close();
			}
			catch (IOException e) {
				getLogger().warn( "Unable to close JDBC classloader", e );
			}
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
			String tableNamePattern,
			String revengFile) {
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

		private TableIdentifier identifier() {
			return TableIdentifier.create( catalog, schema, name );
		}

		private List<TableIdentifier> identifiers() {
			final var identifier = identifier();
			final var unqualifiedIdentifier = TableIdentifier.create( null, null, name );
			return identifier.equals( unqualifiedIdentifier )
					? List.of( identifier )
					: List.of( identifier, unqualifiedIdentifier );
		}

		private boolean matches(TableIdentifier identifier) {
			return Objects.equals( name, identifier.getName() )
				&& ( identifier.getCatalog() == null || Objects.equals( catalog, identifier.getCatalog() ) )
				&& ( identifier.getSchema() == null || Objects.equals( schema, identifier.getSchema() ) );
		}

		@Override
		public boolean equals(Object object) {
			return object instanceof Table table
				&& identifier().equals( table.identifier() );
		}

		@Override
		public int hashCode() {
			return identifier().hashCode();
		}
	}

	private record Column(String name, boolean nullable, boolean unique, int length, int precision, int scale,
			ForeignKeyColumn foreignKeyColumn, int position) {
	}

	private record ForeignKeyColumn(String referencedTableName, String referencedColumnName) {
	}
}
