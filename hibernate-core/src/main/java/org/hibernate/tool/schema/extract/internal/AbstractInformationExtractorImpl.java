/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.JDBCException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.tool.schema.extract.internal.ForeignKeyInformationImpl.ColumnReferenceMappingImpl;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.NameSpaceForeignKeysInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceIndexesInformation;
import org.hibernate.tool.schema.extract.spi.NameSpacePrimaryKeysInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.SchemaExtractionException;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import static java.util.Collections.addAll;
import static org.hibernate.boot.model.naming.DatabaseIdentifier.toIdentifier;
import static org.hibernate.cfg.SchemaToolingSettings.ENABLE_SYNONYMS;
import static org.hibernate.cfg.SchemaToolingSettings.EXTRA_PHYSICAL_TABLE_TYPES;
import static org.hibernate.engine.jdbc.spi.SQLExceptionLogging.ERROR_LOG;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.splitTrimmingTokens;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

public abstract class AbstractInformationExtractorImpl implements InformationExtractor {

	private final String[] tableTypes;

	private final String[] extraPhysicalTableTypes;

	private final ExtractionContext extractionContext;

	private final boolean useJdbcMetadataDefaultsSetting;

	private Identifier currentCatalog;
	private Identifier currentSchema;

	private String currentCatalogFilter;
	private String currentSchemaFilter;


	public AbstractInformationExtractorImpl(ExtractionContext extractionContext) {
		this.extractionContext = extractionContext;
		final var configService =
				extractionContext.getServiceRegistry()
						.requireService( ConfigurationService.class );
		useJdbcMetadataDefaultsSetting = configService.getSetting(
				"hibernate.temp.use_jdbc_metadata_defaults",
				StandardConverters.BOOLEAN,
				Boolean.TRUE
		);
		final String extraPhysicalTableTypesConfig = configService.getSetting(
				EXTRA_PHYSICAL_TABLE_TYPES,
				StandardConverters.STRING,
				configService.getSetting(
						EXTRA_PHYSICAL_TABLE_TYPES,
						StandardConverters.STRING,
						""
				)
		);
		final var dialect = extractionContext.getJdbcEnvironment().getDialect();
		extraPhysicalTableTypes = getPhysicalTableTypes( extraPhysicalTableTypesConfig, dialect );
		tableTypes = getTableTypes( configService, dialect );
	}

	private String[] getPhysicalTableTypes(String extraPhysicalTableTypesConfig, Dialect dialect) {
		final List<String> physicalTableTypesList = new ArrayList<>();
		if ( !isBlank( extraPhysicalTableTypesConfig ) ) {
			addAll( physicalTableTypesList,
					splitTrimmingTokens( ",;", extraPhysicalTableTypesConfig, false ) );
		}
		dialect.augmentPhysicalTableTypes( physicalTableTypesList );
		return physicalTableTypesList.toArray( EMPTY_STRINGS );
	}

	private String[] getTableTypes(ConfigurationService configService, Dialect dialect) {
		final List<String> tableTypesList = new ArrayList<>();
		tableTypesList.add( "TABLE" );
		tableTypesList.add( "VIEW" );
		if ( getBoolean( ENABLE_SYNONYMS, configService.getSettings() ) ) {
			if ( dialect instanceof DB2Dialect ) { //TODO: should not use Dialect types directly!
				tableTypesList.add( "ALIAS" );
			}
			tableTypesList.add( "SYNONYM" );
		}
		addAll( tableTypesList, extraPhysicalTableTypes );
		dialect.augmentRecognizedTableTypes( tableTypesList );
		return tableTypesList.toArray( EMPTY_STRINGS );
	}

	private IdentifierHelper getIdentifierHelper() {
		return getJdbcEnvironment().getIdentifierHelper();
	}

	protected JDBCException convertSQLException(SQLException sqlException, String message) {
		return getJdbcEnvironment().getSqlExceptionHelper().convert( sqlException, message );
	}

	protected String toMetaDataObjectName(Identifier identifier) {
		return getIdentifierHelper().toMetaDataObjectName( identifier );
	}

	protected ExtractionContext getExtractionContext() {
		return extractionContext;
	}

	protected JdbcEnvironment getJdbcEnvironment() {
		return extractionContext.getJdbcEnvironment();
	}

	// The following methods purposely return the column labels that are defined by
	// DatabaseMetaData methods that return a ResultSet. Subclasses that do not rely
	// on DatabaseMetaData may override these methods to use different column labels.

	protected String getResultSetCatalogLabel() {
		return "TABLE_CAT";
	}
	protected String getResultSetSchemaLabel() {
		return "TABLE_SCHEM";
	}
	protected String getResultSetTableNameLabel() {
		return "TABLE_NAME";
	}
	protected String getResultSetTableTypeLabel() {
		return "TABLE_TYPE";
	}
	protected String getResultSetRemarksLabel() {
		return "REMARKS";
	}
	protected String getResultSetPrimaryKeyCatalogLabel() {
		return "PKTABLE_CAT";
	}
	protected String getResultSetPrimaryKeySchemaLabel() {
		return "PKTABLE_SCHEM";
	}
	protected String getResultSetPrimaryKeyTableLabel() {
		return "PKTABLE_NAME";
	}
	protected String getResultSetForeignKeyCatalogLabel() {
		return "FKTABLE_CAT";
	}
	protected String getResultSetForeignKeySchemaLabel() {
		return "FKTABLE_SCHEM";
	}
	protected String getResultSetForeignKeyTableLabel() {
		return "FKTABLE_NAME";
	}
	protected String getResultSetColumnNameLabel() {
		return "COLUMN_NAME";
	}
	protected String getResultSetSqlTypeCodeLabel() {
		return "DATA_TYPE";
	}
	protected String getResultSetTypeNameLabel() {
		return "TYPE_NAME";
	}
	protected String getResultSetColumnSizeLabel() {
		return "COLUMN_SIZE";
	}
	protected String getResultSetDecimalDigitsLabel() {
		return "DECIMAL_DIGITS";
	}
	protected String getResultSetIsNullableLabel() {
		return "IS_NULLABLE";
	}
	protected String getResultSetIndexTypeLabel() {
		return "TYPE";
	}
	protected String getResultSetIndexNameLabel() {
		return "INDEX_NAME";
	}
	protected String getResultSetForeignKeyLabel() {
		return "FK_NAME";
	}
	protected String getResultSetPrimaryKeyNameLabel() {
		return "PK_NAME";
	}
	protected String getResultSetColumnPositionColumn() {
		return "KEY_SEQ" ;
	}
	protected String getResultSetPrimaryKeyColumnNameLabel() {
		return "PKCOLUMN_NAME" ;
	}
	protected String getResultSetForeignKeyColumnNameLabel() {
		return "FKCOLUMN_NAME" ;
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a column of existing catalog
	 *         names. The column label must be the same as returned by
	 *         {@link #getResultSetCatalogLabel}.
	 *     </li>
	 *     <li>execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processCatalogsResultSet(ExtractionContext.ResultSetProcessor<T> processor) throws SQLException;

	@Override
	public boolean catalogExists(Identifier catalog) {
		try {
			return processCatalogsResultSet( resultSet -> {
				while ( resultSet.next() ) {
					final String existingCatalogName = resultSet.getString( getResultSetCatalogLabel() );
					// todo : hmm.. case sensitive or insensitive match...
					// for now, match any case...
					if ( catalog.getText().equalsIgnoreCase( existingCatalogName ) ) {
						return true;
					}
				}
				return false;
			} );
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Unable to query ResultSet for existing catalogs" );
		}
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for any existing
	 *         catalog/schema combination as specified by the {@code catalog}
	 *         and {@code schemaPattern} parameters described below. The row
	 *         contents will not be examined by {@code processor.process( resultSet )},
	 *         so column label names are not specified;
	 *     </li>
	 *     <li>execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code catalog} and {@code schemaPattern} parameters are as
	 * specified by {@link DatabaseMetaData#getSchemas(String, String)},
	 * and are copied here:
	 * @param catalog – a catalog name; must match the catalog name as it is
	 *                   stored in the database; "" retrieves those without
	 *                   a catalog; null means catalog name should not be
	 *                   used to narrow down the search.
	 * @param schemaPattern – a schema name; must match the schema name as
	 *                         it is stored in the database; null means schema
	 *                         name should not be used to narrow down the search.
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processSchemaResultSet(
			String catalog,
			String schemaPattern,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	@Override
	public boolean schemaExists(Identifier catalog, Identifier schema) {
		final var helper = getIdentifierHelper();
		final String catalogFilter =
				helper.toMetaDataCatalogName( catalog == null ? extractionContext.getDefaultCatalog() : catalog );
		final String schemaFilter =
				helper.toMetaDataSchemaName( schema == null ? extractionContext.getDefaultSchema() : schema );
		try {
			return processSchemaResultSet(
					catalogFilter,
					schemaFilter,
					resultSet -> {
						if ( !resultSet.next() ) {
							return false;
						}
						else if ( resultSet.next() ) {
							final String catalogName = catalog == null ? "" : catalog.getCanonicalName();
							final String schemaName = schema == null ? "" : schema.getCanonicalName();
							CORE_LOGGER.debugf(
									"Multiple schemas found with that name [%s.%s]",
									catalogName,
									schemaName
							);
						}
						return true;
					}
			);
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Unable to query ResultSet for existing schemas" );
		}
	}

	private TableInformation extractTableInformation(ResultSet resultSet) throws SQLException {
		return new TableInformationImpl(
				this,
				getIdentifierHelper(),
				extractTableName( resultSet ),
				isPhysicalTableType( resultSet.getString( getResultSetTableTypeLabel() ) ),
				resultSet.getString( getResultSetRemarksLabel() )
		);
	}

	private Connection getConnection() {
		return extractionContext.getJdbcConnection();
	}

	@Override
	public TableInformation getTable(Identifier catalog, Identifier schema, Identifier tableName) {
		if ( catalog != null || schema != null ) {
			// The table defined an explicit namespace.  In such cases we only ever want to look
			// in the identified namespace
			return locateTableInNamespace( catalog, schema, tableName );
		}
		else {
			// The table did not define an explicit namespace:
			// 		1) look in current namespace
			//		2) look in default namespace
			//		3) look in all namespaces - multiple hits is considered an error

			// 1) look in current namespace
			final Identifier currentSchema = getCurrentSchema();
			final Identifier currentCatalog = getCurrentCatalog();
			if ( currentCatalog != null || currentSchema != null ) {
				final var tableInfo = locateTableInNamespace( currentCatalog, currentSchema, tableName );
				if ( tableInfo != null ) {
					return tableInfo;
				}
			}

			// 2) look in default namespace
			final Identifier defaultCatalog = extractionContext.getDefaultCatalog();
			final Identifier defaultSchema = extractionContext.getDefaultSchema();
			if ( defaultCatalog != null || defaultSchema != null ) {
				final var tableInfo = locateTableInNamespace( defaultCatalog, defaultSchema, tableName );
				if ( tableInfo != null ) {
					return tableInfo;
				}
			}

			// 3) look in all namespaces
			try {
				return processTableResultSet(
						null,
						null,
						toMetaDataObjectName( tableName ),
						tableTypes,
						resultSet -> extractTableInformation( null, null, tableName, resultSet )
				);
			}
			catch (SQLException sqlException) {
				throw convertSQLException( sqlException, "Error accessing table metadata" );
			}
		}
	}

	private Identifier getCurrentSchema() {
		if ( getNameQualifierSupport() == NameQualifierSupport.CATALOG ) {
			return null;
		}
		else if ( currentSchema != null ) {
			return currentSchema;
		}
		else {
			final Identifier schema = getJdbcEnvironment().getCurrentSchema();
			if ( schema != null ) {
				currentSchema = schema;
			}
			if ( !useJdbcMetadataDefaultsSetting ) {
				try {
					currentSchema =
							getIdentifierHelper()
									.toIdentifier( getConnection().getSchema() );
				}
				catch (SQLException sqle) {
					ERROR_LOG.logErrorCodes( sqle.getErrorCode(), sqle.getSQLState() );
				}
				catch (AbstractMethodError ignore) {
					// jConnect and jTDS report that they "support" schemas, but they don't really
				}
			}
			return currentSchema;
		}
	}

	private Identifier getCurrentCatalog() {
		if ( getNameQualifierSupport() == NameQualifierSupport.SCHEMA ) {
			return null;
		}
		else if ( currentCatalog != null ) {
			return currentCatalog;
		}
		else {
			final Identifier catalog = getJdbcEnvironment().getCurrentCatalog();
			if ( catalog != null ) {
				currentCatalog = catalog;
			}
			if ( !useJdbcMetadataDefaultsSetting ) {
				try {
					currentCatalog =
							getIdentifierHelper()
									.toIdentifier( getConnection().getCatalog() );
				}
				catch (SQLException sqle) {
					ERROR_LOG.logErrorCodes( sqle.getErrorCode(), sqle.getSQLState() );
				}
			}
			return currentCatalog;
		}
	}

	private String getCurrentCatalogFilter(JdbcEnvironment jdbcEnvironment) {
		if ( currentCatalogFilter != null ) {
			return currentCatalogFilter;
		}
		final Identifier currentCatalog = jdbcEnvironment.getCurrentCatalog();
		if ( currentCatalog != null ) {
			currentCatalogFilter = toMetaDataObjectName( currentCatalog );
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentCatalogFilter = getConnection().getCatalog();
			}
			catch (SQLException sqle) {
				ERROR_LOG.logErrorCodes( sqle.getErrorCode(), sqle.getSQLState() );
			}
		}
		return currentCatalogFilter;
	}

	private String getCurrentSchemaFilter(JdbcEnvironment jdbcEnvironment) {
		if ( currentSchemaFilter != null ) {
			return currentSchemaFilter;
		}
		final Identifier currentSchema = jdbcEnvironment.getCurrentSchema();
		if ( currentSchema != null ) {
			currentSchemaFilter = toMetaDataObjectName( currentSchema );
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentSchemaFilter = getConnection().getSchema();
			}
			catch (SQLException sqle) {
				ERROR_LOG.logErrorCodes( sqle.getErrorCode(), sqle.getSQLState() );
			}
			catch (AbstractMethodError ignore) {
				// jConnect and jTDS report that they "support" schemas, but they don't really
			}
		}
		return currentSchemaFilter;
	}

	@Override
	public NameSpaceTablesInformation getTables(Identifier catalog, Identifier schema) {
		final String catalogFilter = getCatalogFilter( catalog );
		final String schemaFilter = getSchemaFilter( schema );
		try {
			return processTableResultSet(
					catalogFilter,
					schemaFilter,
					"%",
					tableTypes,
					resultSet -> {
						final var tablesInformation = extractNameSpaceTablesInformation( resultSet );
						populateTablesWithColumns( catalogFilter, schemaFilter, tablesInformation );
						return tablesInformation;
					} );
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	private String getCatalogFilter(Identifier catalog) {
		if ( supportsCatalogs() ) {
			if ( catalog == null ) {
				// look in the current namespace
				final String currentCatalogFilter = getCurrentCatalogFilter( getJdbcEnvironment() );
				if ( currentCatalogFilter != null ) {
					return currentCatalogFilter;
				}
				else {
					// 2) look in default namespace
					final Identifier defaultCatalog = extractionContext.getDefaultCatalog();
					return defaultCatalog != null ? toMetaDataObjectName( defaultCatalog ) : null;
				}
			}
			else {
				return toMetaDataObjectName( catalog );
			}
		}
		else {
			return null;
		}
	}

	private String getSchemaFilter(Identifier schema) {
		if ( supportsSchemas() ) {
			if ( schema == null ) {
				// 1) look in current namespace
				final String currentSchemaFilter = getCurrentSchemaFilter( getJdbcEnvironment() );
				if ( currentSchemaFilter != null ) {
					return currentSchemaFilter;
				}
				else {
					// 2) look in default namespace
					final Identifier defaultSchema = extractionContext.getDefaultSchema();
					return defaultSchema != null ? toMetaDataObjectName( defaultSchema ) : null;
				}
			}
			else {
				return toMetaDataObjectName( schema );
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for any existing
	 *         catalog/schema/table/column combination as specified by the
	 *         {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 *         and {@code columnNamePattern} parameters described below.
	 *         The {@link ResultSet} must contain the following, consistent with the
	 *         corresponding columns returned by {@link DatabaseMetaData#getColumns}
	 *         <ul>
	 *             <li>column label {@link #getResultSetTableNameLabel} for table name</li>
	 *             <li>column label {@link #getResultSetColumnNameLabel} for column name</li>
	 *             <li>column label {@link #getResultSetSqlTypeCodeLabel} SQL type code from java.sql.Types</li>
	 *             <li>column label {@link #getResultSetTypeNameLabel} for database column type name</li>
	 *             <li>column label {@link #getResultSetColumnSizeLabel} for column size</li>
	 *             <li>column label {@link #getResultSetDecimalDigitsLabel} for number of fractional digits</li>
	 *             <li>column label {@link #getResultSetIsNullableLabel} for nullability</li>
	 *         </ul>
	 *         Rows must be ordered by catalog, schema, table name, and column position.
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 * and {@code columnNamePattern} parameters are as
	 * specified by {@link DatabaseMetaData#getColumns(String, String, String, String)},
	 * and are copied here:
	 * <p>
	 * @param catalog – a catalog name; must match the catalog name as it is
	 *                   stored in the database; "" retrieves those without
	 *                   a catalog; null means that the catalog name should
	 *                   not be used to narrow the search
	 * @param schemaPattern – a schema name pattern; must match the schema
	 *                         name as it is stored in the database; ""
	 *                         retrieves those without a schema; null means
	 *                         that the schema name should not be used to
	 *                         narrow the search
	 * @param tableNamePattern – a table name pattern; must match the table
	 *                            name as it is stored in the database
	 * @param columnNamePattern – a column name pattern; must match the
	 *                             column name as it is stored in the database
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processColumnsResultSet(
			String catalog,
			String schemaPattern,
			String tableNamePattern,
			String columnNamePattern,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	private void populateTablesWithColumns(
			String catalogFilter,
			String schemaFilter,
			NameSpaceTablesInformation tables) {
		try {
			processColumnsResultSet(
					catalogFilter,
					schemaFilter,
					null,
					"%",
					resultSet -> {
						String currentTableName = "";
						TableInformation currentTable = null;
						while ( resultSet.next() ) {
							if ( !currentTableName.equals( resultSet.getString( getResultSetTableNameLabel() ) ) ) {
								currentTableName = resultSet.getString( getResultSetTableNameLabel() );
								currentTable = tables.getTableInformation( currentTableName );
							}
							if ( currentTable != null ) {
								currentTable.addColumn( columnInformation( currentTable, resultSet ) );
							}
						}
						return null;
					}
			);
		}
		catch (SQLException e) {
			throw convertSQLException( e, "Error accessing tables metadata" );
		}
	}

	/*
	 * Hibernate Reactive overrides this
	 */
	protected ColumnInformationImpl columnInformation(TableInformation tableInformation, ResultSet resultSet)
			throws SQLException {
		return new ColumnInformationImpl(
				tableInformation,
				toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) ),
				resultSet.getInt( getResultSetSqlTypeCodeLabel() ),
				new StringTokenizer( resultSet.getString( getResultSetTypeNameLabel() ), "()" ).nextToken(),
				resultSet.getInt( getResultSetColumnSizeLabel() ),
				resultSet.getInt( getResultSetDecimalDigitsLabel() ),
				interpretTruthValue( resultSet.getString( getResultSetIsNullableLabel() ) )
		);
	}

	private NameSpaceTablesInformation extractNameSpaceTablesInformation(ResultSet resultSet)
			throws SQLException {
		final var tables = new NameSpaceTablesInformation( getIdentifierHelper() );
		while ( resultSet.next() ) {
			tables.addTableInformation( extractTableInformation( resultSet ) );
		}
		return tables;
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for any existing
	 *         catalog/schema/table/table type combination as specified by the
	 *         {@code catalogFilter}, {@code schemaFilter}, {@code tableNameFilter},
	 *         and {@code tableTypes} parameters described below.
	 *         The {@link ResultSet} must contain the following, consistent with the
	 *         corresponding columns returned by {@link DatabaseMetaData#getTables(String, String, String, String[])}
	 *         <ul>
	 *             <li>column label {@link #getResultSetTableNameLabel} for table name</li>
	 *             <li>column label {@link #getResultSetTableTypeLabel} for table type</li>
	 *             <li>column label {@link #getResultSetRemarksLabel} for table comment</li>
	 *         </ul>
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 * and {@code columnNamePattern} parameters are as
	 * specified by {@link DatabaseMetaData#getTables(String, String, String, String[])},
	 * and are copied here:
	 *
	 * @param catalog - a catalog name; must match the catalog name as it is
	 *                   stored in the database; "" retrieves those without a
	 *                   catalog; null means that the catalog name should not
	 *                   be used to narrow the search
	 * @param schemaPattern - a schema name pattern; must match the schema name
	 *                        as it is stored in the database; "" retrieves
	 *                        those without a schema; null means that the schema
	 *                        name should not be used to narrow the search
	 * @param tableNamePattern - a table name pattern; must match the table name
	 *                           as it is stored in the database
	 * @param types - a list of table types
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processTableResultSet(
			String catalog,
			String schemaPattern,
			String tableNamePattern,
			String[] types,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	private TableInformation locateTableInNamespace(
			Identifier catalog,
			Identifier schema,
			Identifier tableName) {
		final String catalogFilter = catalogFilter( catalog );
		final String schemaFilter = schemaFilter( schema );
		final Identifier catalogToUse = supportsCatalogs() ? catalog : null;
		final Identifier schemaToUse = supportsSchemas() ? schema : null;
		final String tableNameFilter = toMetaDataObjectName( tableName );
		try {
			return processTableResultSet(
					catalogFilter,
					schemaFilter,
					tableNameFilter,
					tableTypes,
					resultSet -> extractTableInformation( catalogToUse, schemaToUse, tableName, resultSet )
			);

		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	private String catalogFilter(Identifier catalog) {
		if ( supportsCatalogs() ) {
			if ( catalog == null ) {
				try {
					return getConnection().getCatalog();
				}
				catch (SQLException ignore) {
					return "";
				}
			}
			else {
				return toMetaDataObjectName( catalog );
			}
		}
		else {
			return null;
		}
	}

	private String schemaFilter(Identifier schema) {
		if ( supportsSchemas() ) {
			return schema == null ? "" : toMetaDataObjectName( schema );
		}
		else {
			return null;
		}
	}

	private NameQualifierSupport getNameQualifierSupport() {
		return getJdbcEnvironment().getNameQualifierSupport();
	}

	private boolean supportsCatalogs() {
		return getNameQualifierSupport().supportsCatalogs();
	}

	private boolean supportsSchemas() {
		return getNameQualifierSupport().supportsSchemas();
	}

	private TableInformation extractTableInformation(
			Identifier catalog,
			Identifier schema,
			Identifier tableName,
			ResultSet resultSet)
					throws SQLException {

		boolean found = false;
		TableInformation tableInformation = null;
		while ( resultSet.next() ) {
			final Identifier identifier =
					toIdentifier( resultSet.getString( getResultSetTableNameLabel() ),
							tableName.isQuoted() );
			if ( tableName.equals( identifier ) ) {
				if ( found ) {
					CORE_LOGGER.multipleTablesFound( tableName.render() );
					throw new SchemaExtractionException(
							String.format(
									Locale.ENGLISH,
									"More than one table found in namespace (%s, %s) : %s",
									catalog == null ? "" : catalog.render(),
									schema == null ? "" : schema.render(),
									tableName.render()
							)
					);
				}
				else {
					found = true;
					tableInformation = extractTableInformation( resultSet );
					addColumns( tableInformation );
				}
			}
		}
		if ( !found ) {
			CORE_LOGGER.tableNotFound( tableName.render() );
		}
		return tableInformation;
	}

	protected abstract String getResultSetTableTypesPhysicalTableConstant();

	protected boolean isPhysicalTableType(String tableType) {
		final boolean isTableType =
				getResultSetTableTypesPhysicalTableConstant()
						.equalsIgnoreCase( tableType );
		if ( extraPhysicalTableTypes == null ) {
			return isTableType;
		}
		else {
			if ( isTableType ) {
				return true;
			}
			else {
				for ( String extraPhysicalTableType : extraPhysicalTableTypes ) {
					if ( extraPhysicalTableType.equalsIgnoreCase( tableType ) ) {
						return true;
					}
				}
				return false;
			}
		}
	}

	protected void addColumns(TableInformation tableInformation) {
		final var tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();
		try {
			processColumnsResultSet(
					catalog == null ? "" : catalog.getText(),
					schema == null ? "" : schema.getText(),
					tableName.getTableName().getText(),
					"%",
					resultSet -> {
						while ( resultSet.next() ) {
							tableInformation.addColumn( columnInformation( tableInformation, resultSet ) );
						}
						return null;
					}
			);

		}
		catch (SQLException e) {
			throw convertSQLException( e, "Error accessing tables metadata" );
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected Boolean interpretTruthValue(String nullable) {
		if ( "yes".equalsIgnoreCase( nullable ) ) {
			return Boolean.TRUE;
		}
		else if ( "no".equalsIgnoreCase( nullable ) ) {
			return Boolean.FALSE;
		}
		else {
			return null;
		}
	}

	// This method is not currently used.
	protected abstract <T> T processPrimaryKeysResultSet(
			String catalogFilter,
			String schemaFilter,
			Identifier tableName,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	protected abstract <T> T processPrimaryKeysResultSet(
			String catalogFilter,
			String schemaFilter,
			@Nullable String tableName,
			ExtractionContext.ResultSetProcessor<T> processor)
			throws SQLException;

	@Override
	public @Nullable PrimaryKeyInformation getPrimaryKey(TableInformation tableInformation) {
		final var databaseObjectAccess = extractionContext.getDatabaseObjectAccess();
		if ( databaseObjectAccess.isCaching() && supportsBulkPrimaryKeyRetrieval() ) {
			return databaseObjectAccess.locatePrimaryKeyInformation( tableInformation.getName() );
		}

		final var tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();
		try {
			return processPrimaryKeysResultSet(
					catalog == null ? "" : catalog.getText(),
					schema == null ? "" : schema.getText(),
					tableInformation.getName().getTableName(),
					resultSet -> extractPrimaryKeyInformation( tableInformation, resultSet )
			);
		}
		catch (SQLException e) {
			throw convertSQLException( e,
					"Error while reading primary key meta data for "
							+ tableInformation.getName() );
		}
	}

	private PrimaryKeyInformation extractPrimaryKeyInformation(TableInformation tableInformation, ResultSet resultSet)
			throws SQLException {

		final List<ColumnInformation> columns = new ArrayList<>();
		boolean firstPass = true;
		Identifier primaryKeyIdentifier = null;

		while ( resultSet.next() ) {
			final String currentPkName = resultSet.getString( getResultSetPrimaryKeyNameLabel() );
			final Identifier currentPrimaryKeyIdentifier =
					currentPkName == null ? null : toIdentifier( currentPkName );
			if ( firstPass ) {
				primaryKeyIdentifier = currentPrimaryKeyIdentifier;
				firstPass = false;
			}
			else {
				if ( !Objects.equals( primaryKeyIdentifier, currentPrimaryKeyIdentifier ) ) {
					throw new SchemaExtractionException( "Encountered primary keys differing name on table "
							+ tableInformation.getName().toString() );
				}
			}

			final int columnPosition = resultSet.getInt( getResultSetColumnPositionColumn() );
			final int index = columnPosition - 1;
			// Fill up the array list with nulls up to the desired index, because some JDBC drivers don't return results ordered by column position
			while ( columns.size() <= index ) {
				columns.add( null );
			}
			final Identifier columnIdentifier =
					toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
			columns.set( index, tableInformation.getColumn( columnIdentifier ) );
		}
		if ( firstPass ) {
			// we did not find any results (no pk)
			return null;
		}
		else {
			// validate column list is properly contiguous
			for ( int i = 0; i < columns.size(); i++ ) {
				if ( columns.get( i ) == null ) {
					throw new SchemaExtractionException( "Primary Key information was missing for KEY_SEQ = " + ( i+1) );
				}
			}
			// build the return
			return new PrimaryKeyInformationImpl( primaryKeyIdentifier, columns );
		}
	}

	@Override
	public NameSpacePrimaryKeysInformation getPrimaryKeys(Identifier catalog, Identifier schema) {
		if ( !supportsBulkPrimaryKeyRetrieval() ) {
			throw new UnsupportedOperationException( "Database doesn't support extracting all primary keys at once" );
		}
		else {
			try {
				return processPrimaryKeysResultSet(
						catalog == null ? "" : catalog.getText(),
						schema == null ? "" : schema.getText(),
						(String) null,
						this::extractNameSpacePrimaryKeysInformation
				);
			}
			catch (SQLException e) {
				throw convertSQLException( e,
						"Error while reading primary key meta data for namespace "
						+ new Namespace.Name( catalog, schema ) );
			}
		}
	}

	private TableInformation getTableInformation(
			@Nullable String catalogName,
			@Nullable String schemaName,
			@Nullable String tableName) {
		final var qualifiedTableName = new QualifiedTableName(
				toIdentifier( catalogName ),
				toIdentifier( schemaName ),
				toIdentifier( tableName )
		);
		final var tableInformation =
				extractionContext.getDatabaseObjectAccess().locateTableInformation( qualifiedTableName );
		if ( tableInformation == null ) {
			throw new SchemaExtractionException( "Could not locate table information for " + qualifiedTableName );
		}
		return tableInformation;
	}

	protected NameSpacePrimaryKeysInformation extractNameSpacePrimaryKeysInformation(ResultSet resultSet)
			throws SQLException {
		final var primaryKeysInformation = new NameSpacePrimaryKeysInformation( getIdentifierHelper() );

		while ( resultSet.next() ) {
			final String currentTableName = resultSet.getString( getResultSetPrimaryKeyTableLabel() );
			final String currentPkName = resultSet.getString( getResultSetPrimaryKeyNameLabel() );
			final Identifier currentPrimaryKeyIdentifier =
					currentPkName == null ? null : toIdentifier( currentPkName );
			final TableInformation tableInformation = getTableInformation(
					resultSet.getString( getResultSetPrimaryKeyCatalogLabel() ),
					resultSet.getString( getResultSetPrimaryKeySchemaLabel() ),
					currentTableName
			);
			PrimaryKeyInformation primaryKeyInformation =
					primaryKeysInformation.getPrimaryKeyInformation( currentTableName );
			final List<ColumnInformation> columns;
			if ( primaryKeyInformation != null ) {
				if ( !Objects.equals( primaryKeyInformation.getPrimaryKeyIdentifier(), currentPrimaryKeyIdentifier ) ) {
					throw new SchemaExtractionException( "Encountered primary keys differing name on table "
														+ currentTableName );
				}
				columns = (List<ColumnInformation>) primaryKeyInformation.getColumns();
			}
			else {
				columns = new ArrayList<>();
				primaryKeyInformation = new PrimaryKeyInformationImpl( currentPrimaryKeyIdentifier, columns );
				primaryKeysInformation.addPrimaryKeyInformation( tableInformation, primaryKeyInformation );
			}

			final int columnPosition = resultSet.getInt( getResultSetColumnPositionColumn() );
			final int index = columnPosition - 1;
			// Fill up the array list with nulls up to the desired index, because some JDBC drivers don't return results ordered by column position
			while ( columns.size() <= index ) {
				columns.add( null );
			}
			final Identifier columnIdentifier =
					toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
			columns.set( index, tableInformation.getColumn( columnIdentifier ) );
		}
		primaryKeysInformation.validate();
		return primaryKeysInformation;
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for each column
	 *         defined in an index. The {@link ResultSet} must contain the
	 *         following, consistent with the corresponding columns returned
	 *         by {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)}
	 *         <ul>
	 *             <li>column label {@link #getResultSetIndexNameLabel} for index name;
	 *             null when TYPE is tableIndexStatistic</li>
	 *             <li>column label {@link #getResultSetIndexTypeLabel} index type:
	 *                 <ul>
	 *                     <li>
	 *                         {@link DatabaseMetaData#tableIndexStatistic} -
	 *                         this identifies table statistics that are returned
	 *                         in conjunction with a table's index descriptions
	 *                     </li>
	 *                     <li>
	 *                         Any value other than {@link DatabaseMetaData#tableIndexStatistic} -
	 *                         this indicates that a table's index description
	 *                         (not statisics) is being returned.
	 *                     </li>
	 *                 </ul>
	 *                 Note that Hibernate ignores statistics and does not care
	 *                 about the actual type of index.
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetColumnNameLabel} -
	 *                 column name; <code>null</code> when TYPE is
	 *                 {@link DatabaseMetaData#tableIndexStatistic}
	 *             </li>
	 *         </ul>
	 *         The ResultSet must be ordered so that the columns for a
	 *         particular index are in contiguous rows in order of column
	 *         position.
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 * and {@code columnNamePattern} parameters are as
	 * specified by {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)},
	 * and are copied here:
	 * <p>
	 * @param catalog – a catalog name; must match the catalog name as it is
	 *                   stored in this database; "" retrieves those without
	 *                   a catalog; null means that the catalog name should
	 *                   not be used to narrow the search
	 * @param schema – a schema name; must match the schema name as it is
	 *                  stored in this database; "" retrieves those without
	 *                  a schema; null means that the schema name should not
	 *                  be used to narrow the search
	 * @param table – a table name; must match the table name as it is stored
	 *                in this database
	 * @param unique – when true, return only indices for unique values; when
	 *                 false, return indices regardless of whether unique or not
	 * @param approximate – when true, result is allowed to reflect approximate
	 *                       or out of data values; when false, results are
	 *                       requested to be accurate
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processIndexInfoResultSet(
			String catalog,
			String schema,
			@Nullable String table,
			boolean unique,
			boolean approximate,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	@Override
	public Iterable<IndexInformation> getIndexes(TableInformation tableInformation) {
		final var databaseObjectAccess = extractionContext.getDatabaseObjectAccess();
		if ( databaseObjectAccess.isCaching() && supportsBulkIndexRetrieval() ) {
			return databaseObjectAccess.locateIndexesInformation( tableInformation.getName() );
		}

		final var tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();

		final Map<Identifier, IndexInformationImpl.Builder> builders = new HashMap<>();
		try {
			processIndexInfoResultSet(
					catalog == null ? "" : catalog.getText(),
					schema == null ? "" : schema.getText(),
					tableName.getTableName().getText(),
					false,        // DO NOT limit to just unique
					true,        // DO require up-to-date results
					resultSet -> {
						while ( resultSet.next() ) {
							if ( resultSet.getShort( getResultSetIndexTypeLabel() )
									!= DatabaseMetaData.tableIndexStatistic ) {
								final Identifier indexIdentifier =
										toIdentifier( resultSet.getString( getResultSetIndexNameLabel() ) );
								var builder = indexInformationBuilder( builders, indexIdentifier );
								final Identifier columnIdentifier =
										toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
								final var columnInformation = tableInformation.getColumn( columnIdentifier );
								if ( columnInformation == null ) {
									// See HHH-10191: this may happen when dealing with Oracle/PostgreSQL function indexes
									CORE_LOGGER.logCannotLocateIndexColumnInformation(
											columnIdentifier.getText(),
											indexIdentifier.getText()
									);
								}
								builder.addColumn( columnInformation );
							}

						}
						return null;
					}
			);

		}
		catch (SQLException e) {
			throw convertSQLException( e,
					"Error accessing index information: "
							+ tableInformation.getName() );
		}

		final List<IndexInformation> indexes = new ArrayList<>( builders.size() );
		for ( var builder : builders.values() ) {
			final var index = builder.build();
			indexes.add( index );
		}
		return indexes;
	}

	private static IndexInformationImpl.Builder indexInformationBuilder(
			Map<Identifier, IndexInformationImpl.Builder> builders,
			Identifier indexIdentifier) {
		var builder = builders.get( indexIdentifier );
		if ( builder == null ) {
			builder = IndexInformationImpl.builder( indexIdentifier );
			builders.put( indexIdentifier, builder );
		}
		return builder;
	}

	@Override
	public NameSpaceIndexesInformation getIndexes(Identifier catalog, Identifier schema) {
		if ( !supportsBulkIndexRetrieval() ) {
			throw new UnsupportedOperationException( "Database doesn't support extracting all indexes at once" );
		}
		else {
			try {
				return processIndexInfoResultSet(
						catalog == null ? "" : catalog.getText(),
						schema == null ? "" : schema.getText(),
						null,
						false,
						true,
						this::extractNameSpaceIndexesInformation
				);
			}
			catch (SQLException e) {
				throw convertSQLException( e,
						"Error while reading index information for namespace "
						+ new Namespace.Name( catalog, schema ) );
			}
		}
	}

	protected NameSpaceIndexesInformation extractNameSpaceIndexesInformation(ResultSet resultSet)
			throws SQLException {
		final var indexesInformation = new NameSpaceIndexesInformation( getIdentifierHelper() );

		while ( resultSet.next() ) {
			if ( resultSet.getShort( getResultSetIndexTypeLabel() )
				!= DatabaseMetaData.tableIndexStatistic ) {
				final TableInformation tableInformation = getTableInformation(
						resultSet.getString( getResultSetCatalogLabel() ),
						resultSet.getString( getResultSetSchemaLabel() ),
						resultSet.getString( getResultSetTableNameLabel() )
				);
				final Identifier indexIdentifier =
						toIdentifier( resultSet.getString( getResultSetIndexNameLabel() ) );
				final var index = getOrCreateIndexInformation( indexesInformation, indexIdentifier, tableInformation );
				final Identifier columnIdentifier =
						toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
				final var columnInformation = tableInformation.getColumn( columnIdentifier );
				if ( columnInformation == null ) {
					// See HHH-10191: this may happen when dealing with Oracle/PostgreSQL function indexes
					CORE_LOGGER.logCannotLocateIndexColumnInformation(
							columnIdentifier.getText(),
							indexIdentifier.getText()
					);
				}
				index.getIndexedColumns().add( columnInformation );
			}
		}
		return indexesInformation;
	}

	private IndexInformation getOrCreateIndexInformation(
			NameSpaceIndexesInformation indexesInformation,
			Identifier indexIdentifier,
			TableInformation tableInformation) {
		final List<IndexInformation> indexes =
				indexesInformation.getIndexesInformation( tableInformation.getName().getTableName().getText() );
		if ( indexes != null ) {
			for ( IndexInformation index : indexes ) {
				if ( indexIdentifier.equals( index.getIndexIdentifier() ) ) {
					return index;
				}
			}
		}
		final var indexInformation = new IndexInformationImpl( indexIdentifier, new ArrayList<>() );
		indexesInformation.addIndexInformation( tableInformation, indexInformation );
		return indexInformation;
	}

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for each foreign key/
	 *         primary key column making up a foreign key for any existing
	 *         catalog/schema/table combination as specified by the
	 *         {@code catalog}, {@code schema}, and {@code table}
	 *         parameters described below.
	 *         The {@link ResultSet} must contain the following, consistent
	 *         with the corresponding columns returned by {@link DatabaseMetaData#getImportedKeys}:
	 *         <ul>
	 *             <li>
	 *                 column label {@link #getResultSetForeignKeyLabel} -
	 *                 foreign key name (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyCatalogLabel} -
	 *                 primary key table catalog being imported (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeySchemaLabel} -
	 *                 primary key table schema being imported (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyTableLabel} -
	 *                 primary key table name being imported
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetForeignKeyColumnNameLabel} -
	 *                 foreign key column name
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyColumnNameLabel} -
	 *                 primary key column name being imported
	 *             </li>
	 *         </ul>
	 *         The ResultSet must be ordered by the primary key
	 *         catalog/schema/table and column position within the key.
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code catalog}, {@code schema}, and {@code table}
	 * parameters are as specified by {@link DatabaseMetaData#getImportedKeys(String, String, String)}
	 * and are copied here:
	 *
	 * @param catalog – a catalog name; must match the catalog name as it is
	 *                   stored in the database; "" retrieves those without a
	 *                   catalog; null means that the catalog name should not
	 *                   be used to narrow the search
	 * @param schema – a schema name; must match the schema name as it is
	 *                  stored in the database; "" retrieves those without a
	 *                  schema; null means that the schema name should not be
	 *                  used to narrow the search
	 * @param table – a table name; must match the table name as it is stored
	 *                in the database
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 */
	protected abstract <T> T processImportedKeysResultSet(
			String catalog,
			String schema,
			@Nullable String table,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	/**
	 * Must do the following:
	 * <ol>
	 *     <li>
	 *         obtain a {@link ResultSet} containing a row for each foreign key
	 *         column making up a foreign key for any existing
	 *         foreignCatalog/foreignSchema/foreignTable combination as specified by
	 *         parameters described below.
	 *         The {@link ResultSet} must contain the following, consistent
	 *         with the corresponding columns returned by {@link DatabaseMetaData#getCrossReference}:
	 *         <ul>
	 *             <li>
	 *                 column label {@link #getResultSetForeignKeyLabel} -
	 *                 foreign key name (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyCatalogLabel} -
	 *                 primary key table catalog being imported (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeySchemaLabel} -
	 *                 primary key table schema being imported (may be null)
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyTableLabel} -
	 *                 primary key table name being imported
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetForeignKeyColumnNameLabel} -
	 *                 foreign key column name
	 *             </li>
	 *             <li>
	 *                 column label {@link #getResultSetPrimaryKeyColumnNameLabel} -
	 *                 primary key column name being imported
	 *             </li>
	 *         </ul>
	 *         The ResultSet must be ordered by the primary key
	 *         foreignCatalog/foreignSchema/foreignTable and column position within the key.
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p>
	 * The {@code parentCatalog}, {@code parentSchema}, {@code parentTable},
	 * {@code foreignCatalog}, {@code foreignSchema}, {@code foreignTable}
	 * parameters are as specified by {@link DatabaseMetaData#getCrossReference(
	 * String, String, String, String, String, String)}
	 * and are copied here:
	 *
	 * @param parentCatalog a catalog name; must match the catalog name
	 * as it is stored in the database; "" retrieves those without a
	 * catalog; {@code null} means drop catalog name from the selection criteria
	 * @param parentSchema a schema name; must match the schema name as
	 * it is stored in the database; "" retrieves those without a schema;
	 * {@code null} means drop schema name from the selection criteria
	 * @param parentTable the name of the table that exports the key; must match
	 * the table name as it is stored in the database
	 * @param foreignCatalog a catalog name; must match the catalog name as
	 * it is stored in the database; "" retrieves those without a
	 * catalog; {@code null} means drop catalog name from the selection criteria
	 * @param foreignSchema a schema name; must match the schema name as it
	 * is stored in the database; "" retrieves those without a schema;
	 * {@code null} means drop schema name from the selection criteria
	 * @param foreignTable the name of the table that imports the key; must match
	 * the table name as it is stored in the database
	 * @param processor - the provided ResultSetProcessor.
	 * @param <T> - defined by {@code processor}
	 * @return - defined by {@code processor}
	 * @throws SQLException - if a database error occurs
	 * @see #processImportedKeysResultSet(String, String, String,
	 * ExtractionContext.ResultSetProcessor)
	 */
	protected abstract <T> T processCrossReferenceResultSet(
			String parentCatalog,
			String parentSchema,
			String parentTable,
			String foreignCatalog,
			String foreignSchema,
			String foreignTable,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;


	@Override
	public Iterable<ForeignKeyInformation> getForeignKeys(TableInformation tableInformation) {
		final var databaseObjectAccess = extractionContext.getDatabaseObjectAccess();
		if ( databaseObjectAccess.isCaching() && supportsBulkForeignKeyRetrieval() ) {
			return databaseObjectAccess.locateForeignKeyInformation( tableInformation.getName() );
		}

		final var tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();
		final String catalogFilter = catalog == null ? "" : catalog.getText();
		final String schemaFilter = schema == null ? "" : schema.getText();
		final Map<Identifier, ForeignKeyBuilder> builders = new HashMap<>();
		try {
			final String table = tableInformation.getName().getTableName().getText();
			processImportedKeysResultSet( catalogFilter, schemaFilter, table,
					resultSet -> {
						process( tableInformation, resultSet, builders );
						return null;
					} );
			final var dialect = getJdbcEnvironment().getDialect();
			if ( dialect.useCrossReferenceForeignKeys() ) {
				processCrossReferenceResultSet(
						null,
						null,
						dialect.getCrossReferenceParentTableFilter(),
						catalogFilter,
						schemaFilter,
						table,
						resultSet -> {
							process( tableInformation, resultSet, builders );
							return null;
						}
				);
			}
		}
		catch (SQLException e) {
			throw convertSQLException( e,
					"Error accessing column metadata: "
							+ tableInformation.getName() );
		}

		final List<ForeignKeyInformation> foreignKeys = new ArrayList<>( builders.size() );
		for ( var foreignKeyBuilder : builders.values() ) {
			foreignKeys.add( foreignKeyBuilder.build() );
		}
		return foreignKeys;
	}

	@Override
	public NameSpaceForeignKeysInformation getForeignKeys(Identifier catalog, Identifier schema) {
		if ( !supportsBulkForeignKeyRetrieval() ) {
			throw new UnsupportedOperationException( "Database doesn't support extracting all foreign keys at once" );
		}
		else {
			try {
				return processImportedKeysResultSet(
						catalog == null ? "" : catalog.getText(),
						schema == null ? "" : schema.getText(),
						null,
						this::extractNameSpaceForeignKeysInformation
				);
			}
			catch (SQLException e) {
				throw convertSQLException( e,
						"Error while reading foreign key information for namespace "
						+ new Namespace.Name( catalog, schema ) );
			}
		}
	}

	protected NameSpaceForeignKeysInformation extractNameSpaceForeignKeysInformation(ResultSet resultSet)
			throws SQLException {
		final var foreignKeysInformation = new NameSpaceForeignKeysInformation( getIdentifierHelper() );

		while ( resultSet.next() ) {
			final TableInformation tableInformation = getTableInformation(
					resultSet.getString( getResultSetForeignKeyCatalogLabel() ),
					resultSet.getString( getResultSetForeignKeySchemaLabel() ),
					resultSet.getString( getResultSetForeignKeyTableLabel() )
			);
			final Identifier foreignKeyIdentifier =
					toIdentifier( resultSet.getString( getResultSetForeignKeyLabel() ) );
			final var foreignKey = getOrCreateForeignKeyInformation( foreignKeysInformation, foreignKeyIdentifier, tableInformation );
			final var primaryKeyTableInformation =
					extractionContext.getDatabaseObjectAccess()
							.locateTableInformation( extractPrimaryKeyTableName( resultSet ) );
			if ( primaryKeyTableInformation != null ) {
				// the assumption here is that we have not seen this table already based on fully-qualified name
				// during previous step of building all table metadata so most likely this is
				// not a match based solely on schema/catalog and that another row in this result set
				// should match.
				final Identifier foreignKeyColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetForeignKeyColumnNameLabel() ) );
				final Identifier pkColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetPrimaryKeyColumnNameLabel() ) );
				((List<ForeignKeyInformation.ColumnReferenceMapping>) foreignKey.getColumnReferenceMappings()).add(
						new ColumnReferenceMappingImpl(
							tableInformation.getColumn( foreignKeyColumnIdentifier ),
							primaryKeyTableInformation.getColumn( pkColumnIdentifier )
						)
				);
			}
		}
		return foreignKeysInformation;
	}

	private ForeignKeyInformation getOrCreateForeignKeyInformation(
			NameSpaceForeignKeysInformation foreignKeysInformation,
			Identifier foreignKeyIdentifier,
			TableInformation tableInformation) {
		final List<ForeignKeyInformation> foreignKeys =
				foreignKeysInformation.getForeignKeysInformation( tableInformation.getName().getTableName().getText() );
		if ( foreignKeys != null ) {
			for ( ForeignKeyInformation foreignKey : foreignKeys ) {
				if ( foreignKeyIdentifier.equals( foreignKey.getForeignKeyIdentifier() ) ) {
					return foreignKey;
				}
			}
		}
		final var foreignKeyInformation = new ForeignKeyInformationImpl( foreignKeyIdentifier, new ArrayList<>() );
		foreignKeysInformation.addForeignKeyInformation( tableInformation, foreignKeyInformation );
		return foreignKeyInformation;
	}

	private void process(
			TableInformation tableInformation,
			ResultSet resultSet,
			Map<Identifier, ForeignKeyBuilder> fkBuilders)
					throws SQLException {
		while ( resultSet.next() ) {
			// IMPL NOTE: The builder is mainly used to collect the column reference mappings
			final Identifier foreignKeyIdentifier =
					toIdentifier( resultSet.getString( getResultSetForeignKeyLabel() ) );
			final var foreignKeyBuilder = getForeignKeyBuilder( fkBuilders, foreignKeyIdentifier );
			final var primaryKeyTableInformation =
					extractionContext.getDatabaseObjectAccess()
							.locateTableInformation( extractPrimaryKeyTableName( resultSet ) );
			if ( primaryKeyTableInformation != null ) {
				// the assumption here is that we have not seen this table already based on fully-qualified name
				// during previous step of building all table metadata so most likely this is
				// not a match based solely on schema/catalog and that another row in this result set
				// should match.
				final Identifier foreignKeyColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetForeignKeyColumnNameLabel() ) );
				final Identifier pkColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetPrimaryKeyColumnNameLabel() ) );
				foreignKeyBuilder.addColumnMapping(
						tableInformation.getColumn( foreignKeyColumnIdentifier ),
						primaryKeyTableInformation.getColumn( pkColumnIdentifier )
				);
			}

		}
	}

	private ForeignKeyBuilder getForeignKeyBuilder(
			Map<Identifier, ForeignKeyBuilder> builders, Identifier foreignKeyIdentifier) {
		var foreignKeyBuilder = builders.get( foreignKeyIdentifier );
		if ( foreignKeyBuilder == null ) {
			foreignKeyBuilder = generateForeignKeyBuilder( foreignKeyIdentifier );
			builders.put( foreignKeyIdentifier, foreignKeyBuilder );
		}
		return foreignKeyBuilder;
	}

	private ForeignKeyBuilder generateForeignKeyBuilder(Identifier fkIdentifier) {
		return new ForeignKeyBuilderImpl( fkIdentifier );
	}

	protected interface ForeignKeyBuilder {
		ForeignKeyBuilder addColumnMapping(ColumnInformation referencing, ColumnInformation referenced);
		ForeignKeyInformation build();
	}

	protected static class ForeignKeyBuilderImpl implements ForeignKeyBuilder {
		private final Identifier foreignKeyIdentifier;
		private final List<ForeignKeyInformation.ColumnReferenceMapping> columnMappingList = new ArrayList<>();

		public ForeignKeyBuilderImpl(Identifier foreignKeyIdentifier) {
			this.foreignKeyIdentifier = foreignKeyIdentifier;
		}

		@Override
		public ForeignKeyBuilder addColumnMapping(ColumnInformation referencing, ColumnInformation referenced) {
			columnMappingList.add( new ColumnReferenceMappingImpl( referencing, referenced ) );
			return this;
		}

		@Override
		public ForeignKeyInformationImpl build() {
			if ( columnMappingList.isEmpty() ) {
				throw new SchemaManagementException(
						"Attempt to resolve foreign key metadata from JDBC metadata failed to find " +
						"column mappings for foreign key named [" + foreignKeyIdentifier.getText() + "]"
				);
			}
			return new ForeignKeyInformationImpl( foreignKeyIdentifier, columnMappingList );
		}
	}

	private QualifiedTableName extractPrimaryKeyTableName(ResultSet resultSet) throws SQLException {
		return new QualifiedTableName(
				toIdentifier( resultSet.getString( getResultSetPrimaryKeyCatalogLabel() ) ),
				toIdentifier( resultSet.getString( getResultSetPrimaryKeySchemaLabel() ) ),
				toIdentifier( resultSet.getString( getResultSetPrimaryKeyTableLabel() ) ) );
	}

	private QualifiedTableName extractTableName(ResultSet resultSet) throws SQLException {
		return new QualifiedTableName(
				toIdentifier( resultSet.getString( getResultSetCatalogLabel() ) ),
				toIdentifier( resultSet.getString( getResultSetSchemaLabel() ) ),
				toIdentifier( resultSet.getString( getResultSetTableNameLabel() ) )
		);
	}

	@Override
	public boolean supportsBulkPrimaryKeyRetrieval() {
		return false;
	}

	@Override
	public boolean supportsBulkForeignKeyRetrieval() {
		return false;
	}

	@Override
	public boolean supportsBulkIndexRetrieval() {
		return false;
	}

}
