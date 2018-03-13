/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;

/**
 * @author Steve Ebersole
 */
public class NaturalIdTransactionalAccess extends BaseNaturalIdDataAccess {
	public NaturalIdTransactionalAccess(
			DomainDataRegionImpl region,
			NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		super( region );
	}
}
