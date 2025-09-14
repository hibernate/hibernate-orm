/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to UUID/GUID identifier generators
 */
@SubSystemLogging(
		name = UUIDLogger.NAME,
		description = "Logging related to UUID/GUID identifier generation"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90301, max = 90400)
@Internal
public interface UUIDLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".id.uuid";

	UUIDLogger UUID_MESSAGE_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(),
			UUIDLogger.class,
			NAME
	);

	@LogMessage(level = WARN)
	@Message(value = "Unable to instantiate UUID generation strategy class", id = 90301)
	void unableToInstantiateUuidGenerationStrategy(@Cause Exception ignore);

	@LogMessage(level = WARN)
	@Message(value = "Unable to locate requested UUID generation strategy class: %s", id = 90302)
	void unableToLocateUuidGenerationStrategy(String strategyClassName);

	@LogMessage(level = WARN)
	@Message(value = "GUID identifier generated: %s", id = 90305)
	void guidGenerated(String result);
}
