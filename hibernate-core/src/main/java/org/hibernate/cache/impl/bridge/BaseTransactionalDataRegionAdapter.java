/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cfg.Settings;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public abstract class BaseTransactionalDataRegionAdapter
		extends BaseRegionAdapter
		implements TransactionalDataRegion {

	protected final CacheDataDescription metadata;

	protected BaseTransactionalDataRegionAdapter(Cache underlyingCache, Settings settings, CacheDataDescription metadata) {
		super( underlyingCache, settings );
		this.metadata = metadata;
	}

	public boolean isTransactionAware() {
		return underlyingCache instanceof org.hibernate.cache.TransactionAwareCache;
	}

	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}
}
