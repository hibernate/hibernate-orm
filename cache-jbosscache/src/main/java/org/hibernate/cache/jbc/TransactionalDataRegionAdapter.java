/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache.jbc;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.TransactionalDataRegion;
import org.jboss.cache.Cache;

/**
 * {@inheritDoc}
 * 
 * @author Steve Ebersole
 */
public abstract class TransactionalDataRegionAdapter extends BasicRegionAdapter implements TransactionalDataRegion {

    protected final CacheDataDescription metadata;

    public TransactionalDataRegionAdapter(Cache jbcCache, String regionName, String regionPrefix,
            CacheDataDescription metadata) {
        super(jbcCache, regionName, regionPrefix);
        this.metadata = metadata;
    }

    /**
     * Here, for JBossCache, we consider the cache to be transaction aware if
     * the underlying cache instance has a reference to the transaction manager.
     */
    public boolean isTransactionAware() {
        return transactionManager != null;
    }

    /**
     * {@inheritDoc}
     */
    public CacheDataDescription getCacheDataDescription() {
        return metadata;
    }

}
