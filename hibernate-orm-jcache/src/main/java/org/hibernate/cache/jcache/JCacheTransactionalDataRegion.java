/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.util.EnumSet;
import java.util.Set;
import javax.cache.Cache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.TransactionalDataRegion;
import org.hibernate.cache.spi.access.AccessType;

/**
 * @author Alex Snaps
 */
public class JCacheTransactionalDataRegion extends JCacheRegion implements TransactionalDataRegion {

	private static final Set<AccessType> SUPPORTED_ACCESS_TYPES
			= EnumSet.of( AccessType.READ_ONLY, AccessType.NONSTRICT_READ_WRITE, AccessType.READ_WRITE );

	private final CacheDataDescription metadata;
	private final SessionFactoryOptions options;

	public JCacheTransactionalDataRegion(Cache<Object, Object> cache, CacheDataDescription metadata, SessionFactoryOptions options) {
		super( cache );
		this.metadata = metadata;
		this.options = options;
	}

	@Override
	public boolean isTransactionAware() {
		return false;
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

	protected void throwIfAccessTypeUnsupported(AccessType accessType) {
		if ( !supportedAccessTypes().contains( accessType ) ) {
			throw new UnsupportedOperationException( "JCacheTransactionalDataRegion doesn't support " + accessType );
		}
	}

	protected Set<AccessType> supportedAccessTypes() {
		return SUPPORTED_ACCESS_TYPES;
	}

	public void clear() {
		cache.removeAll();
	}

	public Object get(Object key) {
		return cache.get( key );
	}

	public void remove(Object key) {
		cache.remove( key );
	}

	public void put(Object key, Object value) {
		cache.put( key, value );
	}

	public boolean putIfAbsent(Object key, Object value) {
		return cache.putIfAbsent( key, value );
	}

	public boolean replace(Object key, Object expected, Object value) {
		return cache.replace( key, expected, value );
	}

	public SessionFactoryOptions getSessionFactoryOptions() {
		return options;
	}
}
