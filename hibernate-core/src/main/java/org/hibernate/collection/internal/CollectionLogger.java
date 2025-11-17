/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.internal;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import java.lang.invoke.MethodHandles;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Subsystem logging related to PersistentCollection runtime events
 */
@SubSystemLogging(
		name = CollectionLogger.NAME,
		description = "Logging related to persistent collection lifecycle and operations"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90030001, max = 90031000)
@Internal
public interface CollectionLogger extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".collection";

	CollectionLogger COLLECTION_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), CollectionLogger.class, NAME );

	@LogMessage(level = WARN)
	@Message(id = 90030001, value = "Unable to close temporary session used to load lazy collection associated to no session")
	void unableToCloseTemporarySession();

	@LogMessage(level = WARN)
	@Message(id = 90030002, value = "Detaching an uninitialized collection with enabled filters from a session: %s")
	void enabledFiltersWhenDetachFromSession(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(id = 90030004, value = "Attaching an uninitialized collection with queued operations to a session: %s")
	void queuedOperationWhenAttachToSession(String collectionInfoString);

	@LogMessage(level = INFO)
	@Message(id = 90030005, value = "Detaching an uninitialized collection with queued operations from a session: %s")
	void queuedOperationWhenDetachFromSession(String collectionInfoString);

	@LogMessage(level = DEBUG)
	@Message(id = 90030006, value = "Detaching an uninitialized collection with queued operations from a session due to rollback: %s")
	void queuedOperationWhenDetachFromSessionOnRollback(String collectionInfoString);

	@LogMessage(level = WARN)
	@Message(id = 90030007, value = "Cannot unset session in a collection because an unexpected session is defined."
									+ " A persistent collection may only be associated with one session at a time. %s")
	void logCannotUnsetUnexpectedSessionInCollection(String msg);

	@LogMessage(level = WARN)
	@Message(id = 90030008, value = "An unexpected session is defined for a collection, but the collection is not connected to that session."
									+ " A persistent collection may only be associated with one session at a time. Overwriting session. %s")
	void logUnexpectedSessionInCollectionNotConnected(String msg);
}
