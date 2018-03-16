/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.support.AbstractNaturalIdDataAccess;

/**
 * @author Steve Ebersole
 */
public class NaturalIdTransactionalAccess extends AbstractNaturalIdDataAccess {
	public NaturalIdTransactionalAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccessImpl domainDataStorageAccess,
			NaturalIdDataCachingConfig config) {
		super( region, keysFactory, domainDataStorageAccess, config );
	}
}
