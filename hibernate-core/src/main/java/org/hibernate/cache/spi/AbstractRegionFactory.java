/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.cache.spi.support.SimpleTimestamper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRegionFactory implements RegionFactory {

	private final AtomicBoolean started = new AtomicBoolean( false );

	private SessionFactoryOptions options;


	protected boolean isStarted() {
		if ( started.get() ) {
			assert options != null;
			return true;
		}
		else {
			assert options == null;
			throw new IllegalStateException( "Cache provider not started" );
		}
	}

	protected void verifyStarted() {
		if ( ! verifiedStartStatus() ) {
			throw new IllegalStateException( "Cache provider not started" );
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
	public final void start(SessionFactoryOptions settings, Map configValues) throws CacheException {
		if ( started.compareAndSet( false, true ) ) {
			synchronized (this) {
				this.options = settings;
				try {
					prepareForUse( settings, configValues );
				}
				catch ( Exception e ) {
					options = null;
					started.set( false );
				}
			}
		}
		else {
			SecondLevelCacheLogger.INSTANCE.attemptToStartAlreadyStartedCacheProvider();
		}
	}

	protected abstract void prepareForUse(SessionFactoryOptions settings, Map configValues);

	@Override
	public final void stop() {
		if ( started.compareAndSet( true, false ) ) {
			synchronized ( this ) {
				try {
					releaseFromUse();
				}
				finally {
					options = null;
				}
			}
		}
		else {
			SecondLevelCacheLogger.INSTANCE.attemptToStopAlreadyStoppedCacheProvider();
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
