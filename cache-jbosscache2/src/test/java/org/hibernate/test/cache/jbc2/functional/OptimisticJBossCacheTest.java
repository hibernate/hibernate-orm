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
package org.hibernate.test.cache.jbc2.functional;

import junit.framework.Test;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * FIXME Move to hibernate-testsuite project and rename class x- "Disabled"
 * 
 * @author Brian Stansberry
 */
public class OptimisticJBossCacheTest extends AbstractQueryCacheFunctionalTestCase {

    // note that a lot of the fucntionality here is intended to be used
    // in creating specific tests for each CacheProvider that would extend
    // from a base test case (this) for common requirement testing...

    public OptimisticJBossCacheTest(String x) {
        super(x);
    }

    public static Test suite() {
        return new FunctionalTestClassTestSuite(OptimisticJBossCacheTest.class);
    }

    protected Class<? extends RegionFactory> getCacheRegionFactory() {
        return JBossCacheRegionFactory.class;
    }

    protected String getConfigResourceKey() {
        return SharedCacheInstanceManager.CACHE_RESOURCE_PROP;
    }

    protected String getConfigResourceLocation() {
        return "org/hibernate/test/cache/jbc2/functional/optimistic-treecache.xml";
    }

}
