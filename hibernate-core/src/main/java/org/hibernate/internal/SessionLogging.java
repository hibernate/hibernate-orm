/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

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
import java.util.UUID;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to Session/StatelessSession runtime events
 */
@SubSystemLogging(
		name = SessionLogging.NAME,
		description = "Logging related to session lifecycle and operations"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90010001, max = 90020000)
@Internal
public interface SessionLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".session";

	SessionLogging SESSION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SessionLogging.class, NAME );

	@LogMessage(level = DEBUG)
	@Message("Session creation specified 'autoJoinTransactions', "
			+ "which is invalid in conjunction with sharing JDBC connection between sessions; ignoring")
	void invalidAutoJoinTransactionsWithSharedConnection();

	@LogMessage(level = DEBUG)
	@Message("Session creation specified a 'PhysicalConnectionHandlingMode', "
			+ "which is invalid in conjunction with sharing JDBC connection between sessions; ignoring")
	void invalidPhysicalConnectionHandlingModeWithSharedConnection();

	@LogMessage(level = TRACE)
	@Message("Opened Session [%s] at timestamp: %s")
	void openedSession(UUID sessionIdentifier, long timestamp);

	@LogMessage(level = TRACE)
	@Message("Already closed")
	void alreadyClosed();

	@LogMessage(level = TRACE)
	@Message("Closing session [%s]")
	void closingSession(UUID sessionIdentifier);

	@LogMessage(level = WARN)
	@Message(id = 90010101, value = "Closing shared session with unprocessed transaction completion actions")
	void closingSharedSessionWithUnprocessedTxCompletions();

	@LogMessage(level = TRACE)
	@Message("Forcing-closing session since factory is already closed")
	void forcingCloseBecauseFactoryClosed();

	@LogMessage(level = TRACE)
	@Message("Skipping auto-flush since the session is closed")
	void skippingAutoFlushSessionClosed();

	@LogMessage(level = TRACE)
	@Message("Automatically flushing session")
	void automaticallyFlushingSession();

	@LogMessage(level = TRACE)
	@Message("Automatically closing session")
	void automaticallyClosingSession();

	@LogMessage(level = TRACE)
	@Message("%s remove orphan before updates: [%s]")
	void removeOrphanBeforeUpdates(String timing, String entityInfo);

	@LogMessage(level = TRACE)
	@Message("Initializing proxy: %s")
	void initializingProxy(String entityInfo);

	@LogMessage(level = TRACE)
	@Message("Clearing effective entity graph for subsequent select")
	void clearingEffectiveEntityGraph();

	@LogMessage(level = TRACE)
	@Message("Flushing to force deletion of re-saved object: %s")
	void flushingToForceDeletion(String entityInfo);

	@LogMessage(level = TRACE)
	@Message("Before transaction completion processing")
	void beforeTransactionCompletion();

	@LogMessage(level = TRACE)
	@Message("After transaction completion processing (successful=%s, delayed=%s)")
	void afterTransactionCompletion(boolean successful, boolean delayed);

	@LogMessage(level = ERROR)
	@Message(id = 90010102, value = "JDBC exception executing SQL; transaction rolled back")
	void jdbcExceptionThrownWithTransactionRolledBack(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 90010103, value = "Ignoring EntityNotFoundException for '%s.%s'")
	void ignoringEntityNotFound(String entityName, Object id);

	@LogMessage(level = WARN)
	@Message(id = 90010104, value = "Property '%s' is not serializable, value won't be set")
	void nonSerializableProperty(String propertyName);

	@LogMessage(level = WARN)
	@Message(id = 90010105, value = "Property having key null is illegal, value won't be set")
	void nullPropertyKey();

	@LogMessage(level = TRACE)
	@Message("Serializing Session [%s]")
	void serializingSession(UUID sessionIdentifier);

	@LogMessage(level = TRACE)
	@Message("Deserializing Session [%s]")
	void deserializingSession(UUID sessionIdentifier);

	@LogMessage(level = ERROR)
	@Message(id = 90010106, value = "Exception in interceptor beforeTransactionCompletion()")
	void exceptionInBeforeTransactionCompletionInterceptor(@Cause Throwable e);

	@LogMessage(level = ERROR)
	@Message(id = 90010107, value = "Exception in interceptor afterTransactionCompletion()")
	void exceptionInAfterTransactionCompletionInterceptor(@Cause Throwable e);

	// StatelessSession-specific

	@LogMessage(level = TRACE)
	@Message("Refreshing transient %s")
	void refreshingTransient(String entityInfo);

	@LogMessage(level = TRACE)
	@Message("Initializing collection %s")
	void initializingCollection(String collectionInfo);

	@LogMessage(level = TRACE)
	@Message("Collection initialized from cache")
	void collectionInitializedFromCache();

	@LogMessage(level = TRACE)
	@Message("Collection initialized")
	void collectionInitialized();

	@LogMessage(level = TRACE)
	@Message("Entity proxy found in session cache")
	void entityProxyFoundInSessionCache();

	@LogMessage(level = DEBUG)
	@Message("Ignoring NO_PROXY to honor laziness")
	void ignoringNoProxyToHonorLaziness();

	@LogMessage(level = TRACE)
	@Message("Creating a HibernateProxy for to-one association with subclasses to honor laziness")
	void creatingHibernateProxyToHonorLaziness();

	@LogMessage(level = TRACE)
	@Message("Collection fetched from cache")
	void collectionFetchedFromCache();

	@LogMessage(level = TRACE)
	@Message("Collection fetched")
	void collectionFetched();
}
