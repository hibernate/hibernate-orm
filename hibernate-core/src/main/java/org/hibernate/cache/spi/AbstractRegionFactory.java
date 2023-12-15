/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRegionFactory implements RegionFactory {

	private final AtomicBoolean started = new AtomicBoolean( false );

	/**
	 * Legacy names that used to be the default for the query results region.
	 */
	public static final List<String> LEGACY_QUERY_RESULTS_REGION_UNQUALIFIED_NAMES = List.of(
			"org.hibernate.cache.spi.QueryResultsRegion",
			"org.hibernate.cache.internal.StandardQueryCache"
	);

	/**
	 * Legacy names that used to be the default for the update timestamps region.
	 */
	public static final List<String> LEGACY_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAMES = List.of(
			"org.hibernate.cache.spi.TimestampsRegion",
			"org.hibernate.cache.spi.UpdateTimestampsCache"
	);

	private Exception startingException;

	private SessionFactoryOptions options;

	protected final Lock thisLock = new ReentrantLock();

	protected boolean isStarted() {
		if ( started.get() ) {
			assert options != null;
			return true;
		}
		else {
			assert options == null;
			throw new IllegalStateException( "Cache provider not started", startingException );
		}
	}

	protected void verifyStarted() {
		if ( ! verifiedStartStatus() ) {
			throw new IllegalStateException( "Cache provider not started", startingException );
		}
	}

	protected boolean verifiedStartStatus() {
		if ( started.get() ) {
			assert options != null;
			return true;
		}
		else {
			assert options == null;
			return false;
		}
	}

	protected SessionFactoryOptions getOptions() {
		verifyStarted();
		return options;
	}

	@Override
	public final void start(SessionFactoryOptions settings, Map<String,Object> configValues) throws CacheException {
		if ( started.compareAndSet( false, true ) ) {
			thisLock.lock();
			try {
				this.options = settings;
				try {
					prepareForUse( settings, configValues );
					startingException = null;
				}
				catch ( Exception e ) {
					options = null;
					started.set( false );
					startingException = e;
				}
			} finally {
				thisLock.unlock();
			}
		}
		else {
			L2CACHE_LOGGER.attemptToStartAlreadyStartedCacheProvider();
		}
	}

	protected abstract void prepareForUse(SessionFactoryOptions settings, Map<String,Object> configValues);

	@Override
	public final void stop() {
		if ( started.compareAndSet( true, false ) ) {
			thisLock.lock();
			try {
				try {
					releaseFromUse();
				}
				finally {
					options = null;
					startingException = null;
				}
			} finally {
				thisLock.unlock();
			}
		}
		else {
			L2CACHE_LOGGER.attemptToStopAlreadyStoppedCacheProvider();
		}
	}

	protected abstract void releaseFromUse();

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public String qualify(String regionName) {
		return RegionNameQualifier.INSTANCE.qualify( regionName, options );
	}

	@Override
	public CacheTransactionSynchronization createTransactionContext(SharedSessionContractImplementor session) {
		return new StandardCacheTransactionSynchronization( this );
	}

	@Override
	public long nextTimestamp() {
		return SimpleTimestamper.next();
	}

	@Override
	public long getTimeout() {
		return SimpleTimestamper.timeOut();
	}
}
