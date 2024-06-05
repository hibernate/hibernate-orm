/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import java.sql.SQLException;
import java.util.Properties;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10001001, max = 10001500 )
@SubSystemLogging(
		name = ConnectionPoolingLogger.LOGGER_NAME,
		description = "Logging related to connection pooling"
)
public interface ConnectionPoolingLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".connections.pooling";

	/**
	 * Static access to the logging instance
	 */
	Logger CONNECTIONS_LOGGER = Logger.getLogger( LOGGER_NAME );
	ConnectionPoolingLogger CONNECTIONS_MESSAGE_LOGGER = Logger.getMessageLogger( ConnectionPoolingLogger.class, LOGGER_NAME );

	@LogMessage(level = INFO)
	@Message(value = "Connection properties: %s", id = 10001001)
	void connectionProperties(Properties connectionProps);

	@LogMessage(level = WARN)
	@Message(value = "Using built-in connection pool (not intended for production use)", id = 10001002)
	void usingHibernateBuiltInConnectionPool();

	@Message(value = "No JDBC URL specified by property %s", id = 10001004)
	String jdbcUrlNotSpecified(String property);

	@LogMessage(level = INFO)
	@Message(value = "No JDBC driver class specified by %s", id = 10001010)
	void noDriver(String property);

	@LogMessage(level = WARN)
	@Message(id = 10001006, value = "No JDBC Driver class was specified by property `jakarta.persistence.jdbc.driver`, `hibernate.driver` or `javax.persistence.jdbc.driver`")
	void jdbcDriverNotSpecified();

	@LogMessage(level = INFO)
	@Message(value = "Cleaning up connection pool [%s]", id = 10001008)
	void cleaningUpConnectionPool(String url);

	@LogMessage(level = WARN)
	@Message(value = "Problem closing pooled connection", id = 10001009)
	void unableToClosePooledConnection(@Cause SQLException e);

	@LogMessage(level = INFO)
	@Message(value = "Connection pool size: %s (min=%s)", id = 10001115)
	void hibernateConnectionPoolSize(int poolSize, int minSize);

	@LogMessage(level = ERROR)
	@Message(value = "Error closing connection", id = 10001284)
	void unableToCloseConnection(@Cause Exception e);
}
