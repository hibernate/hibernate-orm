/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

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
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90001001, max = 90002000 )
@SubSystemLogging(
		name = SecondLevelCacheLogger.LOGGER_NAME,
		description = "Logging related to the second-level cache"
)
@Internal
public interface SecondLevelCacheLogger extends BasicLogger {
	String LOGGER_NAME = SubSystemLogging.BASE + ".cache";

	SecondLevelCacheLogger L2CACHE_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), SecondLevelCacheLogger.class, LOGGER_NAME );

	int NAMESPACE = 90001000;

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to start an already-started RegionFactory, probably because a SessionFactory was not closed." +
					" Using previously created RegionFactory.",
			id = NAMESPACE + 1
	)
	void attemptToStartAlreadyStartedCacheProvider();

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to stop an already-stopped JCacheRegionFactory.",
			id = NAMESPACE + 2
	)
	void attemptToStopAlreadyStoppedCacheProvider();

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for mutable entity [%s]",
			id = NAMESPACE + 3
	)
	void readOnlyCachingMutableEntity(String entity);

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for entity [%s] with mutable natural id",
			id = NAMESPACE + 4
	)
	void readOnlyCachingMutableNaturalId(String entity);

	@LogMessage(level = INFO)
	@Message(
			value = "A soft-locked cache entry in region [%s] with key [%s] was expired by the underlying cache." +
					" If this happens regularly, consider increasing the cache timeouts and/or capacity limits.",
			id = NAMESPACE + 5
	)
	void softLockedCacheExpired(String regionName, Object key);

	@LogMessage(level = WARN)
	@Message(
			value = "Missing cache region [%1$s] was created with provider-specific default policies." +
					" Explicitly configure the region and its policies, or disable this warning by setting '%2$s' to '%3$s'.",
			id = NAMESPACE + 6
	)
	@SuppressWarnings( "unused" ) // used by hibernate-jcache
	void missingCacheCreated(String regionName, String configurationPropertyToDisableKey, String configurationPropertyToDisableValue);

	@LogMessage(level = WARN)
	@Message(
			value = "Using legacy cache name [%2$s] because configuration could not be found for cache [%1$s]." +
					" Update configuration to rename cache [%2$s] to [%1$s].",
			id = NAMESPACE + 7
	)
	@SuppressWarnings( "unused" ) // used by hibernate-jcache
	void usingLegacyCacheName(String currentName, String legacyName);

	@LogMessage(level = WARN)
	@Message(
			value = "Cache region [%1$s] has the access type '%2$s' which is not supported by [%3$s]." +
					" Ensure cache implementation supports JTA transactions.",
			id = NAMESPACE + 8
	)
	@SuppressWarnings( "unused" ) // used by hibernate-jcache
	void nonStandardSupportForAccessType(String regionName, String accessType, String regionFactoryClass);

	@LogMessage(level = TRACE)
	@Message(
			value = "Caching query results in region '%s' with timestamp %s",
			id = NAMESPACE + 9
	)
	void cachingQueryResults(String regionName, long timestamp);

	@LogMessage(level = TRACE)
	@Message(
			value = "Checking cached query results in region '%s'",
			id = NAMESPACE + 10
	)
	void checkingCachedQueryResults(String regionName);

	@LogMessage(level = TRACE)
	@Message(
			value = "Query results were not found in cache",
			id = NAMESPACE + 11
	)
	void queryResultsNotFound();

	@LogMessage(level = TRACE)
	@Message(
			value = "Cached query results were stale",
			id = NAMESPACE + 12
	)
	void cachedQueryResultsStale();

	@LogMessage(level = TRACE)
	@Message(
		value = "Returning cached query results",
		id = NAMESPACE + 14
	)
	void returningCachedQueryResults();

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting entity second-level cache: %s",
		id = NAMESPACE + 15
	)
	void evictingEntityCache(String entityInfo);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting entity second-level cache: %s",
		id = NAMESPACE + 16
	)
	void evictingEntityCacheByRole(String role);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting natural id cache: %s",
		id = NAMESPACE + 17
	)
	void evictingNaturalIdCache(String role);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting collection second-level cache: %s",
		id = NAMESPACE + 18
	)
	void evictingCollectionCache(String collectionInfo);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting collection second-level cache: %s",
		id = NAMESPACE + 19
	)
	void evictingCollectionCacheByRole(String role);

	@LogMessage(level = TRACE)
	@Message(
			value = "Auto-evicting collection from second-level cache: %s"
					+ " (since 'hibernate.cache.auto_evict_collection_cache' is enabled)",
			id = NAMESPACE + 25
	)
	void autoEvictingCollectionCache(String collectionInfo);

	@LogMessage(level = TRACE)
	@Message(
			value = "Auto-evicting collection from second-level cache: %s"
					+ " (since 'hibernate.cache.auto_evict_collection_cache' is enabled)",
			id = NAMESPACE + 26
	)
	void autoEvictingCollectionCacheByRole(String collectionRole);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting query cache region: %s",
		id = NAMESPACE + 20
	)
	void evictingQueryCacheRegion(String regionName);

	@LogMessage(level = TRACE)
	@Message(
		value = "Evicting cache of all query regions",
		id = NAMESPACE + 21
	)
	void evictingAllQueryRegions();

	@LogMessage(level = TRACE)
	@Message(
		value = "Pre-invalidating space [%s], timestamp: %s",
		id = NAMESPACE + 22
	)
	void preInvalidatingSpace(String space, Long timestamp);

	@LogMessage(level = TRACE)
	@Message(
		value = "Invalidating space [%s], timestamp: %s",
		id = NAMESPACE + 23
	)
	void invalidatingSpace(String space, Long timestamp);

	@LogMessage(level = TRACE)
	@Message(
		value = "[%s] last update timestamp: %s, result set timestamp: %s",
		id = NAMESPACE + 24
	)
	void lastUpdateTimestampForSpace(String space, Long lastUpdate, Long timestamp);

	@LogMessage(level = INFO)
	@Message(
			value = "Second-level cache region factory [%s]",
			id = NAMESPACE + 28
	)
	void regionFactory(String name);

	@LogMessage(level = DEBUG)
	@Message(
			value = "Second-level cache disabled",
			id = NAMESPACE + 29
	)
	void noRegionFactory();

	@LogMessage(level = INFO)
	@Message(
			value = "Cannot default RegionFactory based on registered strategies as %s RegionFactory strategies were registered",
			id = NAMESPACE + 30
	)
	void cannotDefaultRegionFactory(int size);
}
