/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.internal;

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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to CurrentSessionContext implementations
 */
@SubSystemLogging(
		name = CurrentSessionLogging.NAME,
		description = "Logging related to current session context"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90070001, max = 90080000)
@Internal
public interface CurrentSessionLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".current_session";

	CurrentSessionLogging CURRENT_SESSION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), CurrentSessionLogging.class, NAME );

	@LogMessage(level = WARN)
	@Message(id = 90070001, value = "Session already bound on call to bind(); make sure you clean up your sessions")
	void alreadySessionBound();

	@LogMessage(level = TRACE)
	@Message("Allowing invocation [%s] to proceed to real session")
	void allowingInvocationToProceed(String methodName);

	@LogMessage(level = TRACE)
	@Message("Allowing invocation [%s] to proceed to real (closed) session")
	void allowingInvocationToProceedToClosedSession(String methodName);

	@LogMessage(level = TRACE)
	@Message("Allowing invocation [%s] to proceed to real (non-transacted) session")
	void allowingInvocationToProceedToNonTransactedSession(String methodName);

	@LogMessage(level = DEBUG)
	@Message(id = 90070011, value = "Unable to rollback transaction for orphaned session")
	void unableToRollbackTransactionForOrphanedSession(@Cause Throwable t);

	@LogMessage(level = DEBUG)
	@Message(id = 90070012, value = "Unable to close orphaned session")
	void unableToCloseOrphanedSession(@Cause Throwable t);

	@LogMessage(level = DEBUG)
	@Message(id = 90070013, value = "Unable to release generated current session on failed synchronization registration")
	void unableToReleaseGeneratedCurrentSessionOnFailedSynchronizationRegistration(@Cause Throwable t);
}
