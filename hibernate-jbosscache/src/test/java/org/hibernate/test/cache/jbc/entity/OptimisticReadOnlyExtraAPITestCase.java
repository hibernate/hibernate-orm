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
package org.hibernate.test.cache.jbc.entity;

import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.jbc.entity.TransactionalAccess;

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

    private static EntityRegionAccessStrategy localAccessStrategy;
    
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
    protected EntityRegionAccessStrategy getEntityAccessStrategy() {
        return localAccessStrategy;
    }
    
    @Override
    protected void setEntityRegionAccessStrategy(EntityRegionAccessStrategy strategy) {
        localAccessStrategy = strategy;
    }
    
    /**
     * Test method for {@link TransactionalAccess#lockItem(java.lang.Object, java.lang.Object)}.
     */
    @Override
    public void testLockItem() {
        try {
            getEntityAccessStrategy().lockItem(KEY, new Integer(1));
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
            getEntityAccessStrategy().lockRegion();
            fail("Call to lockRegion did not throw exception");
        }
        catch (UnsupportedOperationException expected) {}
    }

    /**
     * Test method for {@link TransactionalAccess#afterUpdate(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object, org.hibernate.cache.access.SoftLock)}.
     */
    @Override
    public void testAfterUpdate() {
        try {
            getEntityAccessStrategy().afterUpdate(KEY, VALUE2, new Integer(1), new Integer(2), new MockSoftLock());
            fail("Call to afterUpdate did not throw exception");
        }
        catch (UnsupportedOperationException expected) {}
    }

}
