/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.TRACE;

@MessageLogger(projectCode = "HHH")
@ValidIdRange(min=160000,max = 160100)
@SubSystemLogging(
		name = VersionLogger.LOGGER_NAME,
		description = "Logging related to versioning"
)
@Internal
public interface VersionLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".versioning";
	VersionLogger INSTANCE = Logger.getMessageLogger( MethodHandles.lookup(), VersionLogger.class, LOGGER_NAME );

	@LogMessage(level = TRACE)
	@Message(value = "Seeding version: %s", id = 160001)
	void seed(Object seededVersion);
	@LogMessage(level = TRACE)
	@Message(value = "Using initial version: %s", id = 160002)
	void initial(Object initialVersion);
	@LogMessage(level = TRACE)
	@Message(value = "Incrementing version: %s to: %s", id = 160003)
	void incrementing(Object oldVersion, Object newVersion);
}
