/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.spi.AbstractCacheTransactionSynchronization;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public class NoCachingTransactionSynchronizationImpl extends AbstractCacheTransactionSynchronization {
	public NoCachingTransactionSynchronizationImpl(RegionFactory regionFactory) {
		super( regionFactory );
	}
}
