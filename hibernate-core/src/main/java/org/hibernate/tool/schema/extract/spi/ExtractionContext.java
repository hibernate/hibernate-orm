package org.hibernate.tool.schema.extract.spi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

/**
 * Defines a context for performing extraction including providing access
 * to information about ongoing extraction as well as to delegates needed
 * in performing extraction.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
@Incubating(since = "5.1")
public interface ExtractionContext {
	@Nonnull ServiceRegistry getServiceRegistry();
	@Nonnull JdbcEnvironment getJdbcEnvironment();
	@Nonnull SqlStringGenerationContext getSqlStringGenerationContext();
	@Nonnull Connection getJdbcConnection();
	@Nonnull DatabaseMetaData getJdbcDatabaseMetaData();

	@Incubating(since = "8.0")
	default <T> @Nullable T getQueryResults(
			@Nonnull String queryString,
			@Nullable Object[] positionalParameters,
			@Nonnull ResultSetProcessor<T> resultSetProcessor) throws SQLException {
		try ( var statement = getJdbcConnection().prepareStatement( queryString ) ) {
			if ( positionalParameters != null ) {
				for ( int i = 0 ; i < positionalParameters.length ; i++ ) {
					statement.setObject( i + 1, positionalParameters[i] );
				}
			}
			try ( var resultSet = statement.executeQuery() ) {
				return resultSetProcessor.process( resultSet );
			}
		}
	}

	@Nullable Identifier getDefaultCatalog();
	@Nullable Identifier getDefaultSchema();

	@Incubating(since = "8.0")
	interface ResultSetProcessor<T> {
		@Nullable T process(@Nonnull ResultSet resultSet) throws SQLException;
	}

	/**
	 * In conjunction with {@link #getDatabaseObjectAccess()} provides access to
	 * information about known database objects to the extractor.
	 */
	@Incubating(since = "8.0")
	interface DatabaseObjectAccess {
		@Nullable TableInformation locateTableInformation(@Nonnull QualifiedTableName tableName);
		@Nullable SequenceInformation locateSequenceInformation(@Nonnull QualifiedSequenceName sequenceName);
		@Nullable PrimaryKeyInformation locatePrimaryKeyInformation(@Nonnull QualifiedTableName tableName);
		@Nonnull Iterable<ForeignKeyInformation> locateForeignKeyInformation(@Nonnull QualifiedTableName tableName);
		@Nonnull Iterable<IndexInformation> locateIndexesInformation(@Nonnull QualifiedTableName tableName);
		boolean isCaching();
	}

	@Nonnull DatabaseObjectAccess getDatabaseObjectAccess();

	void cleanup();

	/**
	 * @deprecated Implement {@link ExtractionContext} directly.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	abstract class EmptyExtractionContext implements ExtractionContext {
		@Override
		public @Nonnull ServiceRegistry getServiceRegistry() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nonnull JdbcEnvironment getJdbcEnvironment() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nonnull SqlStringGenerationContext getSqlStringGenerationContext() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nonnull Connection getJdbcConnection() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nonnull DatabaseMetaData getJdbcDatabaseMetaData() {
			throw new UnsupportedOperationException();
		}

		@Override
		public @Nullable Identifier getDefaultCatalog() {
			return null;
		}

		@Override
		public @Nullable Identifier getDefaultSchema() {
			return null;
		}

		@Override
		public @Nonnull DatabaseObjectAccess getDatabaseObjectAccess() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void cleanup() {

		}
	}
}
