/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.cache.CacheException;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;

/**
 * This class represents Infinispan cache parameters that can be configured via hibernate configuration properties
 * for either general entity/collection/query/timestamp data type caches and overrides for individual entity or
 * collection caches. Configuration these properties override previously defined properties in XML file.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TypeOverrides implements Serializable {

	private final Set<String> overridden = new HashSet<String>();

	private String cacheName;

	private EvictionStrategy evictionStrategy;

	private long evictionWakeUpInterval;

	private int evictionMaxEntries;

	private long expirationLifespan;

	private long expirationMaxIdle;

	private boolean isExposeStatistics;

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public EvictionStrategy getEvictionStrategy() {
		return evictionStrategy;
	}

   /**
    * Sets eviction strategy for cached type.
    *
    * @param evictionStrategy String defining eviction strategy allowed.
    *                         Possible values are defined in {@link EvictionStrategy}
    */
	public void setEvictionStrategy(String evictionStrategy) {
		markAsOverriden( "evictionStrategy" );
		this.evictionStrategy = EvictionStrategy.valueOf( uc( evictionStrategy ) );
	}

	public long getEvictionWakeUpInterval() {
		return evictionWakeUpInterval;
	}

   /**
    * Sets how often eviction process should be run for the cached type.
    *
    * @param evictionWakeUpInterval long representing the frequency for executing
    *                               the eviction process, in milliseconds
    *
    */
	public void setEvictionWakeUpInterval(long evictionWakeUpInterval) {
		markAsOverriden( "evictionWakeUpInterval" );
		this.evictionWakeUpInterval = evictionWakeUpInterval;
	}

	public int getEvictionMaxEntries() {
		return evictionMaxEntries;
	}

   /**
    * Maximum number of entries in a cache for this cached type. Cache size
    * is guaranteed  not to exceed upper limit specified by max entries.
    * However, due to the nature of eviction it is unlikely to ever be
    * exactly maximum number of entries specified here.
    *
    * @param evictionMaxEntries number of maximum cache entries
    */
	public void setEvictionMaxEntries(int evictionMaxEntries) {
		markAsOverriden( "evictionMaxEntries" );
		this.evictionMaxEntries = evictionMaxEntries;
	}

	public long getExpirationLifespan() {
		return expirationLifespan;
	}

   /**
    * Maximum lifespan of a cache entry, after which the entry is expired
    * cluster-wide, in milliseconds. -1 means the entries never expire.
    *
    * @param expirationLifespan long representing the maximum lifespan,
    *                           in milliseconds, for a cached entry before
    *                           it's expired
    */
	public void setExpirationLifespan(long expirationLifespan) {
		markAsOverriden( "expirationLifespan" );
		this.expirationLifespan = expirationLifespan;
	}

	public long getExpirationMaxIdle() {
		return expirationMaxIdle;
	}

   /**
    * Maximum idle time a cache entry will be maintained in the cache, in
    * milliseconds. If the idle time is exceeded, the entry will be expired
    * cluster-wide. -1 means the entries never expire.
    *
    * @param expirationMaxIdle long representing the maximum idle time, in
    *                          milliseconds, for a cached entry before it's
    *                          expired
    */
	public void setExpirationMaxIdle(long expirationMaxIdle) {
		markAsOverriden( "expirationMaxIdle" );
		this.expirationMaxIdle = expirationMaxIdle;
	}

	public boolean isExposeStatistics() {
		return isExposeStatistics;
	}

   /**
    * Enable statistics gathering and reporting via JMX.
    *
    * @param isExposeStatistics boolean indicating whether statistics should
    *                           be enabled or disabled
    */
	public void setExposeStatistics(boolean isExposeStatistics) {
		markAsOverriden( "isExposeStatistics" );
		this.isExposeStatistics = isExposeStatistics;
	}

   /**
    * Apply the configuration overrides in this {@link TypeOverrides} instance
    * to the cache configuration builder passed as parameter.
    *
    * @param builder cache configuration builder on which to apply
    *                configuration overrides
    */
	public void applyTo(ConfigurationBuilder builder) {
		if ( overridden.contains( "evictionStrategy" ) ) {
			builder.eviction().strategy( evictionStrategy );
		}
		if ( overridden.contains( "evictionWakeUpInterval" ) ) {
			builder.expiration().wakeUpInterval( evictionWakeUpInterval );
		}
		if ( overridden.contains( "evictionMaxEntries" ) ) {
			builder.eviction().maxEntries( evictionMaxEntries );
		}
		if ( overridden.contains( "expirationLifespan" ) ) {
			builder.expiration().lifespan( expirationLifespan );
		}
		if ( overridden.contains( "expirationMaxIdle" ) ) {
			builder.expiration().maxIdle( expirationMaxIdle );
		}
		if ( overridden.contains( "isExposeStatistics" ) && isExposeStatistics ) {
			builder.jmxStatistics().enable();
		}
	}

   /**
    * Validate the configuration for this cached type.
    *
    * @param cfg configuration to validate
    * @throws CacheException if validation fails
    */
	public void validateInfinispanConfiguration(Configuration cfg) throws CacheException {
		// no-op, method overriden
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ '{' + "cache=" + cacheName
				+ ", strategy=" + evictionStrategy
				+ ", wakeUpInterval=" + evictionWakeUpInterval
				+ ", maxEntries=" + evictionMaxEntries
				+ ", lifespan=" + expirationLifespan
				+ ", maxIdle=" + expirationMaxIdle
				+ '}';
	}

	private String uc(String s) {
		return s == null ? null : s.toUpperCase( Locale.ENGLISH );
	}

	private void markAsOverriden(String fieldName) {
		overridden.add( fieldName );
	}
}
