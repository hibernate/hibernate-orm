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

import org.hibernate.cache.jbc.access.OptimisticTransactionalAccessDelegate;

/**
 * Defines the strategy for transactional access to entity data in an
 * optimistic-locking JBoss Cache using its 2.x APIs
 * 
 * @author Brian Stansberry
 * @version $Revision: 1 $
 */
public class OptimisticTransactionalAccess extends TransactionalAccess {

    /**
     * Create a new OptimisticTransactionalAccess.
     * 
     * @param region The region\ to which this is providing access
     */
    public OptimisticTransactionalAccess(EntityRegionImpl region) {
        super(region, new OptimisticTransactionalAccessDelegate(region, region.getPutFromLoadValidator()));
    }
}
