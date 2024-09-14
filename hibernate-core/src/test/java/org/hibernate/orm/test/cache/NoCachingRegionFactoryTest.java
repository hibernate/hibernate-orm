/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cache;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Gail Badner.
 */
public class NoCachingRegionFactoryTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, NoCachingRegionFactory.class  );
	}

	@Test
	@JiraKey( value = "HHH-12508" )
	public void testSessionFactoryOptionsConsistent() {
		assertFalse( sessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled() );
	}
}
