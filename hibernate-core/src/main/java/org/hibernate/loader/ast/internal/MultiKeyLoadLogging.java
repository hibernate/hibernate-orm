/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

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

/**
 * Logging related to loading a {@linkplain org.hibernate.loader.ast.spi.Loadable loadable}
 * by multiple "keys". The key can be primary, foreign or natural.
 *
 * @see org.hibernate.annotations.BatchSize
 * @see org.hibernate.Session#byMultipleIds
 * @see org.hibernate.Session#byMultipleNaturalId
 *
 * @author Steve Ebersole
 */
@MessageLogger(projectCode = "HHH")
@ValidIdRange(min = 90006101, max = 90007000)
@SubSystemLogging(
		name = MultiKeyLoadLogging.LOGGER_NAME,
		description = "Logging related to multi-key loading of entity and collection references"
)
@Internal
public interface MultiKeyLoadLogging extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".loader.multi";

	MultiKeyLoadLogging MULTI_KEY_LOAD_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), MultiKeyLoadLogging.class, LOGGER_NAME );

	// Enablement messages
	@LogMessage(level = TRACE)
	@Message(id = 90006101,
			value = "Batch fetching enabled for collection '%s' using ARRAY strategy with batch size %s")
	void enabledCollectionArray(String role, int domainBatchSize);

	@LogMessage(level = TRACE)
	@Message(id = 90006102,
			value = "Batch fetching enabled for collection '%s' using IN-predicate with batch size %s (%s)")
	void enabledCollectionInPredicate(String role, int sqlBatchSize, int domainBatchSize);

	@LogMessage(level = TRACE)
	@Message(id = 90006103,
			value = "Batch fetching enabled for entity '%s' using ARRAY strategy with batch size %s")
	void enabledEntityArray(String entityName, int domainBatchSize);

	@LogMessage(level = TRACE)
	@Message(id = 90006104,
			value = "Batch fetching enabled for entity '%s' using IN-predicate with batch size %s (%s)")
	void enabledEntityInPredicate(String entityName, int sqlBatchSize, int domainBatchSize);

	// Start operations
	@LogMessage(level = TRACE)
	@Message(id = 90006110, value = "Batch fetching collection: %s")
	void batchFetchingCollection(String collectionInfoString);

	@LogMessage(level = TRACE)
	@Message(id = 90006111, value = "Finishing initializing batch fetched collection: %s")
	void finishingInitializingBatchFetchedCollection(String collectionInfoString);

	@LogMessage(level = TRACE)
	@Message(id = 90006112, value = "Batch fetching entity: %s")
	void batchFetchingEntity(String entityInfoString);

	@LogMessage(level = TRACE)
	@Message(id = 90006113, value = "Unordered batch load starting: %s")
	void unorderedBatchLoadStarting(String entityName);

	@LogMessage(level = TRACE)
	@Message(id = 90006114, value = "Ordered MultiLoad starting: %s")
	void orderedMultiLoadStarting(String entityName);

	// Details
	@LogMessage(level = TRACE)
	@Message(id = 90006120, value = "Collection keys to initialize via batch fetching (%s) %s")
	void collectionKeysToInitialize(String collectionInfoString, Object[] keysToInitialize);

	@LogMessage(level = TRACE)
	@Message(id = 90006121, value = "Entity ids to initialize via batch fetching (%s) %s")
	void entityIdsToInitialize(String entityInfoString, Object[] idsToInitialize);

	@LogMessage(level = TRACE)
	@Message(id = 90006122, value = "Processing entity batch-fetch chunk (%s) %s - %s")
	void processingEntityBatchFetchChunk(String entityInfoString, int startIndex, int endIndex);

	@LogMessage(level = TRACE)
	@Message(id = 90006123, value = "Processing collection batch fetch chunk (%s) %s - %s")
	void processingCollectionBatchFetchChunk(String collectionInfoString, int startIndex, int endIndex);

	@LogMessage(level = TRACE)
	@Message(id = 90006124, value = "Finishing collection batch fetch chunk (%s) %s - %s (%s)")
	void finishingCollectionBatchFetchChunk(String collectionInfoString, int startIndex, int endIndex, int nonNullElementCount);
}
