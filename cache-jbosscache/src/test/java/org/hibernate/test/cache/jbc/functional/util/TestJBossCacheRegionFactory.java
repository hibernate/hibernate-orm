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
package org.hibernate.test.cache.jbc.functional.util;

import java.util.Properties;

import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.jboss.cache.CacheManager;

/**
 * {@link JBossCacheRegionFactory} that uses
 * {@link TestCacheInstanceManager} as its
 * {@link #getCacheInstanceManager() CacheInstanceManager}.
 * <p>
 * This version lets a test fixture to access {@link CacheManager},
 * making it easy for the test fixture to get access to the caches being 
 * used.  Intended for FunctionalUnitTestCase subclasses where the creation
 * of the region factory is hidden inside the initialization of a SessionFactory.
 * </p>
 * 
 * @author Brian Stansberry
 */
public class TestJBossCacheRegionFactory extends JBossCacheRegionFactory {

    /**
     * FIXME Per the RegionFactory class Javadoc, this constructor version
     * should not be necessary.
     * 
     * @param props The configuration properties
     */
    public TestJBossCacheRegionFactory(Properties props) {
        this();
    }

    /**
     * Create a new TestJBossCacheRegionFactory.
     * 
     */
    public TestJBossCacheRegionFactory() {
        super(new TestCacheInstanceManager());
    }

}
