/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.cache.jbc2.entity;

import org.hibernate.cache.jbc2.access.OptimisticTransactionalAccessDelegate;

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
     * @param region
     */
    public OptimisticTransactionalAccess(EntityRegionImpl region) {
        super(region, new OptimisticTransactionalAccessDelegate(region.getCacheInstance(), region.getRegionFqn(),
                region.getCacheDataDescription()));
    }
}
