/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10002001, max = 10002100 )
@SubSystemLogging(
		name = ConnectionProviderLogger.LOGGER_NAME,
		description = "Used to log details of database access through `ConnectionProvider`"
)
public interface ConnectionProviderLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".connections.provider";

	/**
	 * Static access to the logging instance
	 */
	ConnectionProviderLogger INSTANCE = Logger.getMessageLogger(
			ConnectionProviderLogger.class,
			LOGGER_NAME
	);


	@LogMessage(level = INFO)
	@Message(
			value = "Database info:\n%s",
			id = 10002001
	)
	void logConnectionDetails(DatabaseConnectionInfo databaseConnectionInfo);
}
