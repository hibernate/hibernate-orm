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
package org.hibernate.service.schema.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.TruthValue;
import org.hibernate.service.jdbc.env.spi.IdentifierHelper;
import org.hibernate.service.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.service.schema.spi.ColumnInformation;
import org.hibernate.service.schema.spi.DatabaseInformation;
import org.hibernate.service.schema.spi.SequenceInformation;
import org.hibernate.service.schema.spi.TableInformation;

/**
 * @author Steve Ebersole
 */
public class DatabaseInformationImpl implements DatabaseInformation {
	private final JdbcEnvironment jdbcEnvironment;
	private final DatabaseMetaData databaseMetaData;

	private final Map<ObjectName,TableInformationImpl> tables = new HashMap<ObjectName, TableInformationImpl>();
	private final Map<ObjectName,SequenceInformation> sequences;

	public static Builder builder(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) {
		try {
			return new BuilderImpl( jdbcEnvironment, databaseMetaData );
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper().convert( e, "Error accessing java.sql.DatabaseMetaData" );
		}
	}

	private DatabaseInformationImpl(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) throws SQLException {
		this.jdbcEnvironment = jdbcEnvironment;
		this.databaseMetaData = databaseMetaData;
		this.sequences = loadSequenceMetadataMap();
	}

	public IdentifierHelper identifierHelper() {
		return jdbcEnvironment.getIdentifierHelper();
	}

	private static final String[] TABLE_TYPES = new String[] { "TABLE", "VIEW" };

	private void loadTableMetadata(ResultSet resultSet) throws SQLException {
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
			// make sure it does not already exist...
			TableInformationImpl tableMetadata = tables.get( tableName );
			if ( tableMetadata != null ) {
				throw new IllegalStateException( "Table already found on parsing database metadata [" + tableName + "]" );
			}

			tableMetadata = new TableInformationImpl( this, tableName );
			tables.put( toMapKey( tableName ), tableMetadata );
		}
	}

	private Map<ObjectName,SequenceInformation> loadSequenceMetadataMap() throws SQLException {
		Map<ObjectName,SequenceInformation> sequences = new HashMap<ObjectName, SequenceInformation>();
		// todo : temporary impl!
		final Iterable<SequenceInformation> sequenceMetadatas =
				new TemporarySequenceInformationExtractor( jdbcEnvironment ).extractMetadata( databaseMetaData );
		if ( sequenceMetadatas != null ) {
			for ( SequenceInformation sequenceInformation :sequenceMetadatas ) {
				sequences.put( toMapKey( sequenceInformation.getSequenceName() ), sequenceInformation );
			}
		}
		return sequences;
	}

	@Override
	public TableInformation getTableInformation(ObjectName tableName) {
		return tables.get( toMapKey( tableName ) );
	}

	public static ObjectName toMapKey(ObjectName qualifiedName) {
		Identifier catalog = qualifiedName.getCatalog();
		if ( catalog != null ) {
			if ( ! catalog.isQuoted() ) {
				catalog = Identifier.toIdentifier( catalog.getText().toUpperCase() );
			}
		}

		Identifier schema = qualifiedName.getSchema();
		if ( schema != null ) {
			if ( ! schema.isQuoted() ) {
				schema = Identifier.toIdentifier( schema.getText().toUpperCase() );
			}
		}

		Identifier name = qualifiedName.getName();
		if ( name != null ) {
			if ( ! name.isQuoted() ) {
				name = Identifier.toIdentifier( name.getText().toUpperCase() );
			}
		}

		return new ObjectName( catalog, schema, name );
	}

	@Override
	public SequenceInformation getSequenceInformation(ObjectName sequenceName) {
		return sequences.get( toMapKey( sequenceName ) );
	}

	public Map<Identifier, ColumnInformation> getColumnMetadata(TableInformation tableInformation) {
		final Map<Identifier, ColumnInformation> results = new HashMap<Identifier, ColumnInformation>();

		try {
			ResultSet resultSet = databaseMetaData.getColumns(
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

					final Identifier columnIdentifier = Identifier.toIdentifier( columnName );
					if ( results.containsKey( columnIdentifier ) ) {
						continue;
					}

					final ColumnInformationImpl meta = new ColumnInformationImpl(
							tableInformation,
							columnIdentifier,
							resultSet.getInt( "DATA_TYPE" ),
							new StringTokenizer( resultSet.getString( "TYPE_NAME" ), "() " ).nextToken(),
							resultSet.getInt( "COLUMN_SIZE" ),
							resultSet.getInt("DECIMAL_DIGITS"),
							interpretTruthValue( resultSet.getString( "IS_NULLABLE" ) )
					);
					results.put( columnIdentifier, meta );
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper().convert(
					e,
					"Error accessing column metadata: " + tableInformation.getName().toString()
			);
		}

		return results;
	}

	public Map<Identifier, ForeignKeyInformationImpl> getForeignKeyMetadata(TableInformationImpl tableMetadata) {
		final Map<Identifier, ForeignKeyInformationImpl.Builder> fkBuilders
				= new HashMap<Identifier, ForeignKeyInformationImpl.Builder>();

		try {
			ResultSet resultSet = databaseMetaData.getImportedKeys(
					identifierHelper().toMetaDataCatalogName( tableMetadata.getName().getCatalog() ),
					identifierHelper().toMetaDataSchemaName( tableMetadata.getName().getSchema() ),
					identifierHelper().toMetaDataObjectName( tableMetadata.getName().getName() )
			);

			// todo : need to account for getCrossReference() as well...

			try {
				while ( resultSet.next() ) {
					// IMPL NOTE : intentionally build the builder early!
					final Identifier fkIdentifier = Identifier.toIdentifier( resultSet.getString( "FK_NAME" ) );
					ForeignKeyInformationImpl.Builder fkBuilder = fkBuilders.get( fkIdentifier );
					if ( fkBuilder == null ) {
						fkBuilder = ForeignKeyInformationImpl.builder( fkIdentifier );
						fkBuilders.put( fkIdentifier, fkBuilder );
					}

					final ObjectName incomingPkTableName = extractKeyTableName( resultSet, "PK" );

					final TableInformationImpl pkTableMetadata = tables.get( incomingPkTableName );
					if ( pkTableMetadata == null ) {
						// the assumption here is that we have not seen this table already based on fully-qualified name
						// during previous step of building all table metadata so most likely this is
						// not a match based solely on schema/catalog and that another row in this result set
						// should match.
						continue;
					}

					final Identifier fkColumnIdentifier = Identifier.toIdentifier( resultSet.getString( "FKCOLUMN_NAME" ) );
					final Identifier pkColumnIdentifier = Identifier.toIdentifier( resultSet.getString( "PKCOLUMN_NAME" ) );

					fkBuilder.addColumnMapping(
							tableMetadata.getColumnInformation( fkColumnIdentifier ),
							pkTableMetadata.getColumnInformation( pkColumnIdentifier )
					);
				}
			}
			finally {
				resultSet.close();
			}
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper().convert(
					e,
					"Error accessing column metadata: " + tableMetadata.getName().toString()
			);
		}

		final Map<Identifier, ForeignKeyInformationImpl> fks = new HashMap<Identifier, ForeignKeyInformationImpl>();
		for ( ForeignKeyInformationImpl.Builder fkBuilder : fkBuilders.values() ) {
			ForeignKeyInformationImpl fk = fkBuilder.build();
			fks.put( fk.getForeignKeyIdentifier(), fk );
		}
		return fks;
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

	private TruthValue interpretTruthValue(String nullable) {
		if ( "yes".equalsIgnoreCase( nullable ) ) {
			return TruthValue.TRUE;
		}
		else if ( "no".equalsIgnoreCase( nullable ) ) {
			return TruthValue.FALSE;
		}
		return TruthValue.UNKNOWN;
	}

	public static interface Builder {
		public Builder prepareAll();
		public Builder prepareCatalogAndSchema(Schema.Name schemaName);
		public Builder prepareCatalog(Identifier catalog);
		public Builder prepareSchema(Identifier schema);
		public DatabaseInformation build();
	}

	private static class BuilderImpl implements Builder {
		private final DatabaseInformationImpl it;

		public BuilderImpl(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) throws SQLException {
			it = new DatabaseInformationImpl( jdbcEnvironment, databaseMetaData );
		}

		@Override
		public Builder prepareAll() {
			prepare( null, null );
			return this;
		}

		private void prepare(String catalog, String schema) {
			try {
				ResultSet resultSet = it.databaseMetaData.getTables(
						catalog,
						schema,
						null,
						TABLE_TYPES
				);

				try {
					it.loadTableMetadata( resultSet );
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
				throw it.jdbcEnvironment.getSqlExceptionHelper().convert( sqlException, "Error accessing table metadata" );
			}

		}

		@Override
		public Builder prepareCatalogAndSchema(Schema.Name schemaName) {
			prepare(
					it.identifierHelper().toMetaDataCatalogName( schemaName.getCatalog() ),
					it.identifierHelper().toMetaDataSchemaName( schemaName.getSchema() )
			);
			return this;
		}

		@Override
		public Builder prepareCatalog(Identifier catalog) {
			prepare(
					it.identifierHelper().toMetaDataCatalogName( catalog ),
					null
			);
			return this;
		}

		@Override
		public Builder prepareSchema(Identifier schema) {
			prepare(
					null,
					it.identifierHelper().toMetaDataSchemaName( schema )
			);
			return this;
		}

		@Override
		public DatabaseInformation build() {
			return it;
		}
	}
}
