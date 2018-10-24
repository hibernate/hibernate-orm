/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10001001, max = 10001500 )
public interface ConnectionPoolingLogger extends BasicLogger {
	/**
	 * Static access to the logging instance
	 */
	ConnectionPoolingLogger CONNECTIONS_LOGGER = Logger.getMessageLogger(
			ConnectionPoolingLogger.class,
			"org.hibernate.orm.connections.pooling"
	);

	@LogMessage(level = INFO)
	@Message(value = "Connection properties: %s", id = 10001001)
	void connectionProperties(Properties connectionProps);

	@LogMessage(level = WARN)
	@Message(value = "Using Hibernate built-in connection pool (not for production use!)", id = 10001002)
	void usingHibernateBuiltInConnectionPool();

	@LogMessage(level = INFO)
	@Message(value = "Autocommit mode: %s", id = 10001003)
	void autoCommitMode(boolean autocommit);

	@Message(value = "JDBC URL was not specified by property %s", id = 10001004)
	String jdbcUrlNotSpecified(String url);

	@LogMessage(level = INFO)
	@Message(value = "using driver [%s] at URL [%s]", id = 10001005)
	void usingDriver(String driverClassName, String url);

	@LogMessage(level = WARN)
	@Message(value = "No JDBC Driver class was specified by property %s", id = 10001006)
	void jdbcDriverNotSpecified(String driver);

	@LogMessage(level = INFO)
	@Message(value = "JDBC isolation level: %s", id = 10001007)
	void jdbcIsolationLevel(String isolationLevelToString);

	@LogMessage(level = INFO)
	@Message(value = "Cleaning up connection pool [%s]", id = 10001008)
	void cleaningUpConnectionPool(String url);

	@LogMessage(level = WARN)
	@Message(value = "Problem closing pooled connection", id = 10001009)
	void unableToClosePooledConnection(@Cause SQLException e);
}
