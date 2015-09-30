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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.JDBCException;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
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

	private final ExtractionContext extractionContext;

	public InformationExtractorJdbcDatabaseMetaDataImpl(ExtractionContext extractionContext) {
		this.extractionContext = extractionContext;
		
		ConfigurationService configService = extractionContext.getServiceRegistry()
				.getService( ConfigurationService.class );
		if ( ConfigurationHelper.getBoolean( AvailableSettings.ENABLE_SYNONYMS, configService.getSettings(), false ) ) {
			this.tableTypes = new String[] { "TABLE", "VIEW", "SYNONYM" };
		}
		else {
			this.tableTypes = new String[] { "TABLE", "VIEW" };
		}
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
					log.debugf(
							"Multiple schemas found with that name [%s.%s]",
							catalog.getCanonicalName(),
							schema.getCanonicalName()
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

	public TableInformation extractTableInformation(
			Identifier catalog,
			Identifier schema,
			Identifier name,
			ResultSet resultSet) throws SQLException {
		if ( catalog == null ) {
			catalog = identifierHelper().toIdentifier( resultSet.getString( "TABLE_CAT" ) );
		}
		if ( schema == null ) {
			schema = identifierHelper().toIdentifier( resultSet.getString( "TABLE_SCHEM" ) );
		}
		if ( name == null ) {
			name = identifierHelper().toIdentifier( resultSet.getString( "TABLE_NAME" ) );
		}

		final QualifiedTableName tableName = new QualifiedTableName( catalog, schema, name );

		return new TableInformationImpl(
				this,
				tableName,
				isPhysicalTableType( resultSet.getString( "TABLE_TYPE" ) ),
				resultSet.getString( "REMARKS" )
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
						extractionContext.getJdbcEnvironment().getCurrentCatalog(),
						extractionContext.getJdbcEnvironment().getCurrentSchema(),
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
					return processGetTableResults(
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

			return processGetTableResults(
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

	private TableInformation processGetTableResults(
			Identifier catalog,
			Identifier schema,
			Identifier tableName,
			ResultSet resultSet) throws SQLException {
		try {
			if ( !resultSet.next() ) {
				log.tableNotFound( tableName.render() );
				return null;
			}

			final TableInformation tableInformation = extractTableInformation(
					catalog,
					schema,
					tableName,
					resultSet
			);

			if ( resultSet.next() ) {
				log.multipleTablesFound( tableName.render() );
				throw new SchemaExtractionException(
						String.format(
								Locale.ENGLISH,
								"More than one table found in namespace (%s, %s) : %s",
								catalog.render(),
								schema.render(),
								tableName.render()
						)
				);
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
		return "TABLE".equalsIgnoreCase( tableType );
	}

	@Override
	public ColumnInformation getColumn(TableInformation tableInformation, Identifier columnIdentifier) {
		final Identifier catalog = tableInformation.getName().getCatalogName();
		final Identifier schema = tableInformation.getName().getSchemaName();

		final String catalogFilter;
		final String schemaFilter;

		if ( extractionContext.getJdbcEnvironment().getNameQualifierSupport().supportsCatalogs() ) {
			if ( catalog == null ) {
				catalogFilter = "";
			}
			else {
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
				schemaFilter = toMetaDataObjectName( schema );
			}
		}
		else {
			schemaFilter = null;
		}

		final String tableFilter = toMetaDataObjectName( tableInformation.getName().getTableName() );
		final String columnFilter = toMetaDataObjectName( columnIdentifier );
		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getColumns(
					catalogFilter,
					schemaFilter,
					tableFilter,
					columnFilter
			);

			try {
				if ( !resultSet.next() ) {
					return null;
				}
				return new ColumnInformationImpl(
						tableInformation,
						identifierHelper().toIdentifier( resultSet.getString( "COLUMN_NAME" ) ),
						resultSet.getInt( "DATA_TYPE" ),
						new StringTokenizer( resultSet.getString( "TYPE_NAME" ), "() " ).nextToken(),
						resultSet.getInt( "COLUMN_SIZE" ),
						resultSet.getInt( "DECIMAL_DIGITS" ),
						interpretTruthValue( resultSet.getString( "IS_NULLABLE" ) )
				);

			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException( e, "Error accessing column metadata: " + tableInformation.getName().toString() );
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
		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getPrimaryKeys(
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalogName() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchemaName() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getTableName() )
			);

			final List<ColumnInformation> pkColumns = new ArrayList<ColumnInformation>();
			boolean firstPass = true;
			Identifier pkIdentifier = null;

			try {
				while ( resultSet.next() ) {
					final String currentPkName = resultSet.getString( "PK_NAME" );
					final Identifier currentPkIdentifier = currentPkName == null
							? null
							: identifierHelper().toIdentifier( currentPkName );
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
					final String columnName = resultSet.getString( "COLUMN_NAME" );

					final Identifier columnIdentifier = identifierHelper().toIdentifier( columnName );
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
		final Map<Identifier, IndexInformationImpl.Builder> builders = new HashMap<Identifier, IndexInformationImpl.Builder>();

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getIndexInfo(
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalogName() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchemaName() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getTableName() ),
					false,		// DO NOT limit to just unique
					true		// DO require up-to-date results
			);

			try {
				while ( resultSet.next() ) {
					if ( resultSet.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic ) {
						continue;
					}

					final Identifier indexIdentifier = identifierHelper().toIdentifier(
							resultSet.getString(
									"INDEX_NAME"
							)
					);
					IndexInformationImpl.Builder builder = builders.get( indexIdentifier );
					if ( builder == null ) {
						builder = IndexInformationImpl.builder( indexIdentifier );
						builders.put( indexIdentifier, builder );
					}

					final Identifier columnIdentifier = identifierHelper().toIdentifier( resultSet.getString( "COLUMN_NAME" ) );
					final ColumnInformation columnInformation = tableInformation.getColumn( columnIdentifier );
					if ( columnInformation == null ) {
						throw new SchemaManagementException(
								"Could not locate column information using identifier [" + columnIdentifier.getText() + "]"
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
		final Map<Identifier, ForeignKeyBuilder> fkBuilders = new HashMap<Identifier, ForeignKeyBuilder>();

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getImportedKeys(
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalogName() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchemaName() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getTableName() )
			);

			// todo : need to account for getCrossReference() as well...

			try {
				while ( resultSet.next() ) {
					// IMPL NOTE : The builder is mainly used to collect the column reference mappings
					final Identifier fkIdentifier = identifierHelper().toIdentifier(
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

					final Identifier fkColumnIdentifier = identifierHelper().toIdentifier(
							resultSet.getString( "FKCOLUMN_NAME" )
					);
					final Identifier pkColumnIdentifier = identifierHelper().toIdentifier(
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

		return new QualifiedTableName(
				identifierHelper().toIdentifier( incomingCatalogName ),
				identifierHelper().toIdentifier( incomingSchemaName ),
				identifierHelper().toIdentifier( incomingTableName )
		);
	}
}
