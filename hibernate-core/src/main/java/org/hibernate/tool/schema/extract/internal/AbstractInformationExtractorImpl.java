/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

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

import org.hibernate.JDBCException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.SchemaExtractionException;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import static java.util.Collections.addAll;
import static org.hibernate.boot.model.naming.DatabaseIdentifier.toIdentifier;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.splitTrimmingTokens;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

public abstract class AbstractInformationExtractorImpl implements InformationExtractor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractInformationExtractorImpl.class );

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

		final ConfigurationService configService =
				extractionContext.getServiceRegistry().requireService( ConfigurationService.class );

		useJdbcMetadataDefaultsSetting = configService.getSetting(
				"hibernate.temp.use_jdbc_metadata_defaults",
				StandardConverters.BOOLEAN,
				Boolean.TRUE
		);

		final String extraPhysicalTableTypesConfig = configService.getSetting(
				AvailableSettings.EXTRA_PHYSICAL_TABLE_TYPES,
				StandardConverters.STRING,
				configService.getSetting(
						AvailableSettings.EXTRA_PHYSICAL_TABLE_TYPES,
						StandardConverters.STRING,
						""
				)
		);
		final Dialect dialect = extractionContext.getJdbcEnvironment().getDialect();
		this.extraPhysicalTableTypes = getPhysicalTableTypes( extraPhysicalTableTypesConfig, dialect );
		this.tableTypes = getTableTypes( configService, dialect );
	}

	private String[] getPhysicalTableTypes(String extraPhysicalTableTypesConfig, Dialect dialect) {
		final List<String> physicalTableTypesList = new ArrayList<>();
		if ( !isBlank( extraPhysicalTableTypesConfig ) ) {
			addAll( physicalTableTypesList, splitTrimmingTokens( ",;", extraPhysicalTableTypesConfig, false ) );
		}
		dialect.augmentPhysicalTableTypes( physicalTableTypesList );
		return physicalTableTypesList.toArray( EMPTY_STRINGS );
	}

	private String[] getTableTypes(ConfigurationService configService, Dialect dialect) {
		final List<String> tableTypesList = new ArrayList<>();
		tableTypesList.add( "TABLE" );
		tableTypesList.add( "VIEW" );
		if ( getBoolean( AvailableSettings.ENABLE_SYNONYMS, configService.getSettings() ) ) {
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
		final String catalogFilter =
				getIdentifierHelper()
						.toMetaDataCatalogName( catalog == null ? extractionContext.getDefaultCatalog() : catalog );
		final String schemaFilter =
				getIdentifierHelper()
						.toMetaDataSchemaName( schema == null ? extractionContext.getDefaultSchema() : schema );
		try {
			return processSchemaResultSet(
					catalogFilter,
					schemaFilter,
					resultSet -> {

						if ( !resultSet.next() ) {
							return false;
						}

						if ( resultSet.next() ) {
							final String catalogName = catalog == null ? "" : catalog.getCanonicalName();
							final String schemaName = schema == null ? "" : schema.getCanonicalName();

							LOG.debugf(
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
				final TableInformation tableInfo =
						locateTableInNamespace( currentCatalog, currentSchema, tableName );
				if ( tableInfo != null ) {
					return tableInfo;
				}
			}

			// 2) look in default namespace
			final Identifier defaultCatalog = extractionContext.getDefaultCatalog();
			final Identifier defaultSchema = extractionContext.getDefaultSchema();
			if ( defaultCatalog != null
					|| defaultSchema != null ) {
				final TableInformation tableInfo =
						locateTableInNamespace( defaultCatalog, defaultSchema, tableName );
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
		if ( currentSchema != null ) {
			return currentSchema;
		}
		final Identifier schema = getJdbcEnvironment().getCurrentSchema();
		if ( schema != null ) {
			currentSchema = schema;
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentSchema = getIdentifierHelper()
						.toIdentifier( extractionContext.getJdbcConnection().getSchema() );
			}
			catch (SQLException sqle) {
				LOG.sqlWarning( sqle.getErrorCode(), sqle.getSQLState() );
			}
			catch (AbstractMethodError ignore) {
				// jConnect and jTDS report that they "support" schemas, but they don't really
			}
		}
		return currentSchema;
	}

	private Identifier getCurrentCatalog() {
		if ( getNameQualifierSupport() == NameQualifierSupport.SCHEMA ) {
			return null;
		}
		if ( currentCatalog != null ) {
			return currentCatalog;
		}
		final Identifier catalog = getJdbcEnvironment().getCurrentCatalog();
		if ( catalog != null ) {
			currentCatalog = catalog;
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentCatalog = getIdentifierHelper()
						.toIdentifier( extractionContext.getJdbcConnection().getCatalog() );
			}
			catch (SQLException sqle) {
				LOG.sqlWarning( sqle.getErrorCode(), sqle.getSQLState() );
			}
		}
		return currentCatalog;
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
				currentCatalogFilter = extractionContext.getJdbcConnection().getCatalog();
			}
			catch (SQLException sqle) {
				LOG.sqlWarning( sqle.getErrorCode(), sqle.getSQLState() );
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
				currentSchemaFilter = extractionContext.getJdbcConnection().getSchema();
			}
			catch (SQLException sqle) {
				LOG.sqlWarning( sqle.getErrorCode(), sqle.getSQLState() );
			}
			catch (AbstractMethodError ignore) {
				// jConnect and jTDS report that they "support" schemas, but they don't really
			}
		}
		return currentSchemaFilter;
	}

	@Override
	public NameSpaceTablesInformation getTables(Identifier catalog, Identifier schema) {

		final String catalogFilter;
		final String schemaFilter;

		final NameQualifierSupport nameQualifierSupport = getNameQualifierSupport();

		if ( nameQualifierSupport.supportsCatalogs() ) {
			if ( catalog == null ) {
				// look in the current namespace
				final String currentCatalogFilter = getCurrentCatalogFilter( getJdbcEnvironment() );
				if ( currentCatalogFilter != null ) {
					catalogFilter = currentCatalogFilter;
				}
				else {
					if ( extractionContext.getDefaultCatalog() != null ) {
						// 2) look in default namespace
						catalogFilter = toMetaDataObjectName( extractionContext.getDefaultCatalog() );
					}
					else {
						catalogFilter = null;
					}
				}
			}
			else {
				catalogFilter = toMetaDataObjectName( catalog );
			}
		}
		else {
			catalogFilter = null;
		}

		if ( nameQualifierSupport.supportsSchemas() ) {
			if ( schema == null ) {
				// 1) look in current namespace
				final String currentSchemaFilter = getCurrentSchemaFilter( getJdbcEnvironment() );
				if ( currentSchemaFilter != null ) {
					schemaFilter = currentSchemaFilter;
				}
				else {
					if ( extractionContext.getDefaultSchema() != null ) {
						// 2) look in default namespace
						schemaFilter = toMetaDataObjectName( extractionContext.getDefaultSchema() );
					}
					else {
						schemaFilter = null;
					}
				}
			}
			else {
				schemaFilter = toMetaDataObjectName( schema );
			}
		}
		else {
			schemaFilter = null;
		}

		try {
			return processTableResultSet(
					catalogFilter,
					schemaFilter,
					"%",
					tableTypes,
					resultSet -> {
						final NameSpaceTablesInformation tablesInformation =
								extractNameSpaceTablesInformation( resultSet );
						populateTablesWithColumns( catalogFilter, schemaFilter, tablesInformation );
						return tablesInformation;
					} );
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
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
		final NameSpaceTablesInformation tables = new NameSpaceTablesInformation( getIdentifierHelper() );
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
		final Identifier catalogToUse;
		final Identifier schemaToUse;

		final String catalogFilter;
		final String schemaFilter;

		final NameQualifierSupport nameQualifierSupport = getNameQualifierSupport();

		if ( nameQualifierSupport.supportsCatalogs() ) {
			if ( catalog == null ) {
				String defaultCatalog;
				try {
					defaultCatalog = extractionContext.getJdbcConnection().getCatalog();
				}
				catch (SQLException ignore) {
					defaultCatalog = "";
				}
				catalogToUse = null;
				catalogFilter = defaultCatalog;
			}
			else {
				catalogToUse = catalog;
				catalogFilter = toMetaDataObjectName( catalog );
			}
		}
		else {
			catalogToUse = null;
			catalogFilter = null;
		}

		if ( nameQualifierSupport.supportsSchemas() ) {
			if ( schema == null ) {
				schemaToUse = null;
				schemaFilter = "";
			}
			else {
				schemaToUse = schema;
				schemaFilter = toMetaDataObjectName( schema );
			}
		}
		else {
			schemaToUse = null;
			schemaFilter = null;
		}

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

	private NameQualifierSupport getNameQualifierSupport() {
		return getJdbcEnvironment().getNameQualifierSupport();
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
					LOG.multipleTablesFound( tableName.render() );
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
			LOG.tableNotFound( tableName.render() );
		}
		return tableInformation;
	}

	protected abstract String getResultSetTableTypesPhysicalTableConstant();

	protected boolean isPhysicalTableType(String tableType) {
		if ( extraPhysicalTableTypes == null ) {
			return getResultSetTableTypesPhysicalTableConstant().equalsIgnoreCase( tableType );
		}
		else {
			if ( getResultSetTableTypesPhysicalTableConstant().equalsIgnoreCase( tableType ) ) {
				return true;
			}
			for ( String extraPhysicalTableType : extraPhysicalTableTypes ) {
				if ( extraPhysicalTableType.equalsIgnoreCase( tableType ) ) {
					return true;
				}
			}
			return false;
		}
	}

	protected void addColumns(TableInformation tableInformation) {
		final QualifiedTableName tableName = tableInformation.getName();
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

	@Override
	public PrimaryKeyInformation getPrimaryKey(TableInformationImpl tableInformation) {
		final QualifiedTableName tableName = tableInformation.getName();
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

		final List<ColumnInformation> pkColumns = new ArrayList<>();
		boolean firstPass = true;
		Identifier pkIdentifier = null;

		while ( resultSet.next() ) {
			final String currentPkName = resultSet.getString( getResultSetPrimaryKeyNameLabel() );
			final Identifier currentPkIdentifier =
					currentPkName == null ? null : toIdentifier( currentPkName );
			if ( firstPass ) {
				pkIdentifier = currentPkIdentifier;
				firstPass = false;
			}
			else {
				if ( !Objects.equals( pkIdentifier, currentPkIdentifier ) ) {
					throw new SchemaExtractionException( "Encountered primary keys differing name on table "
							+ tableInformation.getName().toString() );
				}
			}

			final int columnPosition = resultSet.getInt( getResultSetColumnPositionColumn() );
			final int index = columnPosition - 1;
			// Fill up the array list with nulls up to the desired index, because some JDBC drivers don't return results ordered by column position
			while ( pkColumns.size() <= index ) {
				pkColumns.add( null );
			}
			final Identifier columnIdentifier =
					toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
			pkColumns.set( index, tableInformation.getColumn( columnIdentifier ) );
		}
		if ( firstPass ) {
			// we did not find any results (no pk)
			return null;
		}
		else {
			// validate column list is properly contiguous
			for ( int i = 0; i < pkColumns.size(); i++ ) {
				if ( pkColumns.get( i ) == null ) {
					throw new SchemaExtractionException( "Primary Key information was missing for KEY_SEQ = " + ( i+1) );
				}
			}
			// build the return
			return new PrimaryKeyInformationImpl( pkIdentifier, pkColumns );
		}
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
			String table,
			boolean unique,
			boolean approximate,
			ExtractionContext.ResultSetProcessor<T> processor)
					throws SQLException;

	@Override
	public Iterable<IndexInformation> getIndexes(TableInformation tableInformation) {
		final QualifiedTableName tableName = tableInformation.getName();
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
								IndexInformationImpl.Builder builder = builders.get( indexIdentifier );
								if ( builder == null ) {
									builder = IndexInformationImpl.builder( indexIdentifier );
									builders.put( indexIdentifier, builder );
								}

								final Identifier columnIdentifier =
										toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) );
								final ColumnInformation columnInformation =
										tableInformation.getColumn( columnIdentifier );
								if ( columnInformation == null ) {
									// See HHH-10191: this may happen when dealing with Oracle/PostgreSQL function indexes
									LOG.logCannotLocateIndexColumnInformation(
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

		final List<IndexInformation> indexes = new ArrayList<>();
		for ( IndexInformationImpl.Builder builder : builders.values() ) {
			IndexInformationImpl index = builder.build();
			indexes.add( index );
		}
		return indexes;
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
			String table,
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
		final QualifiedTableName tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();

		final String catalogFilter = catalog == null ? "" : catalog.getText();
		final String schemaFilter = schema == null ? "" : schema.getText();

		final Map<Identifier, ForeignKeyBuilder> fkBuilders = new HashMap<>();
		try {
			final String table = tableInformation.getName().getTableName().getText();
			processImportedKeysResultSet( catalogFilter, schemaFilter, table,
					resultSet -> {
						process( tableInformation, resultSet, fkBuilders );
						return null;
					} );
			final Dialect dialect = getJdbcEnvironment().getDialect();
			if ( dialect.useCrossReferenceForeignKeys() ) {
				processCrossReferenceResultSet(
						null,
						null,
						dialect.getCrossReferenceParentTableFilter(),
						catalogFilter,
						schemaFilter,
						table,
						resultSet -> {
							process( tableInformation, resultSet, fkBuilders );
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

		final List<ForeignKeyInformation> fks = new ArrayList<>();
		for ( ForeignKeyBuilder fkBuilder : fkBuilders.values() ) {
			ForeignKeyInformation fk = fkBuilder.build();
			fks.add( fk );
		}
		return fks;
	}

	private void process(
			TableInformation tableInformation,
			ResultSet resultSet,
			Map<Identifier, ForeignKeyBuilder> fkBuilders)
					throws SQLException {
		while ( resultSet.next() ) {
			// IMPL NOTE : The builder is mainly used to collect the column reference mappings
			final Identifier fkIdentifier = toIdentifier( resultSet.getString( getResultSetForeignKeyLabel() ) );
			ForeignKeyBuilder fkBuilder = fkBuilders.get( fkIdentifier );
			if ( fkBuilder == null ) {
				fkBuilder = generateForeignKeyBuilder( fkIdentifier );
				fkBuilders.put( fkIdentifier, fkBuilder );
			}

			final TableInformation pkTableInformation = extractionContext.getDatabaseObjectAccess()
					.locateTableInformation( extractPrimaryKeyTableName( resultSet ) );
			if ( pkTableInformation != null ) {
				// the assumption here is that we have not seen this table already based on fully-qualified name
				// during previous step of building all table metadata so most likely this is
				// not a match based solely on schema/catalog and that another row in this result set
				// should match.
				final Identifier fkColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetForeignKeyColumnNameLabel() ) );
				final Identifier pkColumnIdentifier =
						toIdentifier( resultSet.getString( getResultSetPrimaryKeyColumnNameLabel() ) );
				fkBuilder.addColumnMapping(
						tableInformation.getColumn( fkColumnIdentifier ),
						pkTableInformation.getColumn( pkColumnIdentifier )
				);
			}

		}
	}

	private ForeignKeyBuilder generateForeignKeyBuilder(Identifier fkIdentifier) {
		return new ForeignKeyBuilderImpl( fkIdentifier );
	}

	protected interface ForeignKeyBuilder {
		ForeignKeyBuilder addColumnMapping(ColumnInformation referencing, ColumnInformation referenced);

		ForeignKeyInformation build();
	}

	protected static class ForeignKeyBuilderImpl implements ForeignKeyBuilder {
		private final Identifier fkIdentifier;
		private final List<ForeignKeyInformation.ColumnReferenceMapping> columnMappingList = new ArrayList<>();

		public ForeignKeyBuilderImpl(Identifier fkIdentifier) {
			this.fkIdentifier = fkIdentifier;
		}

		@Override
		public ForeignKeyBuilder addColumnMapping(ColumnInformation referencing, ColumnInformation referenced) {
			columnMappingList.add( new ForeignKeyInformationImpl.ColumnReferenceMappingImpl( referencing, referenced ) );
			return this;
		}

		@Override
		public ForeignKeyInformationImpl build() {
			if ( columnMappingList.isEmpty() ) {
				throw new SchemaManagementException(
						"Attempt to resolve foreign key metadata from JDBC metadata failed to find " +
								"column mappings for foreign key named [" + fkIdentifier.getText() + "]"
				);
			}
			return new ForeignKeyInformationImpl( fkIdentifier, columnMappingList );
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

}
