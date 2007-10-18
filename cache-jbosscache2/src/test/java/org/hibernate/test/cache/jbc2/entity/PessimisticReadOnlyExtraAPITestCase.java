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

package org.hibernate.test.cache.jbc2.entity;

import org.hibernate.cache.access.EntityRegionAccessStrategy;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy; in this 
 * version using pessimistic locking with READ_ONLY access.
 * <p>
 * By "extra API" we mean those methods that are superfluous to the 
 * function of the JBC integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class PessimisticReadOnlyExtraAPITestCase extends OptimisticReadOnlyExtraAPITestCase {

    private static EntityRegionAccessStrategy localAccessStrategy;
    
    /**
     * Create a new PessimisticAccessStrategyExtraAPITestCase.
     * 
     * @param name
     */
    public PessimisticReadOnlyExtraAPITestCase(String name) {
        super(name);
    }
    
    @Override
    protected String getCacheConfigName() {
        return "pessimistic-entity";
    }
    
    @Override
    protected EntityRegionAccessStrategy getEntityAccessStrategy() {
        return localAccessStrategy;
    }
    
    @Override
    protected void setEntityRegionAccessStrategy(EntityRegionAccessStrategy strategy) {
        localAccessStrategy = strategy;
    }
    
    @Override
    public void testCacheConfiguration() {
        assertFalse("Using Optimistic locking", isUsingOptimisticLocking());
    }

}
