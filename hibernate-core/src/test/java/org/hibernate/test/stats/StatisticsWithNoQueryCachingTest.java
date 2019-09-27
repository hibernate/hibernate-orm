/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stats;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class StatisticsWithNoQueryCachingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true"  );
		configuration.setProperty( AvailableSettings.USE_QUERY_CACHE, "false"  );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13645")
	public void testUncachedRegion() {
		final Statistics statistics = sessionFactory().getStatistics();
		assertNull( statistics.getCacheRegionStatistics( "hibernate.test.unknown" ) );
	}
}
