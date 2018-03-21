/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.spi.support.DomainDataStorageAccess;

/**
 * DomainDataStorageAccess implementation wrapping a JCache {@link Cache} reference.
 *
 * @author Steve Ebersole
 */
public class DomainDataJCacheAccessImpl extends JCacheAccessImpl implements DomainDataStorageAccess {
	public DomainDataJCacheAccessImpl(Cache underlyingCache) {
		super( underlyingCache );
	}

	@Override
	public void putFromLoad(Object key, Object value) {
		putIntoCache( key, value );
	}
}
