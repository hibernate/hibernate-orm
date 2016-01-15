/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.JndiInfinispanRegionFactory;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import javax.naming.NamingException;
import javax.transaction.SystemException;

import static org.jboss.logging.Logger.Level.*;

/**
 * The jboss-logging {@link MessageLogger} for the hibernate-infinispan module.  It reserves message ids ranging from
 * 25001 to 30000 inclusively.
 *
 * @author Radim Vansa &ltrvansa@redhat.com&gt;
 */
@MessageLogger(projectCode = "HHH")
public interface InfinispanMessageLogger extends BasicLogger {
   // Workaround for JBLOGGING-120: cannot add static interface method
	class Provider {
		public static InfinispanMessageLogger getLog(Class clazz) {
			return Logger.getMessageLogger(InfinispanMessageLogger.class, clazz.getName());
		}
	}

	@Message(value = "Pending-puts cache must not be clustered!", id = 25001)
	CacheException pendingPutsMustNotBeClustered();

	@Message(value = "Pending-puts cache must not be transactional!", id = 25002)
	CacheException pendingPutsMustNotBeTransactional();

	@LogMessage(level = WARN)
	@Message(value = "Pending-puts cache configuration should be a template.", id = 25003)
	void pendingPutsShouldBeTemplate();

	@Message(value = "Pending-puts cache must have expiration.max-idle set", id = 25004)
	CacheException pendingPutsMustHaveMaxIdle();

	@LogMessage(level = WARN)
	@Message(value = "Property '" + InfinispanRegionFactory.INFINISPAN_USE_SYNCHRONIZATION_PROP + "' is deprecated; 2LC with transactional cache must always use synchronizations.", id = 25005)
	void propertyUseSynchronizationDeprecated();

	@LogMessage(level = ERROR)
	@Message(value = "Custom cache configuration '%s' was requested for type %s but it was not found!", id = 25006)
	void customConfigForTypeNotFound(String cacheName, String type);

	@LogMessage(level = ERROR)
	@Message(value = "Custom cache configuration '%s' was requested for region %s but it was not found - using configuration by type (%s).", id = 25007)
	void customConfigForRegionNotFound(String templateCacheName, String regionName, String type);

	@Message(value = "Timestamps cache must not use eviction!", id = 25008)
	CacheException timestampsMustNotUseEviction();

	@Message(value = "Unable to start region factory", id = 25009)
	CacheException unableToStart(@Cause Throwable t);

	@Message(value = "Unable to create default cache manager", id = 25010)
	CacheException unableToCreateCacheManager(@Cause Throwable t);

	@Message(value = "Infinispan custom cache command factory not installed (possibly because the classloader where Infinispan lives couldn't find the Hibernate Infinispan cache provider)", id = 25011)
	CacheException cannotInstallCommandFactory();

	@LogMessage(level = WARN)
	@Message(value = "Requesting TRANSACTIONAL cache concurrency strategy but the cache is not configured as transactional.", id = 25012)
	void transactionalStrategyNonTransactionalCache();

	@LogMessage(level = WARN)
	@Message(value = "Requesting READ_WRITE cache concurrency strategy but the cache was configured as transactional.", id = 25013)
	void readWriteStrategyTransactionalCache();

	@LogMessage(level = WARN)
	@Message(value = "Setting eviction on cache using tombstones can introduce inconsistencies!", id = 25014)
	void evictionWithTombstones();

	@LogMessage(level = ERROR)
	@Message(value = "Failure updating cache in afterCompletion, will retry", id = 25015)
	void failureInAfterCompletion(@Cause Exception e);

	@LogMessage(level = ERROR)
	@Message(value = "Failed to end invalidating pending putFromLoad calls for key %s from region %s; the key won't be cached until invalidation expires.", id = 25016)
	void failedEndInvalidating(Object key, String name);

	@Message(value = "Unable to retrieve CacheManager from JNDI [%s]", id = 25017)
	CacheException unableToRetrieveCmFromJndi(String jndiNamespace);

	@LogMessage(level = WARN)
	@Message(value = "Unable to release initial context", id = 25018)
	void unableToReleaseContext(@Cause NamingException ne);

	@LogMessage(level = WARN)
	@Message(value = "Use non-transactional query caches for best performance!", id = 25019)
	void useNonTransactionalQueryCache();

	@LogMessage(level = ERROR)
	@Message(value = "Unable to broadcast invalidations as a part of the prepare phase. Rolling back.", id = 25020)
	void unableToRollbackInvalidationsDuringPrepare(@Cause Throwable t);

	@Message(value = "Could not suspend transaction", id = 25021)
	CacheException cannotSuspendTx(@Cause SystemException se);

	@Message(value = "Could not resume transaction", id = 25022)
	CacheException cannotResumeTx(@Cause Exception e);

	@Message(value = "Unable to get current transaction", id = 25023)
	CacheException cannotGetCurrentTx(@Cause SystemException e);

	@Message(value = "Failed to invalidate pending putFromLoad calls for key %s from region %s", id = 25024)
	CacheException failedInvalidatePendingPut(Object key, String regionName);

	@LogMessage(level = ERROR)
	@Message(value = "Failed to invalidate pending putFromLoad calls for region %s", id = 25025)
	void failedInvalidateRegion(String regionName);

	@Message(value = "Property '" + JndiInfinispanRegionFactory.CACHE_MANAGER_RESOURCE_PROP + "' not set", id = 25026)
	CacheException propertyCacheManagerResourceNotSet();

	@Message(value = "Timestamp cache cannot be configured with invalidation", id = 25027)
	CacheException timestampsMustNotUseInvalidation();
}
