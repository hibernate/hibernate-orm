/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

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

/**
 * Subsystem logging related to EntityCopyObservers
 */
@SubSystemLogging(
		name = EntityCopyLogging.NAME,
		description = "Logging related to EntityCopyObservers"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90070001, max = 90080000)
@Internal
public interface EntityCopyLogging extends BasicLogger {
	String NAME = EventListenerLogging.NAME + ".copy";

	EntityCopyLogging EVENT_COPY_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), EntityCopyLogging.class, NAME );


	// EntityCopyObserver

	@LogMessage(level = TRACE)
	@Message(id = 90070001, value = "More than one representation of the same persistent entity being merged: %s")
	void duplicateRepresentationBeingMerged(String infoString);

	@LogMessage(level = DEBUG)
	@Message(id = 90070002, value = "Summary: number of %s entities with multiple representations merged: %d")
	void mergeSummaryMultipleRepresentations(String entityName, int count);

	@LogMessage(level = DEBUG)
	@Message(id = 90070003, value = "No entity copies merged")
	void noEntityCopiesMerged();

	@LogMessage(level = DEBUG)
	@Message(id = 90070010, value = "Details: merged %d representations of the same entity %s being merged: %s; resulting managed entity: [%s]")
	void mergeDetails(int numberOfRepresentations, String entityInfo, String mergedEntitiesList, String managedEntityString);

	// EntityCopyObserverFactoryInitiator

	@LogMessage(level = TRACE)
	@Message(id = 90070100, value = "Configured EntityCopyObserver strategy: %s")
	void configuredEntityCopyObserverStrategy(String strategyName);

	@LogMessage(level = TRACE)
	@Message(id = 90070101, value = "Configured EntityCopyObserver is a custom implementation of type '%s'")
	void configuredEntityCopyObserverCustomImplementation(String typeName);
}
