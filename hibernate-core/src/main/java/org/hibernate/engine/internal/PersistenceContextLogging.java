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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Sub-system logging related to PersistenceContext runtime events
 */
@SubSystemLogging(
		name = PersistenceContextLogging.NAME,
		description = "Logging related to persistence context operations and serialization"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90031001, max = 90032000)
@Internal
public interface PersistenceContextLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".persistenceContext";

	PersistenceContextLogging PERSISTENCE_CONTEXT_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), PersistenceContextLogging.class, NAME );

	@LogMessage(level = TRACE)
	@Message("Setting proxy identifier: %s")
	void settingProxyIdentifier(Object id);

	@LogMessage(level = WARN)
	@Message("Narrowing proxy to %s - this operation breaks ==")
	void narrowingProxy(Class<?> concreteProxyClass);

	@LogMessage(level = TRACE)
	@Message("Serializing persistence context")
	void serializingPersistenceContext();

	@LogMessage(level = TRACE)
	@Message("Deserializing persistence context")
	void deserializingPersistenceContext();

	@LogMessage(level = TRACE)
	@Message("Encountered pruned proxy")
	void encounteredPrunedProxy();

	@LogMessage(level = TRACE)
	@Message("Starting serialization of [%s] %s entries")
	void startingSerializationOfEntries(int count, String keysName);

	@LogMessage(level = TRACE)
	@Message("Starting deserialization of [%s] %s entries")
	void startingDeserializationOfEntries(int count, String keysName);

	// Merge and reachability logs (DEBUG/TRACE variants matching existing usage)
	@LogMessage(level = TRACE)
	@Message("Detached object being merged (corresponding with a managed entity) has a collection that [%s] the detached child")
	void detachedManagedContainsChild(String containsOrNot);

	@LogMessage(level = DEBUG)
	@Message("Detached proxy being merged has a collection that [%s] the managed child")
	void detachedProxyContainsManagedChild(String containsOrNot);

	@LogMessage(level = DEBUG)
	@Message("Detached proxy being merged has a collection that [%s] the detached child being merged")
	void detachedProxyContainsDetachedChild(String containsOrNot);

	@LogMessage(level = DEBUG)
	@Message("A detached object being merged (corresponding to a parent in parentsByChild) has an indexed collection that [%s] the detached child being merged. ")
	void detachedParentIndexedContainsDetachedChild(String containsOrNot);

	@LogMessage(level = DEBUG)
	@Message("A detached object being merged (corresponding to a managed entity) has an indexed collection that [%s] the detached child being merged. ")
	void detachedManagedIndexedContainsDetachedChild(String containsOrNot);
}
