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

import junit.framework.Test;

/**
 * Tests TRANSACTIONAL access when optimistic locking and replication are used.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class OptimisticReplicatedTransactionalTestCase extends AbstractTransactionalAccessTestCase {

    /**
     * Create a new PessimisticTransactionalAccessTestCase.
     * 
     * @param name
     */
    public OptimisticReplicatedTransactionalTestCase(String name) {
        super(name);
    }
    
    public static Test suite() throws Exception {
        return getTestSetup(OptimisticReplicatedTransactionalTestCase.class, "optimistic-shared");
    }

    @Override
    public void testCacheConfiguration() {
        assertFalse("Using Invalidation", isUsingInvalidation());
        assertTrue("Using Optimistic locking", isUsingOptimisticLocking());
        assertTrue("Synchronous mode", isSynchronous());
    }
    
    

}
