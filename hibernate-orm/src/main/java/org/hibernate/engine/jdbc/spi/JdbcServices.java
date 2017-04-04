/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.Service;

/**
 * Contract for services around JDBC operations.  These represent shared resources, aka not varied by session/use.
 *
 * @author Steve Ebersole
 */
public interface JdbcServices extends Service {
	/**
	 * Obtain the JdbcEnvironment backing this JdbcServices instance.
	 */
	JdbcEnvironment getJdbcEnvironment();

	/**
	 * Obtain a JdbcConnectionAccess usable from bootstrap actions
	 * (hbm2ddl.auto, Dialect resolution, etc).
	 */
	JdbcConnectionAccess getBootstrapJdbcConnectionAccess();

	/**
	 * Obtain the dialect of the database.
	 *
	 * @return The database dialect.
	 */
	Dialect getDialect();

	/**
	 * Obtain service for logging SQL statements.
	 *
	 * @return The SQL statement logger.
	 */
	SqlStatementLogger getSqlStatementLogger();

	/**
	 * Obtain service for dealing with exceptions.
	 *
	 * @return The exception helper service.
	 */
	SqlExceptionHelper getSqlExceptionHelper();

	/**
	 * Obtain information about supported behavior reported by the JDBC driver.
	 * <p/>
	 * Yuck, yuck, yuck!  Much prefer this to be part of a "basic settings" type object.
	 * 
	 * @return The extracted database metadata, oddly enough :)
	 */
	ExtractedDatabaseMetaData getExtractedMetaDataSupport();

	/**
	 * Create an instance of a {@link LobCreator} appropriate for the current environment, mainly meant to account for
	 * variance between JDBC 4 (<= JDK 1.6) and JDBC3 (>= JDK 1.5).
	 *
	 * @param lobCreationContext The context in which the LOB is being created
	 * @return The LOB creator.
	 */
	LobCreator getLobCreator(LobCreationContext lobCreationContext);

	/**
	 * Obtain service for wrapping a {@link java.sql.ResultSet} in a "column name cache" wrapper.
	 * @return The ResultSet wrapper.
	 */
	ResultSetWrapper getResultSetWrapper();
}
