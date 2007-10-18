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

import org.hibernate.cache.access.AccessType;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Base class for tests of TRANSACTIONAL access.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
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
        try {
            localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));
            fail("Call to update did not throw exception");
        }
        catch (UnsupportedOperationException good) {}
    }

}
