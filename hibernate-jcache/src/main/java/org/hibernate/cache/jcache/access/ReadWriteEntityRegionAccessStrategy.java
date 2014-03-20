/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache.access;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.JCacheEntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Alex Snaps
 */
public class ReadWriteEntityRegionAccessStrategy
		extends AbstractReadWriteRegionAccessStrategy<JCacheEntityRegion>
		implements EntityRegionAccessStrategy {


	public ReadWriteEntityRegionAccessStrategy(JCacheEntityRegion region) {
		super( region );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return region.invoke(
				key, new EntryProcessor<Object, Object, Boolean>() {
					@Override
					public Boolean process(MutableEntry<Object, Object> entry, Object... args)
							throws EntryProcessorException {
						if ( !entry.exists() ) {
							entry.setValue( new Item( args[0], args[1], (Long) args[2] ) );
							return true;
						}
						else {
							return false;
						}
					}
				}, value, version, region.nextTimestamp()
		);
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return region.invoke(
				key, new EntryProcessor<Object, Object, Boolean>() {
					@Override
					public Boolean process(MutableEntry<Object, Object> entry, Object... args)
							throws EntryProcessorException {
						final Lockable item = (Lockable) entry.getValue();

						if ( item != null && item.isUnlockable( (SoftLock) args[3] ) ) {
							final Lock lockItem = (Lock) item;
							if ( lockItem.wasLockedConcurrently() ) {
								lockItem.unlock( (Long) args[1] );
								entry.setValue( lockItem );
								return false;
							}
							else {
								entry.setValue( new Item( args[0], args[1], (Long) args[4] ) );
								return true;
							}
						}
						else {
							entry.setValue( null );
							return false;
						}

					}
				}, value, currentVersion, previousVersion, lock, region.nextTimestamp()
		);
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.createEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.getEntityId( cacheKey );
	}
}
