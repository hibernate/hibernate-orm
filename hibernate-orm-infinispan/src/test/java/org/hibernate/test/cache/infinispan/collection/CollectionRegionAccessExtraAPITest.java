/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.test.cache.infinispan.AbstractExtraAPITest;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CollectionRegionAccessExtraAPITest extends AbstractExtraAPITest<CollectionRegionAccessStrategy> {
	@Override
	protected CollectionRegionAccessStrategy getAccessStrategy() {
		return environment.getCollectionRegion( REGION_NAME, CACHE_DATA_DESCRIPTION).buildAccessStrategy( accessType );
	}
}
