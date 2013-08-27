/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.JDBCException;
import org.hibernate.TruthValue;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.SchemaExtractionException;
import org.hibernate.tool.schema.extract.spi.SchemaMetaDataExtractor;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * Implementation of the SchemaMetaDataExtractor contract which uses the standard JDBC {@link java.sql.DatabaseMetaData}
 * API for extraction.
 *
 * @author Steve Ebersole
 */
public class StandardJdbcDatabaseMetaDataExtractor implements SchemaMetaDataExtractor {
	private final String[] tableTypes;

	private final ExtractionContext extractionContext;

	public StandardJdbcDatabaseMetaDataExtractor(ExtractionContext extractionContext) {
		this.extractionContext = extractionContext;
		
		ConfigurationService configService = extractionContext.getJdbcEnvironment().getServiceRegistry()
				.getService( ConfigurationService.class );
		if (ConfigurationHelper.getBoolean( AvailableSettings.ENABLE_SYNONYMS, configService.getSettings(), false ) ) {
			tableTypes = new String[] { "TABLE", "VIEW", "SYNONYM" };
		}
		else {
			tableTypes = new String[] { "TABLE", "VIEW" };
		}
	}

	protected IdentifierHelper identifierHelper() {
		return extractionContext.getJdbcEnvironment().getIdentifierHelper();
	}

	protected JDBCException convertSQLException(SQLException sqlException, String message) {
		return extractionContext.getJdbcEnvironment().getSqlExceptionHelper().convert( sqlException, message );
	}

	@Override
	public Iterable<TableInformation> getTables(String catalogFilter, String schemaFilter) {
		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getTables(
					catalogFilter,
					schemaFilter,
					null,
					tableTypes
			);

			final List<TableInformation> results = new ArrayList<TableInformation>();

			try {
				while ( resultSet.next() ) {
					final Identifier catalogIdentifier = identifierHelper().fromMetaDataCatalogName(
							resultSet.getString(
									"TABLE_CAT"
							)
					);
					final Identifier schemaIdentifier = identifierHelper().fromMetaDataSchemaName(
							resultSet.getString(
									"TABLE_SCHEM"
							)
					);
					final Identifier tableIdentifier = identifierHelper().fromMetaDataObjectName(
							resultSet.getString(
									"TABLE_NAME"
							)
					);
					final ObjectName tableName = new ObjectName( catalogIdentifier, schemaIdentifier, tableIdentifier );
					TableInformation tableInformation = new TableInformationImpl(
							this,
							tableName,
							isPhysicalTableType( resultSet.getString( "TABLE_TYPE" ) ),
							resultSet.getString( "REMARKS" )
					);
					results.add( tableInformation );
				}
			}
			finally {
				try {
					resultSet.close();
				}
				catch (SQLException ignore) {
				}
			}

			return results;
		}
		catch (SQLException sqlException) {
			throw convertSQLException( sqlException, "Error accessing table metadata" );
		}
	}

	protected boolean isPhysicalTableType(String tableType) {
		return "TABLE".equalsIgnoreCase( tableType );
	}

	@Override
	public Iterable<ColumnInformation> getColumns(TableInformation tableInformation) {
		final List<ColumnInformation> results = new ArrayList<ColumnInformation>();

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getColumns(
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalog() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchema() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getName() ),
					"%"
			);

			try {
				while ( resultSet.next() ) {
					final String columnName = resultSet.getString( "COLUMN_NAME" );
					if ( columnName == null ) {
						continue;
					}

					results.add(
							new ColumnInformationImpl(
									tableInformation,
									Identifier.toIdentifier( columnName ),
									resultSet.getInt( "DATA_TYPE" ),
									new StringTokenizer( resultSet.getString( "TYPE_NAME" ), "() " ).nextToken(),
									resultSet.getInt( "COLUMN_SIZE" ),
									resultSet.getInt("DECIMAL_DIGITS"),
									interpretTruthValue( resultSet.getString( "IS_NULLABLE" ) )
							)
					);
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw convertSQLException( e, "Error accessing column metadata: " + tableInformation.getName().toString() );
		}

		return results;
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
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalog() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchema() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getName() )
			);

			final List<ColumnInformation> pkColumns = new ArrayList<ColumnInformation>();
			boolean firstPass = true;
			Identifier pkIdentifier = null;

			try {
				while ( resultSet.next() ) {
					final String currentPkName = resultSet.getString( "PK_NAME" );
					final Identifier currentPkIdentifier = currentPkName == null
							? null
							: identifierHelper().fromMetaDataObjectName( currentPkName );
					if ( firstPass ) {
						pkIdentifier = currentPkIdentifier;
						firstPass = false;
					}
					else {
						if ( !EqualsHelper.equals( pkIdentifier, currentPkIdentifier ) ) {
							throw new SchemaExtractionException(
									String.format(
											"Encountered primary keys differing name on table %s",
											tableInformation.getName().toText()
									)
							);
						}
					}

					final int columnPosition = resultSet.getInt( "KEY_SEQ" );
					final String columnName = resultSet.getString( "COLUMN_NAME" );

					final Identifier columnIdentifier = identifierHelper().fromMetaDataObjectName( columnName );
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
			throw convertSQLException( e, "Error while reading primary key meta data for " + tableInformation.getName().toText() );
		}
	}

	@Override
	public Iterable<IndexInformation> getIndexes(TableInformation tableInformation) {
		final Map<Identifier, IndexInformationImpl.Builder> builders = new HashMap<Identifier, IndexInformationImpl.Builder>();

		try {
			ResultSet resultSet = extractionContext.getJdbcDatabaseMetaData().getIndexInfo(
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalog() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchema() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getName() ),
					false,		// DO NOT limit to just unique
					true		// DO require up-to-date results
			);

			try {
				while ( resultSet.next() ) {
					if ( resultSet.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic ) {
						continue;
					}

					final Identifier indexIdentifier = Identifier.toIdentifier( resultSet.getString( "INDEX_NAME" ) );
					IndexInformationImpl.Builder builder = builders.get( indexIdentifier );
					if ( builder == null ) {
						builder = IndexInformationImpl.builder( indexIdentifier );
						builders.put( indexIdentifier, builder );
					}

					final Identifier columnIdentifier = Identifier.toIdentifier( resultSet.getString( "COLUMN_NAME" ) );
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
					identifierHelper().toMetaDataCatalogName( tableInformation.getName().getCatalog() ),
					identifierHelper().toMetaDataSchemaName( tableInformation.getName().getSchema() ),
					identifierHelper().toMetaDataObjectName( tableInformation.getName().getName() )
			);

			// todo : need to account for getCrossReference() as well...

			try {
				while ( resultSet.next() ) {
					// IMPL NOTE : The builder is mainly used to collect the column reference mappings
					final Identifier fkIdentifier = Identifier.toIdentifier( resultSet.getString( "FK_NAME" ) );
					ForeignKeyBuilder fkBuilder = fkBuilders.get( fkIdentifier );
					if ( fkBuilder == null ) {
						fkBuilder = generateForeignKeyBuilder( fkIdentifier );
						fkBuilders.put( fkIdentifier, fkBuilder );
					}

					final ObjectName incomingPkTableName = extractKeyTableName( resultSet, "PK" );

					final TableInformation pkTableInformation = extractionContext.getRegisteredObjectAccess()
							.locateRegisteredTableInformation( incomingPkTableName );

					if ( pkTableInformation == null ) {
						// the assumption here is that we have not seen this table already based on fully-qualified name
						// during previous step of building all table metadata so most likely this is
						// not a match based solely on schema/catalog and that another row in this result set
						// should match.
						continue;
					}

					final Identifier fkColumnIdentifier = Identifier.toIdentifier( resultSet.getString( "FKCOLUMN_NAME" ) );
					final Identifier pkColumnIdentifier = Identifier.toIdentifier( resultSet.getString( "PKCOLUMN_NAME" ) );

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

	protected static interface ForeignKeyBuilder {
		public ForeignKeyBuilder addColumnMapping(ColumnInformation referencing, ColumnInformation referenced);

		public ForeignKeyInformation build();
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

	private ObjectName extractKeyTableName(ResultSet resultSet, String prefix) throws SQLException {
		final String incomingCatalogName = resultSet.getString( prefix + "TABLE_SCHEM" );
		final String incomingSchemaName = resultSet.getString( prefix + "TABLE_CATALOG" );
		final String incomingTableName = resultSet.getString( prefix + "TABLE_NAME" );

		return new ObjectName(
				Identifier.toIdentifier( incomingCatalogName ), Identifier.toIdentifier( incomingSchemaName ),
				Identifier.toIdentifier( incomingTableName )
		);
	}
}
