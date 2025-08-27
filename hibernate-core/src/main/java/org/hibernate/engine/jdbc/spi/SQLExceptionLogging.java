/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Responsible for logging SQL {@linkplain java.sql.SQLException errors}
 * and {@linkplain java.sql.SQLWarning warnings}.
 *
 * @author Gavin King
 * @since 7
 */
@MessageLogger(projectCode = "HHH")
public interface SQLExceptionLogging extends BasicLogger {
	String ERROR_NAME = SubSystemLogging.BASE + ".jdbc.error";
	String WARN_NAME = SubSystemLogging.BASE + ".jdbc.warn";

	SQLExceptionLogging ERROR_LOG = Logger.getMessageLogger( MethodHandles.lookup(), SQLExceptionLogging.class, ERROR_NAME );
	SQLExceptionLogging WARNING_LOG = Logger.getMessageLogger( MethodHandles.lookup(), SQLExceptionLogging.class, WARN_NAME );

	@LogMessage(level = WARN)
	@Message(value = "ErrorCode: %s, SQLState: %s", id = 247)
	void logErrorCodes(int errorCode, String sqlState);
}
