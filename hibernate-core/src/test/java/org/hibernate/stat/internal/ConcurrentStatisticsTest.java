/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.stat.SecondLevelCacheStatistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class ConcurrentStatisticsTest extends BaseCoreFunctionalTestCase {
	private static final String REGION_PREFIX = "my-app";
	private static final String TRIVIAL_REGION_NAME = "noname";

	private StatisticsImpl statistics;
	private SessionFactory sessionFactory;

	@Before
	public void setUp() {
		sessionFactory = sessionFactory();
		statistics = new StatisticsImpl( (SessionFactoryImplementor) sessionFactory );
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.CACHE_REGION_PREFIX, REGION_PREFIX );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@After
	public void tearDown() {
		sessionFactory.close();
	}

	@Test
	public void testThatGetSecondLevelCacheStatisticsWhenSecondLevelCacheIsNotEnabledReturnsNull() {
		final SecondLevelCacheStatistics secondLevelCacheStatistics = statistics
				.getSecondLevelCacheStatistics( StringHelper.qualify( REGION_PREFIX, TRIVIAL_REGION_NAME ) );
		assertThat( secondLevelCacheStatistics, is( nullValue() ) );
	}
}
