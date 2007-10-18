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

package org.hibernate.test.cache.jbc2.builder;

import org.hibernate.cache.jbc2.MultiplexedJBossCacheRegionFactory;


/**
 * A SharedCacheInstanceManagerTestCase.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class MultiplexedCacheInstanceManagerTestCase extends CacheInstanceManagerTestBase {

    /**
     * Create a new SharedCacheInstanceManagerTestCase.
     * 
     * @param name
     */
    public MultiplexedCacheInstanceManagerTestCase(String name) {
        super(name);
    }

    @Override
    protected Class getRegionFactoryClass() {
        return MultiplexedJBossCacheRegionFactory.class;
    }

}
