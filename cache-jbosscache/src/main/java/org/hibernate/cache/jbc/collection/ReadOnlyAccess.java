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
package org.hibernate.cache.jbc.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.CacheException;

/**
 * This defines the strategy for transactional access to collection data in a
 * pessimistic-locking JBossCache using its 2.x APIs. <p/> The read-only access
 * to a JBossCache really is still transactional, just with the extra semantic
 * or guarantee that we will not update data.
 * 
 * @author Steve Ebersole
 */
public class ReadOnlyAccess extends TransactionalAccess {
    private static final Logger log = LoggerFactory.getLogger(ReadOnlyAccess.class);

	/**
	 * Create a provider of read-only access to the specific region.
	 *
	 * @param region The region to which this provides access.
	 */
	public ReadOnlyAccess(CollectionRegionImpl region) {
        super(region);
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only item");
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
    public SoftLock lockRegion() throws CacheException {
        throw new UnsupportedOperationException("Illegal attempt to edit read only region");
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only item");
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
    public void unlockRegion(SoftLock lock) throws CacheException {
        log.error("Illegal attempt to edit read only region");
    }
}
