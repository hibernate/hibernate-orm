/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * Logging related to Hibernate dialects
 */
@SubSystemLogging(
		name = DialectLogging.LOGGER_NAME,
		description = "Logging related to the dialects of SQL implemented by particular RDBMS"
)
@ValidIdRange( min = 35001, max = 36000)
@MessageLogger(projectCode = "HHH")
@Internal
public interface DialectLogging {
	String LOGGER_NAME = SubSystemLogging.BASE + ".dialect";
	Logger DIALECT_LOGGER = Logger.getLogger(LOGGER_NAME);
	DialectLogging DIALECT_MESSAGE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), DialectLogging.class, LOGGER_NAME );

	@LogMessage(level = DEBUG)
	@Message(value = "Using dialect: %s", id = 35001)
	void usingDialect(Dialect dialect);
}
