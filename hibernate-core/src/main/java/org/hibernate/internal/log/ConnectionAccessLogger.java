/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.log;

import org.hibernate.Internal;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.TRACE;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 10001501, max = 10002000 )
@SubSystemLogging(
		name = ConnectionAccessLogger.LOGGER_NAME,
		description = "Used to log details around use of `JdbcConnectionAccess`"
)
@Internal
public interface ConnectionAccessLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".connections.access";

	/**
	 * Static access to the logging instance
	 */
	ConnectionAccessLogger INSTANCE = Logger.getMessageLogger(
			MethodHandles.lookup(),
			ConnectionAccessLogger.class,
			LOGGER_NAME
	);


	@LogMessage(level = TRACE)
	@Message(
			value = "Connection obtained from JdbcConnectionAccess for (non-JTA) DDL execution was not in auto-commit mode; " +
					"the Connection 'local transaction' will be committed and the Connection will be set into auto-commit mode.",
			id = 10001501
	)
	void informConnectionLocalTransactionForNonJtaDdl();
}
