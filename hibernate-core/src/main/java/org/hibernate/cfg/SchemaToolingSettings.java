/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.tool.schema.UniqueConstraintSchemaUpdateStrategy;

/**
 * @author Steve Ebersole
 */
public interface SchemaToolingSettings {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies what type of schema tooling action should be performed against the
	 * database specified using either {@value JdbcSettings#JAKARTA_HBM2DDL_CONNECTION} or the
	 * configured {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 * for the {@link org.hibernate.SessionFactory}.
	 * <p>
	 * Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * This setting takes precedence over {@value #HBM2DDL_AUTO}.
	 * <p>
	 * If no value is specified, the default is
	 * {@link org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @see org.hibernate.tool.schema.Action
	 * @see JdbcSettings#JAKARTA_HBM2DDL_CONNECTION
	 * @see JdbcSettings#JAKARTA_JDBC_URL
	 */
	String JAKARTA_HBM2DDL_DATABASE_ACTION = "jakarta.persistence.schema-generation.database.action";

	/**
	 * Specifies what type of schema tooling action should be written to script files.
	 * <p>
	 * Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * The script file is identified using {@value #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET}.
	 * <p>
	 * If no value is specified, the default is
	 * {@link org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @see org.hibernate.tool.schema.Action
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_ACTION = "jakarta.persistence.schema-generation.scripts.action";

	/**
	 * Specifies whether schema generation commands for schema creation are to be determined
	 * based on object/relational mapping metadata, DDL scripts, or a combination of the two.
	 * See {@link org.hibernate.tool.schema.SourceType} for the list of legal values.
	 * <p>
	 * If no value is specified, a default is inferred as follows:
	 * <ul>
	 *     <li>if source scripts are specified via {@value #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE},
	 *     then {@link org.hibernate.tool.schema.SourceType#SCRIPT "script"} is assumed, or
	 *     <li>otherwise, {@link org.hibernate.tool.schema.SourceType#SCRIPT "metadata"} is
	 *     assumed.
	 * </ul>
	 *
	 * @see org.hibernate.tool.schema.SourceType
	 */
	String JAKARTA_HBM2DDL_CREATE_SOURCE = "jakarta.persistence.schema-generation.create-source";

	/**
	 * Specifies whether schema generation commands for schema dropping are to be determined
	 * based on object/relational mapping metadata, DDL scripts, or a combination of the two.
	 * See {@link org.hibernate.tool.schema.SourceType} for the list of legal values.
	 * <p>
	 * If no value is specified, a default is inferred as follows:
	 * <ul>
	 *     <li>if source scripts are specified via {@value #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE},
	 *     then {@linkplain org.hibernate.tool.schema.SourceType#SCRIPT "script"} is assumed, or
	 *     <li>otherwise, {@linkplain org.hibernate.tool.schema.SourceType#SCRIPT "metadata"}
	 *     is assumed.
	 * </ul>
	 *
	 * @see org.hibernate.tool.schema.SourceType
	 */
	String JAKARTA_HBM2DDL_DROP_SOURCE = "jakarta.persistence.schema-generation.drop-source";

	/**
	 * Specifies the CREATE script file as either a {@link java.io.Reader} configured for reading
	 * the DDL script file or a string designating a file {@link java.net.URL} for the DDL script.
	 * <p>
	 * Hibernate historically also accepted {@link #HBM2DDL_IMPORT_FILES} for a similar purpose.
	 * This setting is now preferred.
	 *
	 * @see #JAKARTA_HBM2DDL_CREATE_SOURCE
	 * @see #HBM2DDL_IMPORT_FILES
	 */
	String JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE = "jakarta.persistence.schema-generation.create-script-source";

	/**
	 * Specifies the DROP script file as either a {@link java.io.Reader} configured for reading
	 * the DDL script file or a string designating a file {@link java.net.URL} for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_DROP_SOURCE
	 */
	String JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE = "jakarta.persistence.schema-generation.drop-script-source";

	/**
	 * For cases where {@value #JAKARTA_HBM2DDL_SCRIPTS_ACTION} indicates that schema creation
	 * commands should be written to a script file, this setting specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string specifying
	 * the file URL for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_ACTION
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET = "jakarta.persistence.schema-generation.scripts.create-target";

	/**
	 * For cases where {@value #JAKARTA_HBM2DDL_SCRIPTS_ACTION} indicates that schema
	 * drop commands should be written to a script file, this setting specifies either a
	 * {@link java.io.Writer} configured for output of the DDL script or a string
	 * specifying the file URL for the DDL script.
	 *
	 * @see #JAKARTA_HBM2DDL_SCRIPTS_ACTION
	 */
	String JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET = "jakarta.persistence.schema-generation.scripts.drop-target";

	/**
	 * JPA-standard variant of {@link #HBM2DDL_IMPORT_FILES} for specifying a database
	 * initialization script to be run as part of schema-export
	 * <p>
	 * Specifies a {@link java.io.Reader} configured for reading of the SQL load script
	 * or a string designating the {@link java.net.URL} for the SQL load script.
	 */
	String JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE = "jakarta.persistence.sql-load-script-source";

	/**
	 * The JPA variant of {@link #HBM2DDL_CREATE_NAMESPACES} used to specify whether database
	 * schemas used in the mapping model should be created on export in addition to creating
	 * the tables, sequences, etc.
	 * <p>
	 * The default is {@code false}, meaning to not create schemas
	 */
	String JAKARTA_HBM2DDL_CREATE_SCHEMAS = "jakarta.persistence.create-database-schemas";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Specifies the {@link org.hibernate.tool.schema.spi.SchemaManagementTool} to use for
	 * performing schema management.
	 * <p>
	 * By default, {@link org.hibernate.tool.schema.internal.HibernateSchemaManagementTool}
	 * is used.
	 *
	 * @since 5.0
	 */
	String SCHEMA_MANAGEMENT_TOOL = "hibernate.schema_management_tool";

	/**
	 * Setting to perform {@link org.hibernate.tool.schema.spi.SchemaManagementTool}
	 * actions automatically as part of the {@link org.hibernate.SessionFactory}
	 * lifecycle. Valid options are enumerated by {@link org.hibernate.tool.schema.Action}.
	 * <p>
	 * Interpreted in combination with {@link #JAKARTA_HBM2DDL_DATABASE_ACTION} and
	 * {@link #JAKARTA_HBM2DDL_SCRIPTS_ACTION}. If no value is specified, the default
	 * is {@linkplain org.hibernate.tool.schema.Action#NONE "none"}.
	 *
	 * @settingDefault {@code "none"}
	 *
	 * @see org.hibernate.tool.schema.Action
	 */
	String HBM2DDL_AUTO = "hibernate.hbm2ddl.auto";

	/**
	 * For cases where the {@value #JAKARTA_HBM2DDL_SCRIPTS_ACTION} value indicates that schema commands
	 * should be written to DDL script file, specifies if schema commands should be appended to
	 * the end of the file rather than written at the beginning of the file.
	 * <p>
	 * Values are: {@code true} for appending schema commands to the end of the file, {@code false}
	 * for writing schema commands at the beginning.
	 *
	 * @settingDefault {@code true}
	 */
	String HBM2DDL_SCRIPTS_CREATE_APPEND = "hibernate.hbm2ddl.schema-generation.script.append";

	/**
	 * The {@link org.hibernate.tool.schema.spi.SqlScriptCommandExtractor} implementation
	 * to use for parsing source/import files specified by {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE},
	 * {@link #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE} or {@link #HBM2DDL_IMPORT_FILES}. Either:
	 * <ul>
	 * <li>an instance of {@link org.hibernate.tool.schema.spi.SqlScriptCommandExtractor},
	 * <li>a {@link Class} object representing a class that implements {@code SqlScriptCommandExtractor},
	 *     or
	 * <li>the name of a class that implements {@code SqlScriptCommandExtractor}.
	 * </ul>
	 * <p>
	 * The correct extractor to use depends on the format of the SQL script:
	 * <ul>
	 * <li>if the script has one complete SQL statement per line, use
	 *     {@link org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor}, or
	 * <li>if a script contains statements spread over multiple lines, use
	 *     {@link org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor}.
	 * </ul>
	 *
	 * @settingDefault {@code org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor}.
	 *
	 * @see org.hibernate.tool.schema.internal.script.SingleLineSqlScriptExtractor
	 * @see org.hibernate.tool.schema.internal.script.MultiLineSqlScriptExtractor
	 */
	String HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR = "hibernate.hbm2ddl.import_files_sql_extractor";

	/**
	 * Used to specify the {@link org.hibernate.tool.schema.spi.SchemaFilterProvider} to be
	 * used by create, drop, migrate and validate operations on the database schema. A
	 * {@code SchemaFilterProvider} provides filters that can be used to limit the scope of
	 * these operations to specific namespaces, tables and sequences. All objects are
	 * included by default.
	 *
	 * @since 5.1
	 */
	String HBM2DDL_FILTER_PROVIDER = "hibernate.hbm2ddl.schema_filter_provider";

	/**
	 * Setting to choose the strategy used to access the JDBC Metadata.
	 * <p>
	 * Valid options are defined by {@link org.hibernate.tool.schema.JdbcMetadaAccessStrategy}.
	 * {@link org.hibernate.tool.schema.JdbcMetadaAccessStrategy#GROUPED} is the default.
	 *
	 * @settingDefault Grouped, unless {@value #ENABLE_SYNONYMS} is enabled
	 *
	 * @see org.hibernate.tool.schema.JdbcMetadaAccessStrategy
	 */
	String HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY = "hibernate.hbm2ddl.jdbc_metadata_extraction_strategy";

	/**
	 * Identifies the delimiter to use to separate schema management statements in script
	 * outputs.
	 *
	 * @settingDefault {@code ;}
	 */
	String HBM2DDL_DELIMITER = "hibernate.hbm2ddl.delimiter";

	/**
	 * The name of the charset used by the schema generation resource.
	 * <p>
	 * By default, the JVM default charset is used.
	 *
	 * @since 5.2.3
	 */
	String HBM2DDL_CHARSET_NAME = "hibernate.hbm2ddl.charset_name";

	/**
	 * When enabled, specifies that the schema migration tool should halt on any error,
	 * terminating the bootstrap process.
	 *
	 * @settingDefault {@code false}
	 *
	 * @since 5.2.4
	 */
	String HBM2DDL_HALT_ON_ERROR = "hibernate.hbm2ddl.halt_on_error";

	/**
	 * Used with the {@link jakarta.persistence.ConstraintMode#PROVIDER_DEFAULT}
	 * strategy for foreign key mapping.
	 * <p>
	 * Valid values are {@link jakarta.persistence.ConstraintMode#CONSTRAINT} and
	 * {@link jakarta.persistence.ConstraintMode#NO_CONSTRAINT}.
	 *
	 * @settingDefault {@link jakarta.persistence.ConstraintMode#CONSTRAINT}.
	 *
	 * @since 5.4
	 */
	String HBM2DDL_DEFAULT_CONSTRAINT_MODE = "hibernate.hbm2ddl.default_constraint_mode";

	/**
	 * Specifies the default storage engine for a relational databases that supports
	 * multiple storage engines. This property must be set either as an {@link Environment}
	 * variable or JVM System Property, since the {@link org.hibernate.dialect.Dialect} is
	 * instantiated before Hibernate property resolution.
	 *
	 * @since 5.2.9
	 */
	String STORAGE_ENGINE = "hibernate.dialect.storage_engine";

	/**
	 * If enabled, allows schema update and validation to support synonyms. Due
	 * to the possibility that this would return duplicate tables (especially in
	 * Oracle), this is disabled by default.
	 *
	 * @settingDefault {@code false}
	 */
	String ENABLE_SYNONYMS = "hibernate.synonyms";

	/**
	 * Specifies a comma-separated list of extra table types, in addition to the
	 * default types {@code "TABLE"} and {@code "VIEW"}, to recognize as physical
	 * tables when performing schema update, creation and validation.
	 *
	 * @since 5.0
	 */
	String EXTRA_PHYSICAL_TABLE_TYPES = "hibernate.hbm2ddl.extra_physical_table_types";

	/**
	 * Unique columns and unique keys both use unique constraints in most dialects.
	 * The schema exporter must create these constraints, but database support for
	 * finding existing constraints is extremely inconsistent. Worse, unique constraints
	 * without explicit names are assigned names with randomly generated characters.
	 * <p>
	 * Therefore, select from these strategies:
	 * <ul>
	 *     <li>{@linkplain UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY DROP_RECREATE_QUIETLY}:
	 *         Attempt to drop, then (re-)create each unique constraint, ignoring any exceptions thrown.
	 *         This is the default.
	 *     <li>{@linkplain UniqueConstraintSchemaUpdateStrategy#RECREATE_QUIETLY RECREATE_QUIETLY}:
	 *         Attempt to (re-)create unique constraints, ignoring exceptions thrown if the constraint already existed.
	 *     <li>{@linkplain UniqueConstraintSchemaUpdateStrategy#SKIP SKIP}:
	 *         Do not attempt to create unique constraints on a schema update.
	 * </ul>
	 *
	 * @settingDefault {@linkplain UniqueConstraintSchemaUpdateStrategy#DROP_RECREATE_QUIETLY DROP_RECREATE_QUIETLY}
	 */
	String UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY = "hibernate.schema_update.unique_constraint_strategy";

	/**
	 * Allows creation of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#PERSISTENT persistent}
	 * temporary tables at application startup to be disabled. By default, table creation is enabled.
	 *
	 * @deprecated Use {@link PersistentTableStrategy#CREATE_ID_TABLES}.
	 */
	@Deprecated(forRemoval = true)
	String BULK_ID_STRATEGY_PERSISTENT_TEMPORARY_CREATE_TABLES = PersistentTableStrategy.CREATE_ID_TABLES;

	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#PERSISTENT persistent}
	 * temporary tables at application shutdown to be disabled. By default, table dropping is enabled.
	 *
	 * @deprecated Use {@link PersistentTableStrategy#DROP_ID_TABLES}.
	 */
	@Deprecated(forRemoval = true)
	String BULK_ID_STRATEGY_PERSISTENT_TEMPORARY_DROP_TABLES = PersistentTableStrategy.DROP_ID_TABLES;

	/**
	 * Allows creation of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#GLOBAL global}
	 * temporary tables at application startup to be disabled. By default, table creation is enabled.
	 *
	 * @deprecated Use {@link GlobalTemporaryTableStrategy#CREATE_ID_TABLES}.
	 */
	@Deprecated(forRemoval = true)
	String BULK_ID_STRATEGY_GLOBAL_TEMPORARY_CREATE_TABLES = GlobalTemporaryTableStrategy.CREATE_ID_TABLES;

	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#GLOBAL global}
	 * temporary tables at application shutdown to be disabled. By default, table dropping is enabled.
	 *
	 * @deprecated Use {@link GlobalTemporaryTableStrategy#DROP_ID_TABLES}.
	 */
	@Deprecated(forRemoval = true)
	String BULK_ID_STRATEGY_GLOBAL_TEMPORARY_DROP_TABLES = GlobalTemporaryTableStrategy.DROP_ID_TABLES;

	/**
	 * Allows dropping of {@linkplain org.hibernate.dialect.temptable.TemporaryTableKind#LOCAL local}
	 * temporary tables at transaction commit to be enabled. By default, table dropping is disabled,
	 * and the database will drop the temporary tables automatically.
	 *
	 * @deprecated Use {@link LocalTemporaryTableStrategy#DROP_ID_TABLES}.
	 */
	@Deprecated(forRemoval = true)
	String BULK_ID_STRATEGY_LOCAL_TEMPORARY_DROP_TABLES = LocalTemporaryTableStrategy.DROP_ID_TABLES;



	/**
	 * Specifies a comma-separated list of file names of scripts containing SQL DML statements that
	 * should be executed after schema export completes. The order of the scripts is significant,
	 * with the first script in the list being executed first.
	 * <p>
	 * The scripts are only executed if the schema is created by Hibernate, that is, if
	 * {@value #HBM2DDL_AUTO} is set to {@code create} or {@code create-drop}.
	 * <p>
	 * The default value is {@code /import.sql}.
	 *
	 * @deprecated The JPA-standard setting {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE} is now preferred.
	 */
	@Deprecated
	String HBM2DDL_IMPORT_FILES = "hibernate.hbm2ddl.import_files";

	/**
	 * Specifies if the default {@code /import.sql} script file should not be executed
	 * when {@link #HBM2DDL_IMPORT_FILES} is not specified and {@value #HBM2DDL_AUTO} is set to {@code create} or {@code create-drop}.
	 *
	 * The default value is {@code false}.
	 */
	String HBM2DDL_SKIP_DEFAULT_IMPORT_FILE = "hibernate.hbm2ddl.skip_default_import_file";

	/**
	 * Specifies whether to automatically create also the database schema/catalog.
	 * The default is false.
	 *
	 * @since 5.0
	 *
	 * @deprecated The JPA-standard setting {@link #JAKARTA_HBM2DDL_CREATE_SCHEMAS} is now preferred.
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_NAMESPACES = "hibernate.hbm2ddl.create_namespaces";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Legacy JPA settings
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_DATABASE_ACTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DATABASE_ACTION = "javax.persistence.schema-generation.database.action";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_SCRIPTS_ACTION} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_ACTION = "javax.persistence.schema-generation.scripts.action";

	/**
	 * @deprecated  Migrate to {@link #JAKARTA_HBM2DDL_CREATE_SOURCE} instead
	 * @see org.hibernate.tool.schema.SourceType
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SOURCE = "javax.persistence.schema-generation.create-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_DROP_SOURCE}.
	 * @see org.hibernate.tool.schema.SourceType
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DROP_SOURCE = "javax.persistence.schema-generation.drop-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SCRIPT_SOURCE = "javax.persistence.schema-generation.create-script-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_DROP_SCRIPT_SOURCE = "javax.persistence.schema-generation.drop-script-source";

	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_CREATE_TARGET = "javax.persistence.schema-generation.scripts.create-target";


	/**
	 * @deprecated Migrate to {@link #JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET}
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_SCRIPTS_DROP_TARGET = "javax.persistence.schema-generation.scripts.drop-target";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_LOAD_SCRIPT_SOURCE = "javax.persistence.sql-load-script-source";

	/**
	 * @deprecated Use {@link #JAKARTA_HBM2DDL_CREATE_SCHEMAS} instead
	 */
	@Deprecated
	@SuppressWarnings("DeprecatedIsStillUsed")
	String HBM2DDL_CREATE_SCHEMAS = "javax.persistence.create-database-schemas";
}
