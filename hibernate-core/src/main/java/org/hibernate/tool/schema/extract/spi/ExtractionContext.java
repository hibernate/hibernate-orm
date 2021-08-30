/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.spi;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

/**
 * Defines a context for performing extraction including providing access to information about ongoing extraction as
 * well as to delegates needed in performing extraction.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
@Incubating
public interface ExtractionContext {
	ServiceRegistry getServiceRegistry();
	JdbcEnvironment getJdbcEnvironment();
	Connection getJdbcConnection();
	DatabaseMetaData getJdbcDatabaseMetaData();

	@Incubating
	default <T> T getQueryResults(
			String queryString,
			Object[] positionalParameters,
			ResultSetProcessor<T> resultSetProcessor) throws SQLException {
		try (PreparedStatement statement = getJdbcConnection().prepareStatement( queryString )) {
			if ( positionalParameters != null ) {
				for ( int i = 0 ; i < positionalParameters.length ; i++ ) {
					statement.setObject( i + 1, positionalParameters[i] );
				}
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSetProcessor.process( resultSet );
			}
		}
	}

	Identifier getDefaultCatalog();
	Identifier getDefaultSchema();

	@Incubating
	interface ResultSetProcessor<T> {
		T process(ResultSet resultSet) throws SQLException;
	}

	/**
	 * In conjunction with {@link #getDatabaseObjectAccess()} provides access to
	 * information about known database objects to the extractor.
	 */
	@Incubating
	interface DatabaseObjectAccess {
		TableInformation locateTableInformation(QualifiedTableName tableName);
		SequenceInformation locateSequenceInformation(QualifiedSequenceName sequenceName);
	}

	DatabaseObjectAccess getDatabaseObjectAccess();

	void cleanup();

	abstract class EmptyExtractionContext implements ExtractionContext {
		@Override
		public ServiceRegistry getServiceRegistry() {
			return null;
		}

		@Override
		public JdbcEnvironment getJdbcEnvironment() {
			return null;
		}

		@Override
		public Connection getJdbcConnection() {
			return null;
		}

		@Override
		public DatabaseMetaData getJdbcDatabaseMetaData() {
			return null;
		}

		@Override
		public Identifier getDefaultCatalog() {
			return null;
		}

		@Override
		public Identifier getDefaultSchema() {
			return null;
		}

		@Override
		public DatabaseObjectAccess getDatabaseObjectAccess() {
			return null;
		}

		@Override
		public void cleanup() {

		}
	}
}
