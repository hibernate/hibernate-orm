/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Schema;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.DatabaseInformationImpl;
import org.hibernate.tool.schema.extract.internal.ExtractionContextImpl;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;

/**
 * Acts as the entry point into building {@link DatabaseInformation} instances.  The correlation is 1-to-1 between
 * DatabaseInformationBuilder and DatabaseInformation, meaning a given DatabaseInformationBuilder should only be used
 * to build a single DatabaseInformation instance.
 *
 * @author Steve Ebersole
 */
public class DatabaseInformationBuilder {
	private final DatabaseInformationImpl databaseInformation;
	private final ExtractionContext extractionContext;

	private final InformationExtractor metaDataExtractor;

	public DatabaseInformationBuilder(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			final Connection jdbcConnection) {
		this(
				serviceRegistry,
				jdbcEnvironment,
				new JdbcConnectionAccess() {
					@Override
					public Connection obtainConnection() throws SQLException {
						return jdbcConnection;
					}

					@Override
					public void releaseConnection(Connection connection) throws SQLException {
						// nothing to do, we don't "own" the connection
					}

					@Override
					public boolean supportsAggressiveRelease() {
						return false;
					}
				}
		);
	}

	public DatabaseInformationBuilder(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			JdbcConnectionAccess jdbcConnectionAccess) {
		this.databaseInformation = new DatabaseInformationImpl();
		this.extractionContext = new ExtractionContextImpl(
				serviceRegistry,
				jdbcEnvironment,
				jdbcConnectionAccess,
				databaseInformation,
				null,
				null
		);

		// todo : make this pluggable...
		metaDataExtractor = new InformationExtractorJdbcDatabaseMetaDataImpl( extractionContext );
	}

	public DatabaseInformationBuilder prepareAll() {
//		return prepare( InformationExtractor.ALL_CATALOGS_FILTER, InformationExtractor.ALL_SCHEMAS_FILTER );
		return this;
	}

	public DatabaseInformationBuilder prepareCatalogAndSchema(Schema.Name schemaName) {
//		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
//		return prepare(
//				identifierHelper.toMetaDataCatalogName( schemaName.getCatalog() ),
//				identifierHelper.toMetaDataSchemaName( schemaName.getSchema() )
//		);
		return this;
	}

	public DatabaseInformationBuilder prepareCatalog(Identifier catalog) {
//		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
//		return prepare(
//				identifierHelper.toMetaDataCatalogName( catalog ),
//				InformationExtractor.ALL_SCHEMAS_FILTER
//		);
		return this;
	}

	public DatabaseInformationBuilder prepareSchema(Identifier schema) {
//		final IdentifierHelper identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
//		return prepare(
//				InformationExtractor.ALL_CATALOGS_FILTER,
//				identifierHelper.toMetaDataSchemaName( schema )
//		);
		return this;
	}

	private DatabaseInformationBuilder prepare(String catalog, String schema) {
		// todo : apply filtering

//		for ( TableInformation tableInformation : metaDataExtractor.getTables( catalog, schema ) ) {
//			databaseInformation.registerTableInformation( tableInformation );
//		}
//
//		final Iterable<SequenceInformation> sequences = extractSequences();
//		if ( sequences != null ) {
//			for ( SequenceInformation sequenceInformation : sequences ) {
//				databaseInformation.registerSequenceInformation( sequenceInformation );
//			}
//		}

		return this;
	}

	private Iterable<SequenceInformation> extractSequences() {
		if (!extractionContext.getJdbcEnvironment().getDialect().getClass().isAssignableFrom( H2Dialect.class )) {
			// TODO: the temporary impl below is for H2 only
			return null;
		}
		
		// todo : temporary impl!!!
		final SequenceInformationExtractorH2DatabaseImpl seqExtractor = new SequenceInformationExtractorH2DatabaseImpl();
		try {
			return seqExtractor.extractMetadata( extractionContext );
		}
		catch (SQLException e) {
			throw extractionContext.getJdbcEnvironment().getSqlExceptionHelper().convert( e, "Unable to access sequence information" );
		}
	}

	public DatabaseInformation build() {
		return databaseInformation;
	}
}

