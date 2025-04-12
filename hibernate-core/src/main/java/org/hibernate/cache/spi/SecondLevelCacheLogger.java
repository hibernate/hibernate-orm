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

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90001001, max = 90002000 )
@SubSystemLogging(
		name = SecondLevelCacheLogger.LOGGER_NAME,
		description = "Logging related to Hibernate second-level caching"
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
			value = "Read-only caching was requested for mutable natural-id for entity [%s]",
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

}
