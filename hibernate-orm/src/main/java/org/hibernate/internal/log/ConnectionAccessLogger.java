/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.log;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10001501, max = 10002000 )
public interface ConnectionAccessLogger extends BasicLogger {
	String LOGGER_NAME = "org.hibernate.orm.connections.access";

	/**
	 * Static access to the logging instance
	 */
	ConnectionAccessLogger INSTANCE = Logger.getMessageLogger(
			ConnectionAccessLogger.class,
			LOGGER_NAME
	);


	@LogMessage(level = INFO)
	@Message(
			value = "Connection obtained from JdbcConnectionAccess [%s] for (non-JTA) DDL execution was not in auto-commit mode; " +
					"the Connection 'local transaction' will be committed and the Connection will be set into auto-commit mode.",
			id = 10001501
	)
	void informConnectionLocalTransactionForNonJtaDdl(JdbcConnectionAccess jdbcConnectionAccess);
}
