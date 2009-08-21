/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.access.AccessType;
import org.infinispan.transaction.tm.BatchModeTransactionManager;

/**
 * Base class for tests of TRANSACTIONAL access.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractReadOnlyAccessTestCase extends AbstractEntityRegionAccessStrategyTestCase {

    /**
     * Create a new AbstractTransactionalAccessTestCase.
     * 
     */
    public AbstractReadOnlyAccessTestCase(String name) {
        super(name);
    }

    @Override
    protected AccessType getAccessType() {
        return AccessType.READ_ONLY;
    }   

    @Override
    public void testPutFromLoad() throws Exception {
        putFromLoadTest(false);
    }

    @Override
    public void testPutFromLoadMinimal() throws Exception {
        putFromLoadTest(true);
    }
    
    private void putFromLoadTest(boolean minimal) throws Exception {
       
        final String KEY = KEY_BASE + testCount++;
        
        long txTimestamp = System.currentTimeMillis();
        BatchModeTransactionManager.getInstance().begin();
        assertNull(localAccessStrategy.get(KEY, System.currentTimeMillis()));
        if (minimal)
            localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);
        else
            localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
        
        sleep(250);
        Object expected = isUsingInvalidation() ? null : VALUE1;
        assertEquals(expected, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
        
        BatchModeTransactionManager.getInstance().commit();
        assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertEquals(expected, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
    }

    @Override
    public void testUpdate() throws Exception {
       
        final String KEY = KEY_BASE + testCount++;
        
        try {
            localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));
            fail("Call to update did not throw exception");
        }
        catch (UnsupportedOperationException good) {}
    }

}
