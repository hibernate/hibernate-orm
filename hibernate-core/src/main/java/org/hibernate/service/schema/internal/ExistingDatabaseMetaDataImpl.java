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

import org.jboss.logging.Logger;

import org.hibernate.TruthValue;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.service.schema.spi.ExistingColumnMetadata;
import org.hibernate.service.schema.spi.ExistingDatabaseMetaData;
import org.hibernate.service.schema.spi.ExistingSequenceMetadata;
import org.hibernate.service.schema.spi.ExistingTableMetadata;
import org.hibernate.service.schema.spi.IdentifierHelper;

/**
 * @author Steve Ebersole
 */
public class ExistingDatabaseMetaDataImpl implements ExistingDatabaseMetaData {
	private static final Logger log = Logger.getLogger( ExistingDatabaseMetaDataImpl.class );

	private final JdbcEnvironment jdbcEnvironment;
	private final DatabaseMetaData databaseMetaData;

	private final IdentifierHelperImpl identifierHelper;

	private final Map<ObjectName,ExistingTableMetadataImpl> tables = new HashMap<ObjectName, ExistingTableMetadataImpl>();
	private final Map<ObjectName,ExistingSequenceMetadata> sequences;

	public static Builder builder(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) {
		try {
			return new BuilderImpl( jdbcEnvironment, databaseMetaData );
		}
		catch (SQLException e) {
			throw jdbcEnvironment.getSqlExceptionHelper().convert( e, "Error accessing java.sql.DatabaseMetaData" );
		}
	}

	private ExistingDatabaseMetaDataImpl(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) throws SQLException {
		this.jdbcEnvironment = jdbcEnvironment;
		this.databaseMetaData = databaseMetaData;
		this.identifierHelper = new IdentifierHelperImpl(
				databaseMetaData.getConnection().getCatalog(),
				jdbcEnvironment.getSchemaNameResolver().resolveSchemaName( databaseMetaData.getConnection() ),
				databaseMetaData.storesMixedCaseQuotedIdentifiers(),
				databaseMetaData.storesLowerCaseQuotedIdentifiers(),
				databaseMetaData.storesUpperCaseQuotedIdentifiers(),
				databaseMetaData.storesUpperCaseIdentifiers(),
				databaseMetaData.storesLowerCaseIdentifiers()
		);

		sequences = loadSequenceMetadataMap();
	}

	private static final String[] TABLE_TYPES = new String[] { "TABLE", "VIEW" };

	private void loadTableMetadata(ResultSet resultSet) throws SQLException {
		while ( resultSet.next() ) {
			final Identifier catalogIdentifier = identifierHelper.fromMetaDataCatalogName(
					resultSet.getString(
							"TABLE_CAT"
					)
			);
			final Identifier schemaIdentifier = identifierHelper.fromMetaDataSchemaName(
					resultSet.getString(
							"TABLE_SCHEM"
					)
			);
			final Identifier tableIdentifier = identifierHelper.fromMetaDataObjectName(
					resultSet.getString(
							"TABLE_NAME"
					)
			);
			final ObjectName tableName = new ObjectName( catalogIdentifier, schemaIdentifier, tableIdentifier );
			// make sure it does not already exist...
			ExistingTableMetadataImpl tableMetadata = tables.get( tableName );
			if ( tableMetadata != null ) {
				throw new IllegalStateException( "Table already found on parsing database metadata [" + tableName + "]" );
			}

			tableMetadata = new ExistingTableMetadataImpl( this, tableName );
			tables.put( tableName, tableMetadata );
		}
	}

	private Map<ObjectName,ExistingSequenceMetadata> loadSequenceMetadataMap() throws SQLException {
		Map<ObjectName,ExistingSequenceMetadata> sequences = new HashMap<ObjectName, ExistingSequenceMetadata>();
		final Iterable<ExistingSequenceMetadata> sequenceMetadatas =
				jdbcEnvironment.getExistingSequenceMetadataExtractor().extractMetadata(
						databaseMetaData,
						identifierHelper
				);
		if ( sequenceMetadatas != null ) {
			for ( ExistingSequenceMetadata sequenceMetadata :sequenceMetadatas ) {
				sequences.put( sequenceMetadata.getSequenceName(), sequenceMetadata );
			}
		}
		return sequences;
	}

	@Override
	public ExistingTableMetadata getTableMetadata(ObjectName tableName) {
		return tables.get( tableName );
	}

	@Override
	public ExistingSequenceMetadata getSequenceMetadata(ObjectName sequenceName) {
		return sequences.get(  sequenceName );
	}

	public Map<Identifier, ExistingColumnMetadata> getColumnMetadata(ExistingTableMetadata tableMetadata) {
		final Map<Identifier, ExistingColumnMetadata> results = new HashMap<Identifier, ExistingColumnMetadata>();

		try {
			ResultSet resultSet = databaseMetaData.getColumns(
					identifierHelper.toMetaDataCatalogName( tableMetadata.getName().getCatalog() ),
					identifierHelper.toMetaDataSchemaName( tableMetadata.getName().getSchema() ),
					identifierHelper.toMetaDataObjectName( tableMetadata.getName().getName() ),
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

					final ExistingColumnMetadataImpl meta = new ExistingColumnMetadataImpl(
							tableMetadata,
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
					"Error accessing column metadata: " + tableMetadata.getName().toString()
			);
		}

		return results;
	}

	public Map<Identifier, ExistingForeignKeyMetadataImpl> getForeignKeyMetadata(ExistingTableMetadataImpl tableMetadata) {
		final Map<Identifier, ExistingForeignKeyMetadataImpl.Builder> fkBuilders
				= new HashMap<Identifier, ExistingForeignKeyMetadataImpl.Builder>();

		try {
			ResultSet resultSet = databaseMetaData.getImportedKeys(
					identifierHelper.toMetaDataCatalogName( tableMetadata.getName().getCatalog() ),
					identifierHelper.toMetaDataSchemaName( tableMetadata.getName().getSchema() ),
					identifierHelper.toMetaDataObjectName( tableMetadata.getName().getName() )
			);

			// todo : need to account for getCrossReference() as well...

			try {
				while ( resultSet.next() ) {
					// IMPL NOTE : intentionally build the builder early!
					final Identifier fkIdentifier = Identifier.toIdentifier( resultSet.getString( "FK_NAME" ) );
					ExistingForeignKeyMetadataImpl.Builder fkBuilder = fkBuilders.get( fkIdentifier );
					if ( fkBuilder == null ) {
						fkBuilder = ExistingForeignKeyMetadataImpl.builder( fkIdentifier );
						fkBuilders.put( fkIdentifier, fkBuilder );
					}

					final ObjectName incomingPkTableName = extractKeyTableName( resultSet, "PK" );

					final ExistingTableMetadataImpl pkTableMetadata = tables.get( incomingPkTableName );
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
							tableMetadata.getColumnMetadata( fkColumnIdentifier ),
							pkTableMetadata.getColumnMetadata( pkColumnIdentifier )
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

		final Map<Identifier, ExistingForeignKeyMetadataImpl> fks = new HashMap<Identifier, ExistingForeignKeyMetadataImpl>();
		for ( ExistingForeignKeyMetadataImpl.Builder fkBuilder : fkBuilders.values() ) {
			ExistingForeignKeyMetadataImpl fk = fkBuilder.build();
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

	public class IdentifierHelperImpl implements IdentifierHelper {
		private final String currentCatalog;
		private final String currentSchema;
		private final boolean storesMixedCaseQuotedIdentifiers;
		private final boolean storesLowerCaseQuotedIdentifiers;
		private final boolean storesUpperCaseQuotedIdentifiers;
		private final boolean storesUpperCaseIdentifiers;
		private final boolean storesLowerCaseIdentifiers;

		public IdentifierHelperImpl(
				String currentCatalog,
				String currentSchema,
				boolean storesMixedCaseQuotedIdentifiers,
				boolean storesLowerCaseQuotedIdentifiers,
				boolean storesUpperCaseQuotedIdentifiers,
				boolean storesUpperCaseIdentifiers,
				boolean storesLowerCaseIdentifiers) {
			this.currentCatalog = currentCatalog;
			this.currentSchema = currentSchema;
			this.storesMixedCaseQuotedIdentifiers = storesMixedCaseQuotedIdentifiers;
			this.storesLowerCaseQuotedIdentifiers = storesLowerCaseQuotedIdentifiers;
			this.storesUpperCaseQuotedIdentifiers = storesUpperCaseQuotedIdentifiers;
			this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
			this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;

			if ( storesMixedCaseQuotedIdentifiers && storesLowerCaseQuotedIdentifiers && storesUpperCaseQuotedIdentifiers ) {
				log.warn( "JDBC Driver reports it stores quoted identifiers in mixed, upper and lower case" );
			}
			else if ( storesMixedCaseQuotedIdentifiers && storesUpperCaseQuotedIdentifiers ) {
				log.warn( "JDBC Driver reports it stores quoted identifiers in both mixed and upper case" );
			}
			else if ( storesMixedCaseQuotedIdentifiers && storesLowerCaseQuotedIdentifiers ) {
				log.warn( "JDBC Driver reports it stores quoted identifiers in both mixed and lower case" );
			}

			if ( storesUpperCaseIdentifiers && storesLowerCaseIdentifiers ) {
				log.warn( "JDBC Driver reports it stores non-quoted identifiers in both upper and lower case" );
			}

			if ( storesUpperCaseIdentifiers && storesUpperCaseQuotedIdentifiers ) {
				log.warn( "JDBC Driver reports it stores both quoted and non-quoted identifiers in upper case" );
			}

			if ( storesLowerCaseIdentifiers && storesLowerCaseQuotedIdentifiers ) {
				log.warn( "JDBC Driver reports it stores both quoted and non-quoted identifiers in lower case" );
			}
		}

		// In the DatabaseMetaData method params for catalog and schema name have the following meaning:
		//		1) <""> means to match things "without a catalog/schema"
		//		2) <null> means to not limit results based on this field
		//
		// todo : not sure how "without a catalog/schema" is interpreted.  Current?

		@Override
		public String toMetaDataCatalogName(Identifier identifier) {
			if ( identifier == null ) {
				// todo : not sure if this is interpreted as <""> or <currentCatalog>
				return currentCatalog;
			}

			return toText( identifier );
		}

		private String toText(Identifier identifier) {
			if ( identifier == null ) {
				throw new IllegalArgumentException( "Identifier cannot be null; bad usage" );
			}

			if ( identifier.isQuoted() && storesMixedCaseQuotedIdentifiers ) {
				return identifier.getText();
			}
			else if ( ( identifier.isQuoted() && storesUpperCaseQuotedIdentifiers )
					|| ( !identifier.isQuoted() && storesUpperCaseIdentifiers ) ) {
				return StringHelper.toUpperCase( identifier.getText() );
			}
			else if ( ( identifier.isQuoted() && storesLowerCaseQuotedIdentifiers )
					|| ( !identifier.isQuoted() && storesLowerCaseIdentifiers ) ) {
				return StringHelper.toLowerCase( identifier.getText() );
			}
			return identifier.getText();
		}

		@Override
		public String toMetaDataSchemaName(Identifier identifier) {
			if ( identifier == null ) {
				// todo : not sure if this is interpreted as <""> or <currentSchema>
				return currentSchema;
			}

			return toText( identifier );
		}

		@Override
		public String toMetaDataObjectName(Identifier identifier) {
			if ( identifier == null ) {
				// if this method was called, the value is needed
				throw new IllegalArgumentException(  );
			}
			return toText( identifier );
		}

		@Override
		public Identifier fromMetaDataCatalogName(String catalogName) {
			if ( catalogName == null ) {
				return null;
			}

			if ( catalogName.equals( currentCatalog ) ) {
				return null;
			}

			return toIdentifier( catalogName );
			// note really sure the best way to know (can you?) whether the identifier is quoted

		}

		private Identifier toIdentifier(String incomingName) {
			// lovely decipher of whether the incoming value represents a quoted identifier...
			final boolean isUpperCase = incomingName.toUpperCase().equals( incomingName );
			final boolean isLowerCase = incomingName.toLowerCase().equals( incomingName );
			final boolean isMixedCase = ! isLowerCase && ! isUpperCase;

			if ( jdbcEnvironment.getReservedWords().contains( incomingName ) ) {
				// unequivocally it needs to be quoted...
				return Identifier.toIdentifier( incomingName, true );
			}

			if ( storesMixedCaseQuotedIdentifiers && isMixedCase ) {
				return Identifier.toIdentifier( incomingName, true );
			}

			if ( storesLowerCaseQuotedIdentifiers && isLowerCase ) {
				return Identifier.toIdentifier( incomingName, true );
			}

			if ( storesUpperCaseQuotedIdentifiers && isUpperCase ) {
				return Identifier.toIdentifier( incomingName, true );
			}

			return Identifier.toIdentifier( incomingName );
		}

		@Override
		public Identifier fromMetaDataSchemaName(String schemaName) {
			if ( schemaName == null ) {
				return null;
			}

			if ( schemaName.equals( currentSchema ) ) {
				return null;
			}

			return toIdentifier( schemaName );
		}

		@Override
		public Identifier fromMetaDataObjectName(String objectName) {
			if ( objectName == null ) {
				return null;
			}

			return toIdentifier( objectName );
		}
	}

	public static interface Builder {
		public Builder prepareAll();
		public Builder prepareCatalogAndSchema(Schema.Name schemaName);
		public Builder prepareCatalog(Identifier catalog);
		public Builder prepareSchema(Identifier schema);
		public ExistingDatabaseMetaData build();
	}

	private static class BuilderImpl implements Builder {
		private final ExistingDatabaseMetaDataImpl it;

		public BuilderImpl(JdbcEnvironment jdbcEnvironment, DatabaseMetaData databaseMetaData) throws SQLException {
			it = new ExistingDatabaseMetaDataImpl( jdbcEnvironment, databaseMetaData );
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
					it.identifierHelper.toMetaDataCatalogName( schemaName.getCatalog() ),
					it.identifierHelper.toMetaDataSchemaName( schemaName.getSchema() )
			);
			return this;
		}

		@Override
		public Builder prepareCatalog(Identifier catalog) {
			prepare(
					it.identifierHelper.toMetaDataCatalogName( catalog ),
					null
			);
			return this;
		}

		@Override
		public Builder prepareSchema(Identifier schema) {
			prepare(
					null,
					it.identifierHelper.toMetaDataSchemaName( schema )
			);
			return this;
		}

		@Override
		public ExistingDatabaseMetaData build() {
			return it;
		}
	}
}
