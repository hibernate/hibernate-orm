/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.hibernate.JDBCException;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
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

public abstract class AbstractInformationExtractorImpl implements InformationExtractor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractInformationExtractorImpl.class );

	private final String[] tableTypes;

	private String[] extraPhysicalTableTypes;

	private final ExtractionContext extractionContext;

	private final boolean useJdbcMetadataDefaultsSetting;

	private Identifier currentCatalog;
	private Identifier currentSchema;

	private String currentCatalogFilter;
	private String currentSchemaFilter;


	public AbstractInformationExtractorImpl(ExtractionContext extractionContext) {
		this.extractionContext = extractionContext;

		ConfigurationService configService = extractionContext.getServiceRegistry()
				.getService( ConfigurationService.class );

		useJdbcMetadataDefaultsSetting = configService.getSetting(
				"hibernate.temp.use_jdbc_metadata_defaults",
				StandardConverters.BOOLEAN,
				Boolean.TRUE
		);

		final String extraPhysicalTableTypesConfig = configService.getSetting(
				AvailableSettings.EXTRA_PHYSICAL_TABLE_TYPES,
				StandardConverters.STRING,
				configService.getSetting(
						AvailableSettings.DEPRECATED_EXTRA_PHYSICAL_TABLE_TYPES,
						StandardConverters.STRING,
						""
				)
		);
		if ( ! StringHelper.isBlank( extraPhysicalTableTypesConfig ) ) {
			this.extraPhysicalTableTypes = StringHelper.splitTrimmingTokens(
					",;",
					extraPhysicalTableTypesConfig,
					false
			);
		}

		final List<String> tableTypesList = new ArrayList<>();
		tableTypesList.add( "TABLE" );
		tableTypesList.add( "VIEW" );
		if ( ConfigurationHelper.getBoolean( AvailableSettings.ENABLE_SYNONYMS, configService.getSettings(), false ) ) {
			tableTypesList.add( "SYNONYM" );
		}
		if ( extraPhysicalTableTypes != null ) {
			Collections.addAll( tableTypesList, extraPhysicalTableTypes );
		}
		extractionContext.getJdbcEnvironment().getDialect().augmentRecognizedTableTypes( tableTypesList );

		this.tableTypes = tableTypesList.toArray( new String[ tableTypesList.size() ] );
	}

	protected IdentifierHelper identifierHelper() {
		return extractionContext.getJdbcEnvironment().getIdentifierHelper();
	}

	protected JDBCException convertSQLException(SQLException sqlException, String message) {
		return extractionContext.getJdbcEnvironment().getSqlExceptionHelper().convert( sqlException, message );
	}

	protected String toMetaDataObjectName(Identifier identifier) {
		return extractionContext.getJdbcEnvironment().getIdentifierHelper().toMetaDataObjectName( identifier );
	}

	protected ExtractionContext getExtractionContext() {
		return extractionContext;
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
			});
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
	 * <p/>
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
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException;

	@Override
	public boolean schemaExists(Identifier catalog, Identifier schema) {
		final String catalogFilter = determineCatalogFilter( catalog );
		final String schemaFilter = determineSchemaFilter( schema );

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

	protected String determineCatalogFilter(Identifier catalog) {
		Identifier identifierToUse = catalog;
		if ( identifierToUse == null ) {
			identifierToUse = extractionContext.getDefaultCatalog();
		}

		return extractionContext.getJdbcEnvironment().getIdentifierHelper().toMetaDataCatalogName( identifierToUse );
	}

	protected String determineSchemaFilter(Identifier schema) {
		Identifier identifierToUse = schema;
		if ( identifierToUse == null ) {
			identifierToUse = extractionContext.getDefaultSchema();
		}

		return extractionContext.getJdbcEnvironment().getIdentifierHelper().toMetaDataSchemaName( identifierToUse );
	}

	private TableInformation extractTableInformation(ResultSet resultSet) throws SQLException {
		final QualifiedTableName tableName = extractTableName( resultSet );

		final TableInformationImpl tableInformation = new TableInformationImpl(
				this,
				identifierHelper(),
				tableName,
				isPhysicalTableType( resultSet.getString( getResultSetTableTypeLabel() ) ),
				resultSet.getString( getResultSetRemarksLabel() )
		);
		return tableInformation;
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

			TableInformation tableInfo;

			// 1) look in current namespace
			final JdbcEnvironment jdbcEnvironment = extractionContext.getJdbcEnvironment();
			final Identifier currentSchema = getCurrentSchema( jdbcEnvironment );
			final Identifier currentCatalog = getCurrentCatalog( jdbcEnvironment );
			if ( currentCatalog != null
					|| currentSchema != null ) {
				tableInfo = locateTableInNamespace(
						currentCatalog,
						currentSchema,
						tableName
				);

				if ( tableInfo != null ) {
					return tableInfo;
				}
			}

			// 2) look in default namespace
			if ( extractionContext.getDefaultCatalog() != null || extractionContext.getDefaultSchema() != null ) {
				tableInfo = locateTableInNamespace(
						extractionContext.getDefaultCatalog(),
						extractionContext.getDefaultSchema(),
						tableName
				);

				if ( tableInfo != null ) {
					return tableInfo;
				}
			}

			// 3) look in all namespaces
			try {
				final String tableNameFilter = toMetaDataObjectName( tableName );

				return processTableResultSet(
						null,
						null,
						tableNameFilter,
						tableTypes,
						resultSet -> extractTableInformation(
								null,
								null,
								tableName,
								resultSet
						)
				);
			}
			catch (SQLException sqlException) {
				throw convertSQLException( sqlException, "Error accessing table metadata" );
			}
		}
	}

	private Identifier getCurrentSchema(JdbcEnvironment jdbcEnvironment) {
		if ( jdbcEnvironment.getNameQualifierSupport() == NameQualifierSupport.CATALOG ) {
			return null;
		}
		if ( currentSchema != null ) {
			return currentSchema;
		}
		final Identifier schema = jdbcEnvironment.getCurrentSchema();
		if ( schema != null ) {
			currentSchema = schema;
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentSchema = extractionContext.getJdbcEnvironment()
						.getIdentifierHelper()
						.toIdentifier( extractionContext.getJdbcConnection().getSchema() );
			}
			catch (SQLException ignore) {
				LOG.sqlWarning( ignore.getErrorCode(), ignore.getSQLState() );
			}
		}
		return currentSchema;
	}

	private Identifier getCurrentCatalog(JdbcEnvironment jdbcEnvironment) {
		if ( jdbcEnvironment.getNameQualifierSupport() == NameQualifierSupport.SCHEMA ) {
			return null;
		}
		if ( currentCatalog != null ) {
			return currentCatalog;
		}
		final Identifier catalog = jdbcEnvironment.getCurrentCatalog();
		if ( catalog != null ) {
			currentCatalog = catalog;
		}
		if ( !useJdbcMetadataDefaultsSetting ) {
			try {
				currentCatalog = extractionContext.getJdbcEnvironment()
						.getIdentifierHelper()
						.toIdentifier( extractionContext.getJdbcConnection().getCatalog() );
			}
			catch (SQLException ignore) {
				LOG.sqlWarning( ignore.getErrorCode(), ignore.getSQLState() );
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
			catch (SQLException ignore) {
				LOG.sqlWarning( ignore.getErrorCode(), ignore.getSQLState() );
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
			catch (SQLException ignore) {
				LOG.sqlWarning( ignore.getErrorCode(), ignore.getSQLState() );
			}
		}
		return currentSchemaFilter;
	}

	@Override
	public NameSpaceTablesInformation getTables(Identifier catalog, Identifier schema) {

		final String catalogFilter;
		final String schemaFilter;

		final JdbcEnvironment jdbcEnvironment = extractionContext.getJdbcEnvironment();
		final NameQualifierSupport nameQualifierSupport = jdbcEnvironment.getNameQualifierSupport();
		if ( nameQualifierSupport.supportsCatalogs() ) {
			if ( catalog == null ) {
				// look in the current namespace
				final String currentCatalogFilter = getCurrentCatalogFilter(jdbcEnvironment);
				if ( currentCatalogFilter != null ) {
					catalogFilter = currentCatalogFilter;
				}
				else {
					if ( extractionContext.getDefaultCatalog() != null ) {
						// 2) look in default namespace
						catalogFilter = toMetaDataObjectName( extractionContext.getDefaultCatalog() );
					}
					else {
						catalogFilter = "";
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
				final String currentSchemaFilter = getCurrentSchemaFilter( jdbcEnvironment );
				if ( currentSchemaFilter != null ) {
					schemaFilter = currentSchemaFilter;
				}
				else {
					if ( extractionContext.getDefaultSchema() != null ) {
						// 2) look in default namespace
						schemaFilter = toMetaDataObjectName( extractionContext.getDefaultSchema() );
					}
					else {
						schemaFilter = "";
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
						final NameSpaceTablesInformation tablesInformation = extractNameSpaceTablesInformation( resultSet );
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
	 * <p/>
	 * The {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 * and {@code columnNamePattern} parameters are as
	 * specified by {@link DatabaseMetaData#getColumns(String, String, String, String)},
	 * and are copied here:
	 * <p/>
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
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException;

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
								addExtractedColumnInformation( currentTable, resultSet );
							}
						}
						return null;
					}
			);
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing tables metadata"
			);
		}
	}

	protected void addExtractedColumnInformation(TableInformation tableInformation, ResultSet resultSet)
			throws SQLException {
		final ColumnInformation columnInformation = new ColumnInformationImpl(
				tableInformation,
				DatabaseIdentifier.toIdentifier( resultSet.getString( getResultSetColumnNameLabel() ) ),
				resultSet.getInt( getResultSetSqlTypeCodeLabel() ),
				new StringTokenizer( resultSet.getString( getResultSetTypeNameLabel() ), "() " ).nextToken(),
				resultSet.getInt( getResultSetColumnSizeLabel() ),
				resultSet.getInt( getResultSetDecimalDigitsLabel() ),
				interpretTruthValue( resultSet.getString( getResultSetIsNullableLabel() ) )
		);
		tableInformation.addColumn( columnInformation );
	}

	private NameSpaceTablesInformation extractNameSpaceTablesInformation(ResultSet resultSet) throws SQLException {
		NameSpaceTablesInformation tables = new NameSpaceTablesInformation(identifierHelper());
		while ( resultSet.next() ) {
			final TableInformation tableInformation = extractTableInformation( resultSet );
			tables.addTableInformation( tableInformation );
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
	 * <p/>
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
			ExtractionContext.ResultSetProcessor<T> processor
	) throws SQLException;

	private TableInformation locateTableInNamespace(
			Identifier catalog,
			Identifier schema,
			Identifier tableName) {
		final Identifier catalogToUse;
		final Identifier schemaToUse;

		final String catalogFilter;
		final String schemaFilter;

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsCatalogs() ) {
			if ( catalog == null ) {
				String defaultCatalog = "";
				if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsCatalogs() ) {
					try {
						defaultCatalog = extractionContext.getJdbcConnection().getCatalog();
					}
					catch (SQLException ignore) {
					}
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

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsSchemas() ) {
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
					resultSet -> extractTableInformation(
							catalogToUse,
							schemaToUse,
							tableName,
							resultSet
					)
			);

		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	private TableInformation extractTableInformation(
			Identifier catalog,
			Identifier schema,
			Identifier tableName,
			ResultSet resultSet) throws SQLException {

		boolean found = false;
		TableInformation tableInformation = null;

		while ( resultSet.next() ) {

			if ( tableName.equals( Identifier.toIdentifier(
					resultSet.getString( getResultSetTableNameLabel() ),
					tableName.isQuoted()
			) ) ) {
				if ( found ) {
					LOG.multipleTablesFound( tableName.render() );
					final String catalogName = catalog == null ? "" : catalog.render();
					final String schemaName = schema == null ? "" : schema.render();
					throw new SchemaExtractionException(
							String.format(
									Locale.ENGLISH,
									"More than one table found in namespace (%s, %s) : %s",
									catalogName,
									schemaName,
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

		final String catalogFilter = catalog == null ? "" : catalog.getText();
		final String schemaFilter = schema == null ? "" : schema.getText();

		try {
			processColumnsResultSet(
					catalogFilter,
					schemaFilter,
					tableName.getTableName().getText(),
					"%",
					resultSet -> {

						while ( resultSet.next() ) {
							addExtractedColumnInformation( tableInformation, resultSet );
						}
						return null;
					}
			);

		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing tables metadata"
			);
		}
	}

	protected TruthValue interpretNullable(int nullable) {
		switch ( nullable ) {
			case ResultSetMetaData.columnNullable:
				return TruthValue.TRUE;
			case ResultSetMetaData.columnNoNulls:
				return TruthValue.FALSE;
			default:
				return TruthValue.UNKNOWN;
		}
	}

	private TruthValue interpretTruthValue(String nullable) {
		if ( "yes".equalsIgnoreCase( nullable ) ) {
			return TruthValue.TRUE;
		}
		else if ( "no".equalsIgnoreCase( nullable ) ) {
			return TruthValue.FALSE;
		}
		return TruthValue.UNKNOWN;
	}

	// This method is not currently used.
	protected abstract <T> T processPrimaryKeysResultSet(
			String catalogFilter,
			String schemaFilter,
			Identifier tableName,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException;

	@Override
	public PrimaryKeyInformation getPrimaryKey(TableInformationImpl tableInformation) {
		final QualifiedTableName tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();

		final String catalogFilter;
		final String schemaFilter;

		if ( catalog == null ) {
			catalogFilter = "";
		}
		else {
			catalogFilter = catalog.getText();
		}

		if ( schema == null ) {
			schemaFilter = "";
		}
		else {
			schemaFilter = schema.getText();
		}

		try {
			return processPrimaryKeysResultSet(
					catalogFilter,
					schemaFilter,
					tableInformation.getName().getTableName(),
					resultSet -> extractPrimaryKeyInformation( tableInformation, resultSet )
			);
		}
		catch (SQLException e) {
			throw convertSQLException( e, "Error while reading primary key meta data for " + tableInformation.getName().toString() );
		}
	}

	private PrimaryKeyInformation extractPrimaryKeyInformation(
			TableInformation tableInformation,
			ResultSet resultSet
	) throws SQLException {

		final List<ColumnInformation> pkColumns = new ArrayList<>();
		boolean firstPass = true;
		Identifier pkIdentifier = null;

		while ( resultSet.next() ) {
			final String currentPkName = resultSet.getString( getResultSetPrimaryKeyNameLabel() );
			final Identifier currentPkIdentifier = currentPkName == null
					? null
					: DatabaseIdentifier.toIdentifier( currentPkName );
			if ( firstPass ) {
				pkIdentifier = currentPkIdentifier;
				firstPass = false;
			}
			else {
				if ( !Objects.equals( pkIdentifier, currentPkIdentifier ) ) {
					throw new SchemaExtractionException(
							String.format(
									"Encountered primary keys differing name on table %s",
									tableInformation.getName().toString()
							)
					);
				}
			}

			final int columnPosition = resultSet.getInt( getResultSetColumnPositionColumn() );

			final Identifier columnIdentifier = DatabaseIdentifier.toIdentifier(
					resultSet.getString( getResultSetColumnNameLabel() )
			);
			final ColumnInformation column = tableInformation.getColumn( columnIdentifier );
			pkColumns.add( columnPosition-1, column );
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
	 *                 NOTE: Hibernate ignores statistics and does not
	 *                       care about the actual type of index.
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
	 * <p/>
	 * The {@code catalog}, {@code schemaPattern}, {@code tableNamePattern},
	 * and {@code columnNamePattern} parameters are as
	 * specified by {@link DatabaseMetaData#getIndexInfo(String, String, String, boolean, boolean)},
	 * and are copied here:
	 * <p/>
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
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException;

	@Override
	public Iterable<IndexInformation> getIndexes(TableInformation tableInformation) {
		final Map<Identifier, IndexInformationImpl.Builder> builders = new HashMap<>();
		final QualifiedTableName tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();

		final String catalogFilter;
		final String schemaFilter;

		if ( catalog == null ) {
			catalogFilter = "";
		}
		else {
			catalogFilter = catalog.getText();
		}

		if ( schema == null ) {
			schemaFilter = "";
		}
		else {
			schemaFilter = schema.getText();
		}

		try {
			processIndexInfoResultSet(
					catalogFilter,
					schemaFilter,
					tableName.getTableName().getText(),
					false,        // DO NOT limit to just unique
					true,        // DO require up-to-date results
					resultSet -> {
						while ( resultSet.next() ) {
							if ( resultSet.getShort(getResultSetIndexTypeLabel() ) == DatabaseMetaData.tableIndexStatistic ) {
								continue;
							}

							final Identifier indexIdentifier = DatabaseIdentifier.toIdentifier(
									resultSet.getString( getResultSetIndexNameLabel() )
							);
							IndexInformationImpl.Builder builder = builders.get( indexIdentifier );
							if ( builder == null ) {
								builder = IndexInformationImpl.builder( indexIdentifier );
								builders.put( indexIdentifier, builder );
							}

							final Identifier columnIdentifier = DatabaseIdentifier.toIdentifier(
									resultSet.getString( getResultSetColumnNameLabel() )
							);
							final ColumnInformation columnInformation = tableInformation.getColumn( columnIdentifier );
							if ( columnInformation == null ) {
								// See HHH-10191: this may happen when dealing with Oracle/PostgreSQL function indexes
								LOG.logCannotLocateIndexColumnInformation(
										columnIdentifier.getText(),
										indexIdentifier.getText()
								);
							}
							builder.addColumn( columnInformation );
						}
						return null;
					}
			);

		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing index information: " + tableInformation.getName().toString()
			);
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
	 *         </ul>,
	 *         The ResultSet must be ordered by the primary key
	 *         catalog/schema/table and column position within the key.
	 *     </li>
	 *     <li> execute {@code processor.process( resultSet )};</li>
	 *     <li>
	 *         release resources whether {@code processor.process( resultSet )}
	 *         executes successfully or not.
	 *     </li>
	 * </ol>
	 * <p/>
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
			ExtractionContext.ResultSetProcessor<T> processor
	) throws SQLException;

	@Override
	public Iterable<ForeignKeyInformation> getForeignKeys(TableInformation tableInformation) {
		final Map<Identifier, ForeignKeyBuilder> fkBuilders = new HashMap<>();
		final QualifiedTableName tableName = tableInformation.getName();
		final Identifier catalog = tableName.getCatalogName();
		final Identifier schema = tableName.getSchemaName();

		final String catalogFilter;
		final String schemaFilter;

		if ( catalog == null ) {
			catalogFilter = "";
		}
		else {
			catalogFilter = catalog.getText();
		}

		if ( schema == null ) {
			schemaFilter = "";
		}
		else {
			schemaFilter = schema.getText();
		}

		try {
			processImportedKeysResultSet(
					catalogFilter,
					schemaFilter,
					tableInformation.getName().getTableName().getText(),
					resultSet -> {
						// todo : need to account for getCrossReference() as well...

						while ( resultSet.next() ) {
							// IMPL NOTE : The builder is mainly used to collect the column reference mappings
							final Identifier fkIdentifier = DatabaseIdentifier.toIdentifier(
									resultSet.getString( getResultSetForeignKeyLabel() )
							);
							ForeignKeyBuilder fkBuilder = fkBuilders.get( fkIdentifier );
							if ( fkBuilder == null ) {
								fkBuilder = generateForeignKeyBuilder( fkIdentifier );
								fkBuilders.put( fkIdentifier, fkBuilder );
							}

							final QualifiedTableName incomingPkTableName = extractPrimaryKeyTableName( resultSet );

							final TableInformation pkTableInformation = extractionContext.getDatabaseObjectAccess()
									.locateTableInformation( incomingPkTableName );

							if ( pkTableInformation == null ) {
								// the assumption here is that we have not seen this table already based on fully-qualified name
								// during previous step of building all table metadata so most likely this is
								// not a match based solely on schema/catalog and that another row in this result set
								// should match.
								continue;
							}

							final Identifier fkColumnIdentifier = DatabaseIdentifier.toIdentifier(
									resultSet.getString( getResultSetForeignKeyColumnNameLabel() )
							);
							final Identifier pkColumnIdentifier = DatabaseIdentifier.toIdentifier(
									resultSet.getString( getResultSetPrimaryKeyColumnNameLabel() )
							);

							fkBuilder.addColumnMapping(
									tableInformation.getColumn( fkColumnIdentifier ),
									pkTableInformation.getColumn( pkColumnIdentifier )
							);
						}
						return null;
					}
			);
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing column metadata: " + tableInformation.getName().toString()
			);
		}

		final List<ForeignKeyInformation> fks = new ArrayList<>();
		for ( ForeignKeyBuilder fkBuilder : fkBuilders.values() ) {
			ForeignKeyInformation fk = fkBuilder.build();
			fks.add( fk );
		}
		return fks;
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
		final String incomingCatalogName = resultSet.getString( getResultSetPrimaryKeyCatalogLabel() );
		final String incomingSchemaName = resultSet.getString( getResultSetPrimaryKeySchemaLabel() );
		final String incomingTableName = resultSet.getString( getResultSetPrimaryKeyTableLabel() );

		final DatabaseIdentifier catalog = DatabaseIdentifier.toIdentifier( incomingCatalogName );
		final DatabaseIdentifier schema = DatabaseIdentifier.toIdentifier( incomingSchemaName );
		final DatabaseIdentifier table = DatabaseIdentifier.toIdentifier( incomingTableName );

		return new QualifiedTableName( catalog, schema, table );
	}

	private QualifiedTableName extractTableName(ResultSet resultSet) throws SQLException {
		final String incomingCatalogName = resultSet.getString( getResultSetCatalogLabel() );
		final String incomingSchemaName = resultSet.getString( getResultSetSchemaLabel() );
		final String incomingTableName = resultSet.getString( getResultSetTableNameLabel() );

		final DatabaseIdentifier catalog = DatabaseIdentifier.toIdentifier( incomingCatalogName );
		final DatabaseIdentifier schema = DatabaseIdentifier.toIdentifier( incomingSchemaName );
		final DatabaseIdentifier table = DatabaseIdentifier.toIdentifier( incomingTableName );

		return new QualifiedTableName( catalog, schema, table );
	}

}
