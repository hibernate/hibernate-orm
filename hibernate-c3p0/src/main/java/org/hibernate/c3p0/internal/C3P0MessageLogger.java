/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.c3p0.internal;

import org.hibernate.internal.log.ConnectionInfoLogger;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-c3p0 module.  It reserves message ids ranging from
 * 10001 to 15000 inclusively.
 * <p>
 * New messages must be added after the last message defined to ensure message codes are unique.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange( min = 10001, max = 15000 )
@SubSystemLogging(
		name = C3P0MessageLogger.NAME,
		description = "Logging related to the C3P0 connection pool"
)
public interface C3P0MessageLogger extends ConnectionInfoLogger {
	String NAME = ConnectionInfoLogger.LOGGER_NAME + ".c3p0";

	C3P0MessageLogger C3P0_MSG_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), C3P0MessageLogger.class, NAME );

	/**
	 * Log a message (WARN) about conflicting {@code hibernate.c3p0.XYZ} and {@code c3p0.XYZ} settings
	 *
	 * @param hibernateStyle The {@code hibernate.c3p0} prefixed setting
	 * @param c3p0Style The {@code c3p0.} prefixed setting
	 */
	@LogMessage(level = WARN)
	@Message(value = "Both hibernate-style property '%1$s' and c3p0-style property '%2$s' have been set in Hibernate properties.\n"
			+ "Hibernate-style property '%1$s' will be used and c3p0-style property '%2$s' will be ignored", id = 10001)
	void bothHibernateAndC3p0StylesSet(String hibernateStyle,String c3p0Style);
}
