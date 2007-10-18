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

package org.hibernate.test.cache.jbc2.collection;

import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.jbc2.entity.TransactionalAccess;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy; in this
 * version using Optimistic locking with READ_ONLY access.
 * <p>
 * By "extra API" we mean those methods that are superfluous to the 
 * function of the JBC integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class OptimisticReadOnlyExtraAPITestCase extends OptimisticTransactionalExtraAPITestCase {

    private static CollectionRegionAccessStrategy localAccessStrategy;
    
    /**
     * Create a new TransactionalAccessTestCase.
     * 
     * @param name
     */
    public OptimisticReadOnlyExtraAPITestCase(String name) {
        super(name);
    }

    @Override
    protected AccessType getAccessType() {
        return AccessType.READ_ONLY;
    }
    
    @Override
    protected CollectionRegionAccessStrategy getCollectionAccessStrategy() {
        return localAccessStrategy;
    }
    
    @Override
    protected void setCollectionAccessStrategy(CollectionRegionAccessStrategy strategy) {
        localAccessStrategy = strategy;
    }
    
    /**
     * Test method for {@link TransactionalAccess#lockItem(java.lang.Object, java.lang.Object)}.
     */
    @Override
    public void testLockItem() {
        try {
            getCollectionAccessStrategy().lockItem(KEY, new Integer(1));
            fail("Call to lockItem did not throw exception");
        }
        catch (UnsupportedOperationException expected) {}
    }

    /**
     * Test method for {@link TransactionalAccess#lockRegion()}.
     */
    @Override
    public void testLockRegion() {
        try {
            getCollectionAccessStrategy().lockRegion();
            fail("Call to lockRegion did not throw exception");
        }
        catch (UnsupportedOperationException expected) {}
    }

}
