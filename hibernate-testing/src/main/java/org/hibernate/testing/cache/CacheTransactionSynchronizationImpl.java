/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.spi.AbstractCacheTransactionSynchronization;
import org.hibernate.cache.spi.RegionFactory;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
class CacheTransactionSynchronizationImpl extends AbstractCacheTransactionSynchronization {
	private static final Logger log = Logger.getLogger( CacheTransactionSynchronizationImpl.class );

	CacheTransactionSynchronizationImpl(RegionFactory regionFactory) {
		super( regionFactory );
	}
}
