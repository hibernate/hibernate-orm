/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.compare.EqualsHelper;
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

/**
 * Implementation of the SchemaMetaDataExtractor contract which uses the standard JDBC {@link java.sql.DatabaseMetaData}
 * API for extraction.
 *
 * @author Steve Ebersole
 */
public class InformationExtractorJdbcDatabaseMetaDataImpl implements InformationExtractor {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( InformationExtractorJdbcDatabaseMetaDataImpl.class );

	private final String[] tableTypes;

	private String[] extraPhysicalTableTypes;

	private final ExtractionContext extractionContext;

	public InformationExtractorJdbcDatabaseMetaDataImpl(ExtractionContext extractionContext) {
		this.extractionContext = extractionContext;

		ConfigurationService configService = extractionContext.getServiceRegistry()
				.getService( ConfigurationService.class );

		final String extraPhysycalTableTypesConfig = configService.getSetting(
				AvailableSettings.EXTRA_PHYSICAL_TABLE_TYPES,
				StandardConverters.STRING,
				""
		);
		if ( !"".equals( extraPhysycalTableTypesConfig.trim() ) ) {
			this.extraPhysicalTableTypes = StringHelper.splitTrimmingTokens(
					",;",
					extraPhysycalTableTypesConfig,
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

	@Override
	public boolean catalogExists(Identifier catalog) {
		try {
			final ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getCatalogs();

			try {
				while ( resultSet.next() ) {
					final String existingCatalogName = resultSet.getString( "TABLE_CAT" );

					// todo : hmm.. case sensitive or insensitive match...
					// for now, match any case...

					if ( catalog.getText().equalsIgnoreCase( existingCatalogName ) ) {
						return true;
					}
				}

				return false;
			}
			finally {
				try {
					resultSet.close();
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Unable to query DatabaseMetaData for existing catalogs" );
		}
	}

	@Override
	public boolean schemaExists(Identifier catalog, Identifier schema) {
		try {
			final String catalogFilter = determineCatalogFilter( catalog );
			final String schemaFilter = determineSchemaFilter( schema );

			final ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getSchemas(
					catalogFilter,
					schemaFilter
			);

			try {
				if ( !resultSet.next() ) {
					return false;
				}

				if ( resultSet.next() ) {
					final String catalogName = catalog == null ? "" : catalog.getCanonicalName();
					final String schemaName = schema == null ? "" : schema.getCanonicalName();

					log.debugf(
							"Multiple schemas found with that name [%s.%s]",
							catalogName,
							schemaName
					);
				}
				return true;
			}
			finally {
				try {
					resultSet.close();
				}
				catch (SQLException ignore) {
				}
			}
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Unable to query DatabaseMetaData for existing schemas" );
		}
	}

	private String determineCatalogFilter(Identifier catalog) throws SQLException {
		Identifier identifierToUse = catalog;
		if ( identifierToUse == null ) {
			identifierToUse = extractionContext.getDefaultCatalog();
		}

		return extractionContext.getJdbcEnvironment().getIdentifierHelper().toMetaDataCatalogName( identifierToUse );
	}

	private String determineSchemaFilter(Identifier schema) throws SQLException {
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
				isPhysicalTableType( resultSet.getString( "TABLE_TYPE" ) ),
				resultSet.getString( "REMARKS" )
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

			TableInformation tableInfo = null;

			// 1) look in current namespace
			if ( extractionContext.getJdbcEnvironment().getCurrentCatalog() != null
					|| extractionContext.getJdbcEnvironment().getCurrentSchema() != null ) {
				tableInfo = locateTableInNamespace(
						extractionContext.getJdbcEnvironment().getCurrentCatalog(),
						extractionContext.getJdbcEnvironment().getCurrentSchema(),
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

				final ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getTables(
						null,
						null,
						tableNameFilter,
						tableTypes
				);

				try {
					return processTableResults(
							null,
							null,
							tableName,
							resultSet
					);
				}
				finally {
					try {
						resultSet.close();
					}
					catch (SQLException ignore) {
					}
				}
			}
			catch (SQLException sqlException) {
				throw convertSQLException( sqlException, "Error accessing table metadata" );
			}
		}
	}

	public NameSpaceTablesInformation getTables(Identifier catalog, Identifier schema) {

		String catalogFilter = null;
		String schemaFilter = null;

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsCatalogs() ) {
			if ( catalog == null ) {
				if ( extractionContext.getJdbcEnvironment().getCurrentCatalog() != null ) {
					// 1) look in current namespace
					catalogFilter = toMetaDataObjectName( extractionContext.getJdbcEnvironment().getCurrentCatalog() );
				}
				else if ( extractionContext.getDefaultCatalog() != null ) {
					// 2) look in default namespace
					catalogFilter = toMetaDataObjectName( extractionContext.getDefaultCatalog() );
				}
				else {
					catalogFilter = "";
				}
			}
			else {
				catalogFilter = toMetaDataObjectName( catalog );
			}
		}

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsSchemas() ) {
			if ( schema == null ) {
				if ( extractionContext.getJdbcEnvironment().getCurrentSchema() != null ) {
					// 1) look in current namespace
					schemaFilter = toMetaDataObjectName( extractionContext.getJdbcEnvironment().getCurrentSchema() );
				}
				else if ( extractionContext.getDefaultSchema() != null ) {
					// 2) look in default namespace
					schemaFilter = toMetaDataObjectName( extractionContext.getDefaultSchema() );
				}
				else {
					schemaFilter = "";
				}
			}
			else {
				schemaFilter = toMetaDataObjectName( schema );
			}
		}

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getTables(
					catalogFilter,
					schemaFilter,
					"%",
					tableTypes
			);

			final NameSpaceTablesInformation tablesInformation = processTableResults( resultSet );
			populateTablesWithColumns( catalogFilter, schemaFilter, tablesInformation );
			return tablesInformation;
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	private void populateTablesWithColumns(
			String catalogFilter,
			String schemaFilter,
			NameSpaceTablesInformation tables) {
		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getColumns(
					catalogFilter,
					schemaFilter,
					null,
					"%"
			);
			try {
				String currentTableName = "";
				TableInformation currentTable = null;
				while ( resultSet.next() ) {
					if ( !currentTableName.equals( resultSet.getString( "TABLE_NAME" ) ) ) {
						currentTableName = resultSet.getString( "TABLE_NAME" );
						currentTable = tables.getTableInformation( currentTableName );
					}
					if ( currentTable != null ) {
						final ColumnInformationImpl columnInformation = new ColumnInformationImpl(
								currentTable,
								DatabaseIdentifier.toIdentifier( resultSet.getString( "COLUMN_NAME" ) ),
								resultSet.getInt( "DATA_TYPE" ),
								new StringTokenizer( resultSet.getString( "TYPE_NAME" ), "() " ).nextToken(),
								resultSet.getInt( "COLUMN_SIZE" ),
								resultSet.getInt( "DECIMAL_DIGITS" ),
								interpretTruthValue( resultSet.getString( "IS_NULLABLE" ) )
						);
						currentTable.addColumn( columnInformation );
					}
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing tables metadata"
			);
		}
	}

	private NameSpaceTablesInformation processTableResults(ResultSet resultSet) throws SQLException {
		try {
			NameSpaceTablesInformation tables = new NameSpaceTablesInformation(identifierHelper());
			while ( resultSet.next() ) {
				final TableInformation tableInformation = extractTableInformation( resultSet );
				tables.addTableInformation( tableInformation );
			}

			return tables;
		}
		finally {
			try {
				resultSet.close();
			}
			catch (SQLException ignore) {
			}
		}
	}

	private TableInformation locateTableInNamespace(
			Identifier catalog,
			Identifier schema,
			Identifier tableName) {
		Identifier catalogToUse = null;
		Identifier schemaToUse = null;

		final String catalogFilter;
		final String schemaFilter;

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsCatalogs() ) {
			if ( catalog == null ) {
				catalogFilter = "";
			}
			else {
				catalogToUse = catalog;
				catalogFilter = toMetaDataObjectName( catalog );
			}
		}
		else {
			catalogFilter = null;
		}

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsSchemas() ) {
			if ( schema == null ) {
				schemaFilter = "";
			}
			else {
				schemaToUse = schema;
				schemaFilter = toMetaDataObjectName( schema );
			}
		}
		else {
			schemaFilter = null;
		}

		final String tableNameFilter = toMetaDataObjectName( tableName );

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getTables(
					catalogFilter,
					schemaFilter,
					tableNameFilter,
					tableTypes
			);

			return processTableResults(
					catalogToUse,
					schemaToUse,
					tableName,
					resultSet
			);
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	private TableInformation processTableResults(
			Identifier catalog,
			Identifier schema,
			Identifier tableName,
			ResultSet resultSet) throws SQLException {
		try {
			boolean found = false;
			TableInformation tableInformation = null;
			while ( resultSet.next() ) {
				if ( tableName.equals( Identifier.toIdentifier( resultSet.getString( "TABLE_NAME" ),
																tableName.isQuoted() ) ) ) {
					if ( found ) {
						log.multipleTablesFound( tableName.render() );
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
				log.tableNotFound( tableName.render() );
			}
			return tableInformation;
		}
		finally {
			try {
				resultSet.close();
			}
			catch (SQLException ignore) {
			}
		}
	}

	protected boolean isPhysicalTableType(String tableType) {
		if ( extraPhysicalTableTypes == null ) {
			return "TABLE".equalsIgnoreCase( tableType );
		}
		else {
			if ( "TABLE".equalsIgnoreCase( tableType ) ) {
				return true;
			}
			for ( int i = 0; i < extraPhysicalTableTypes.length; i++ ) {
				if ( extraPhysicalTableTypes[i].equalsIgnoreCase( tableType ) ) {
					return true;
				}
			}
			return false;
		}
	}

	private void addColumns(TableInformation tableInformation) {
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
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getColumns(
					catalogFilter,
					schemaFilter,
					tableName.getTableName().getText(),
					"%"
			);

			try {
				while ( resultSet.next() ) {
					final String columnName = resultSet.getString( "COLUMN_NAME" );
					final ColumnInformationImpl columnInformation = new ColumnInformationImpl(
							tableInformation,
							DatabaseIdentifier.toIdentifier( columnName ),
							resultSet.getInt( "DATA_TYPE" ),
							new StringTokenizer( resultSet.getString( "TYPE_NAME" ), "() " ).nextToken(),
							resultSet.getInt( "COLUMN_SIZE" ),
							resultSet.getInt( "DECIMAL_DIGITS" ),
							interpretTruthValue( resultSet.getString( "IS_NULLABLE" ) )
					);
					tableInformation.addColumn( columnInformation );
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing column metadata: " + tableName.toString()
			);
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
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getPrimaryKeys(
					catalogFilter,
					schemaFilter,
					tableInformation.getName().getTableName().getText()
			);

			final List<ColumnInformation> pkColumns = new ArrayList<ColumnInformation>();
			boolean firstPass = true;
			Identifier pkIdentifier = null;

			try {
				while ( resultSet.next() ) {
					final String currentPkName = resultSet.getString( "PK_NAME" );
					final Identifier currentPkIdentifier = currentPkName == null
							? null
							: DatabaseIdentifier.toIdentifier( currentPkName );
					if ( firstPass ) {
						pkIdentifier = currentPkIdentifier;
						firstPass = false;
					}
					else {
						if ( !EqualsHelper.equals( pkIdentifier, currentPkIdentifier ) ) {
							throw new SchemaExtractionException(
									String.format(
											"Encountered primary keys differing name on table %s",
											tableInformation.getName().toString()
									)
							);
						}
					}

					final int columnPosition = resultSet.getInt( "KEY_SEQ" );

					final Identifier columnIdentifier = DatabaseIdentifier.toIdentifier(
							resultSet.getString( "COLUMN_NAME" )
					);
					final ColumnInformation column = tableInformation.getColumn( columnIdentifier );
					pkColumns.add( columnPosition-1, column );
				}
			}
			finally {
				resultSet.close();
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
		catch (SQLException e) {
			throw convertSQLException( e, "Error while reading primary key meta data for " + tableInformation.getName().toString() );
		}
	}

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
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getIndexInfo(
					catalogFilter,
					schemaFilter,
					tableName.getTableName().getText(),
					false,        // DO NOT limit to just unique
					true        // DO require up-to-date results
			);

			try {
				while ( resultSet.next() ) {
					if ( resultSet.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic ) {
						continue;
					}

					final Identifier indexIdentifier = DatabaseIdentifier.toIdentifier(
							resultSet.getString( "INDEX_NAME" )
					);
					IndexInformationImpl.Builder builder = builders.get( indexIdentifier );
					if ( builder == null ) {
						builder = IndexInformationImpl.builder( indexIdentifier );
						builders.put( indexIdentifier, builder );
					}

					final Identifier columnIdentifier = DatabaseIdentifier.toIdentifier( resultSet.getString( "COLUMN_NAME" ) );
					final ColumnInformation columnInformation = tableInformation.getColumn( columnIdentifier );
					if ( columnInformation == null ) {
						// See HHH-10191: this may happen when dealing with Oracle/PostgreSQL function indexes
						log.logCannotLocateIndexColumnInformation(
								columnIdentifier.getText(),
								indexIdentifier.getText()
						);
					}
					builder.addColumn( columnInformation );
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing index information: " + tableInformation.getName().toString()
			);
		}

		final List<IndexInformation> indexes = new ArrayList<IndexInformation>();
		for ( IndexInformationImpl.Builder builder : builders.values() ) {
			IndexInformationImpl index = builder.build();
			indexes.add( index );
		}
		return indexes;
	}

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
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getImportedKeys(
					catalogFilter,
					schemaFilter,
					tableInformation.getName().getTableName().getText()
			);

			// todo : need to account for getCrossReference() as well...

			try {
				while ( resultSet.next() ) {
					// IMPL NOTE : The builder is mainly used to collect the column reference mappings
					final Identifier fkIdentifier = DatabaseIdentifier.toIdentifier(
							resultSet.getString( "FK_NAME" )
					);
					ForeignKeyBuilder fkBuilder = fkBuilders.get( fkIdentifier );
					if ( fkBuilder == null ) {
						fkBuilder = generateForeignKeyBuilder( fkIdentifier );
						fkBuilders.put( fkIdentifier, fkBuilder );
					}

					final QualifiedTableName incomingPkTableName = extractKeyTableName( resultSet, "PK" );

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
							resultSet.getString( "FKCOLUMN_NAME" )
					);
					final Identifier pkColumnIdentifier = DatabaseIdentifier.toIdentifier(
							resultSet.getString( "PKCOLUMN_NAME" )
					);

					fkBuilder.addColumnMapping(
							tableInformation.getColumn( fkColumnIdentifier ),
							pkTableInformation.getColumn( pkColumnIdentifier )
					);
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing column metadata: " + tableInformation.getName().toString()
			);
		}

		final List<ForeignKeyInformation> fks = new ArrayList<ForeignKeyInformation>();
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
		private final List<ForeignKeyInformation.ColumnReferenceMapping> columnMappingList = new ArrayList<ForeignKeyInformation.ColumnReferenceMapping>();

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

	private QualifiedTableName extractKeyTableName(ResultSet resultSet, String prefix) throws SQLException {
		final String incomingCatalogName = resultSet.getString( prefix + "TABLE_CAT" );
		final String incomingSchemaName = resultSet.getString( prefix + "TABLE_SCHEM" );
		final String incomingTableName = resultSet.getString( prefix + "TABLE_NAME" );

		final DatabaseIdentifier catalog = DatabaseIdentifier.toIdentifier( incomingCatalogName );
		final DatabaseIdentifier schema = DatabaseIdentifier.toIdentifier( incomingSchemaName );
		final DatabaseIdentifier table = DatabaseIdentifier.toIdentifier( incomingTableName );

		return new QualifiedTableName( catalog, schema, table );
	}

	private QualifiedTableName extractTableName(ResultSet resultSet) throws SQLException {
		final String incomingCatalogName = resultSet.getString( "TABLE_CAT" );
		final String incomingSchemaName = resultSet.getString( "TABLE_SCHEM" );
		final String incomingTableName = resultSet.getString( "TABLE_NAME" );

		final DatabaseIdentifier catalog = DatabaseIdentifier.toIdentifier( incomingCatalogName );
		final DatabaseIdentifier schema = DatabaseIdentifier.toIdentifier( incomingSchemaName );
		final DatabaseIdentifier table = DatabaseIdentifier.toIdentifier( incomingTableName );

		return new QualifiedTableName( catalog, schema, table );
	}
}
