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
 * Subsystem logging related to event listeners
 */
@SubSystemLogging(
		name = EventListenerLogging.NAME,
		description = "Logging related to event listeners and event listener services"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90060001, max = 90070000)
@Internal
public interface EventListenerLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".event";

	EventListenerLogging EVENT_LISTENER_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), EventListenerLogging.class, NAME );

	// Load

	@LogMessage(level = TRACE)
	@Message(id = 90060008, value = "Loading entity %s")
	void loadingEntity(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060005, value = "Entity proxy found in session cache")
	void entityProxyFoundInSessionCache(); // dupe of SessionLogging

	@LogMessage(level = DEBUG)
	@Message(id = 90060007, value = "Ignoring NO_PROXY to honor laziness")
	void ignoringNoProxyToHonorLaziness(); // dupe of SessionLogging

	@LogMessage(level = TRACE)
	@Message(id = 90060050, value = "Creating new proxy for entity")
	void creatingNewProxy();

	@LogMessage(level = TRACE)
	@Message(id = 90060051, value = "Searching caches for entity %s")
	void resolving(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060006, value = "Entity found in persistence context")
	void entityResolvedInPersistenceContext();

	@LogMessage(level = TRACE)
	@Message(id = 90060052, value = "Entity found in second-level cache %s")
	void entityResolvedInCache(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060053, value = "Entity not found in any cache, loading from datastore %s")
	void entityNotResolvedInCache(String infoString);

	// Auto-flush

	@LogMessage(level = TRACE)
	@Message(id = 90060038, value = "Need to execute flush")
	void needToExecuteFlush();

	@LogMessage(level = TRACE)
	@Message(id = 90060039, value = "No need to execute flush")
	void noNeedToExecuteFlush();

	// Flush

	@LogMessage(level = DEBUG)
	@Message(id = 90060071, value = "Flushed: %s insertions, %s updates, %s deletions to %s objects")
	void flushedEntitiesSummary(int insertions, int updates, int deletions, int objects);

	@LogMessage(level = DEBUG)
	@Message(id = 90060072, value = "Flushed: %s (re)creations, %s updates, %s removals to %s collections")
	void flushedCollectionsSummary(int recreations, int updates, int removals, int collections);

	@LogMessage(level = TRACE)
	@Message(id = 90060011, value = "Flushing session")
	void flushingSession();

	@LogMessage(level = TRACE)
	@Message(id = 90060012, value = "Processing flush-time cascades")
	void processingFlushTimeCascades();

	@LogMessage(level = TRACE)
	@Message(id = 90060013, value = "Dirty checking collections")
	void dirtyCheckingCollections();

	@LogMessage(level = TRACE)
	@Message(id = 90060014, value = "Flushing entities and processing referenced collections")
	void flushingEntitiesAndProcessingReferencedCollections();

	@LogMessage(level = TRACE)
	@Message(id = 90060015, value = "Processing unreferenced collections")
	void processingUnreferencedCollections();

	@LogMessage(level = TRACE)
	@Message(id = 90060016, value = "Scheduling collection removes, (re)creates, and updates")
	void schedulingCollectionOperations();

	@LogMessage(level = TRACE)
	@Message(id = 90060017, value = "Executing flush")
	void executingFlush();

	@LogMessage(level = TRACE)
	@Message(id = 90060018, value = "Post flush")
	void postFlush();

	// Flush entity

	@LogMessage(level = TRACE)
	@Message(id = 90060059, value = "Updating immutable, deleted entity %s")
	void updatingImmutableDeletedEntity(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060060, value = "Updating non-modifiable, deleted entity %s")
	void updatingNonModifiableDeletedEntity(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060061, value = "Updating deleted entity %s")
	void updatingDeletedEntity(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060062, value = "Updating entity %s")
	void updatingEntity(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060063, value = "Found dirty properties [%s] (%s)")
	void foundDirtyProperties(String entityInfo, String dirtyPropertyNames);

	// Merge

	@LogMessage(level = TRACE)
	@Message(id = 90060019, value = "Ignoring uninitialized proxy in merge")
	void ignoringUninitializedProxy();

	@LogMessage(level = TRACE)
	@Message(id = 90060020, value = "Ignoring uninitialized enhanced proxy in merge")
	void ignoringUninitializedEnhancedProxy();

	@LogMessage(level = TRACE)
	@Message(id = 90060021, value = "Already in merge process")
	void alreadyInMergeProcess();

	@LogMessage(level = TRACE)
	@Message(id = 90060022, value = "Already in merge context; adding to merge process")
	void alreadyInMergeContext();

	@LogMessage(level = TRACE)
	@Message(id = 90060023, value = "Ignoring persistent instance %s")
	void ignoringPersistentInstance(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060024, value = "Merging transient instance %s")
	void mergingTransientInstance(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060025, value = "Merging detached instance %s")
	void mergingDetachedInstance(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060026, value = "Detached instance not found in database")
	void detachedInstanceNotFoundInDatabase();

	// Replicate

	@LogMessage(level = TRACE)
	@Message(id = 90060027, value = "Uninitialized proxy passed to replicate()")
	void uninitializedProxyPassedToReplicate();

	@LogMessage(level = TRACE)
	@Message(id = 90060028, value = "Ignoring persistent instance passed to replicate()")
	void ignoringPersistentInstancePassedToReplicate();

	@LogMessage(level = TRACE)
	@Message(id = 90060029, value = "Found existing row for %s")
	void foundExistingRowFor(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060030, value = "No need to replicate")
	void noNeedToReplicate();

	@LogMessage(level = TRACE)
	@Message(id = 90060031, value = "No existing row, replicating new instance %s")
	void noExistingRowReplicatingNewInstance(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060032, value = "Replicating changes to %s")
	void replicatingChangesTo(String infoString);

	// Initialize collection

	@LogMessage(level = TRACE)
	@Message(id = 90060033, value = "Initializing collection %s")
	void initializingCollection(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060034, value = "Collection initialized from cache")
	void collectionInitializedFromCache();

	@LogMessage(level = TRACE)
	@Message(id = 90060035, value = "Collection not cached")
	void collectionNotCached();

	@LogMessage(level = TRACE)
	@Message(id = 90060036, value = "Collection initialized")
	void collectionInitialized();

	@LogMessage(level = TRACE)
	@Message(id = 90060037, value = "Disregarding cached version (if any) of collection due to enabled filters")
	void disregardingCachedVersionDueToEnabledFilters();

	// Persist

	@LogMessage(level = TRACE)
	@Message(id = 90060058, value = "Persisting %s")
	void persisting(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060040, value = "Persisting transient instance")
	void persistingTransientInstance();

	@LogMessage(level = TRACE)
	@Message(id = 90060041, value = "Unscheduling entity deletion %s")
	void unschedulingEntityDeletion(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060076, value ="Generated identifier [%s] using generator '%s'")
	void generatedId(String loggableString, String name);

	// Refresh

	@LogMessage(level = TRACE)
	@Message(id = 90060044, value = "Refreshing %s")
	void refreshing(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060043, value = "Refreshing transient %s")
	void refreshingTransient(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060042, value = "Already refreshed")
	void alreadyRefreshed();

	// Delete

	@LogMessage(level = TRACE)
	@Message(id = 90060047, value = "Deleting %s")
	void deleting(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060045, value = "Deleted entity was not associated with current session")
	void deletedEntityNotAssociatedWithSession();

	@LogMessage(level = TRACE)
	@Message(id = 90060046, value = "Already handled transient entity; skipping")
	void alreadyHandledTransient();

	@LogMessage(level = DEBUG)
	@Message(id = 90060001, value = "Flushing and evicting managed instance of type [%s] before removing detached instance with same id")
	void flushAndEvictOnRemove(String entityName);

	@LogMessage(level = DEBUG)
	@Message(id = 90060002, value = "Handling transient entity in delete processing")
	void handlingTransientEntity();

	@LogMessage(level = TRACE)
	@Message(id = 90060009, value = "Deleting a persistent instance")
	void deletingPersistentInstance();

	@LogMessage(level = TRACE)
	@Message(id = 90060010, value = "Persistent instance was already deleted or scheduled for deletion")
	void alreadyDeleted();

	// Evict

	@LogMessage(level = TRACE)
	@Message(id = 90060048, value = "Evicting %s")
	void evicting(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060049, value = "Evicting collection %s")
	void evictingCollection(String collectionInfo);

	// Lock

	@LogMessage(level = TRACE)
	@Message(id = 90060064, value = "Reassociating transient instance: %s")
	void reassociatingTransientInstance(String infoString);

	// EntityState

//	@LogMessage(level = TRACE)
//	@Message(id = 90060054, value = "Persistent instance of: %s")
//	void persistentInstance(String loggableName);
//
//	@LogMessage(level = TRACE)
//	@Message(id = 90060055, value = "Deleted instance of: %s")
//	void deletedInstance(String loggableName);
//
//	@LogMessage(level = TRACE)
//	@Message(id = 90060056, value = "Transient instance of: %s")
//	void transientInstance(String loggableName);
//
//	@LogMessage(level = TRACE)
//	@Message(id = 90060057, value = "Detached instance of: %s")
//	void detachedInstance(String loggableName);

	// Reattach / wrap

	@LogMessage(level = TRACE)
	@Message(id = 90060066, value = "Collection dereferenced while transient %s")
	void collectionDereferencedWhileTransient(String infoString);

	@LogMessage(level = TRACE)
	@Message(id = 90060067, value = "Wrapped collection in role: %s")
	void wrappedCollectionInRole(String role);
}
