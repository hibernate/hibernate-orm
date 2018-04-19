/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stats;

import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class StatisticsWithNoCachingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.CACHE_REGION_FACTORY, NoCachingRegionFactory.class.getName()  );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12508")
	public void testUncachedRegion() {
		final Statistics statistics = sessionFactory().getStatistics();
		statistics.getSecondLevelCacheStatistics( "hibernate.test.unknown" );
	}
}
