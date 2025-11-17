/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.transaction.internal;

import jakarta.transaction.Synchronization;
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

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

/**
 * Logging interface for local Synchronization registry operations.
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90008001, max = 90009000)
@SubSystemLogging(
		name = SynchronizationLogging.LOGGER_NAME,
		description = "Logging related to local Synchronization registry management"
)
@Internal
public interface SynchronizationLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".synchronization";

	SynchronizationLogging SYNCHRONIZATION_LOGGER = Logger.getMessageLogger(
			MethodHandles.lookup(), SynchronizationLogging.class, LOGGER_NAME
	);

	int NAMESPACE = 90008000;

	@LogMessage(level = TRACE)
	@Message(
			value = "Notifying Synchronizations (before completion)",
			id = NAMESPACE + 1
	)
	void notifyingSynchronizationsBefore();

	@LogMessage(level = TRACE)
	@Message(
			value = "Notifying Synchronizations (after completion with status %s)",
			id = NAMESPACE + 2
	)
	void notifyingSynchronizationsAfter(int status);

	@LogMessage(level = TRACE)
	@Message(
			value = "Clearing local Synchronizations",
			id = NAMESPACE + 3
	)
	void clearingSynchronizations();

	@LogMessage(level = INFO)
	@Message(
			value = "Synchronization [%s] was already registered",
			id = NAMESPACE + 4
	)
	void synchronizationAlreadyRegistered(Synchronization synchronization);

	@LogMessage(level = ERROR)
	@Message(
			value = "Exception calling user Synchronization [%s]",
			id = NAMESPACE + 5
	)
	void synchronizationFailed(Synchronization synchronization, @Cause Throwable t);
}
