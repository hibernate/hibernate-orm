/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.cache.jcache.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.JCacheNaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Alex Snaps
 */
public class ReadOnlyNaturalIdRegionAccessStrategy
		extends JCacheRegionAccessStrategy<JCacheNaturalIdRegion>
		implements NaturalIdRegionAccessStrategy {

	public ReadOnlyNaturalIdRegionAccessStrategy(JCacheNaturalIdRegion jCacheNaturalIdRegion) {
		super( jCacheNaturalIdRegion );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		getRegion().put( key, value );
		return true;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		throw new UnsupportedOperationException( "This is a ReadOnly strategy!" );
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException( "This is a ReadOnly strategy!" );
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return DefaultCacheKeysFactory.staticCreateNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetNaturalIdValues( cacheKey );
	}
}
