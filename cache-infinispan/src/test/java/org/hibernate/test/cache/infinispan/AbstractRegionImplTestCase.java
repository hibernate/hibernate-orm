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
package org.hibernate.test.cache.infinispan;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.Region;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.util.ComparableComparator;

/**
 * Base class for tests of Region implementations.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractRegionImplTestCase extends AbstractNonFunctionalTestCase {

   public AbstractRegionImplTestCase(String name) {
      super(name);
   }

   protected abstract CacheAdapter getInfinispanCache(InfinispanRegionFactory regionFactory);

   protected abstract Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd);

   protected abstract void putInRegion(Region region, Object key, Object value);

   protected abstract void removeFromRegion(Region region, Object key);

   protected CacheDataDescription getCacheDataDescription() {
      return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
   }

}
