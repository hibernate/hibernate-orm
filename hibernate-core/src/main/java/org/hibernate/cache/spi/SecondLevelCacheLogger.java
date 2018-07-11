/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.NavigableRole;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author Steve Ebersole
 */
@MessageLogger( projectCode = "HHH" )
@ValidIdRange( min = 90001001, max = 90002000 )
public interface SecondLevelCacheLogger extends BasicLogger {
	SecondLevelCacheLogger INSTANCE = Logger.getMessageLogger(
			SecondLevelCacheLogger.class,
			"org.hibernate.orm.cache"
	);

	int NAMESPACE = 90001000;

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to restart an already started RegionFactory.  Use sessionFactory.close() between " +
					"repeated calls to buildSessionFactory. Using previously created RegionFactory.",
			id = NAMESPACE + 1
	)
	void attemptToStartAlreadyStartedCacheProvider();

	@LogMessage(level = WARN)
	@Message(
			value = "Attempt to restop an already stopped JCacheRegionFactory.",
			id = NAMESPACE + 2
	)
	void attemptToStopAlreadyStoppedCacheProvider();

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for mutable entity [%s]",
			id = NAMESPACE + 3
	)
	void readOnlyCachingMutableEntity(NavigableRole navigableRole);

	@LogMessage( level = WARN )
	@Message(
			value = "Read-only caching was requested for mutable natural-id for entity [%s]",
			id = NAMESPACE + 4
	)
	void readOnlyCachingMutableNaturalId(NavigableRole navigableRole);

	/**
	 * Log a message (WARN) about expiry of soft-locked region.
	 */
	@LogMessage(level = INFO)
	@Message(
			value = "Cache[%s] Key[%s]\n" +
					"A soft-locked cache entry was expired by the underlying cache. If this happens regularly you " +
					"should consider increasing the cache timeouts and/or capacity limits",
			id = NAMESPACE + 5
	)
	void softLockedCacheExpired(String regionName, Object key);

	@LogMessage(level = WARN)
	@Message(
			value = "Missing cache[%1$s] was created on-the-fly." +
					" The created cache will use a provider-specific default configuration:" +
					" make sure you defined one." +
					" You can disable this warning by setting '%2$s' to '%3$s'.",
			id = NAMESPACE + 6
	)
	void missingCacheCreated(String regionName, String configurationPropertyToDisableKey, String configurationPropertyToDisableValue);

	@LogMessage(level = WARN)
	@Message(
			value = "Using legacy cache name [%2$s] because configuration could not be found for cache [%1$s]." +
					" Update your configuration to rename cache [%2$s] to [%1$s].",
			id = NAMESPACE + 7
	)
	void usingLegacyCacheName(String currentName, String legacyName);

	@LogMessage(level = WARN)
	@Message(
			value = "Cache [%1$s] uses the [%2$s] access type, but [%3$s] does not support it natively." +
					" Make sure your cache implementation supports JTA transactions.",
			id = NAMESPACE + 8
	)
	void nonStandardSupportForAccessType(String key, String accessType, String regionName);

}
