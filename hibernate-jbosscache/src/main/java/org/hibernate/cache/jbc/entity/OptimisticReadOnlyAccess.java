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
package org.hibernate.cache.jbc.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.CacheException;

/**
 * This defines the strategy for read-only access to enity data in an
 * optimistic-locking JBossCache using its 2.x APIs <p/> The read-only access to
 * a JBossCache really is still transactional, just with the extra semantic or
 * guarantee that we will not update data.
 * 
 * @author Brian Stansberry
 */
public class OptimisticReadOnlyAccess extends OptimisticTransactionalAccess {
    private static final Logger log = LoggerFactory.getLogger(OptimisticReadOnlyAccess.class);

    public OptimisticReadOnlyAccess(EntityRegionImpl region) {
        super(region);
    }

    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

    @Override
    public SoftLock lockRegion() throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only region");
    }

    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only item");
    }

    @Override
    public void unlockRegion(SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only region");
    }

    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

    @Override
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }
}
