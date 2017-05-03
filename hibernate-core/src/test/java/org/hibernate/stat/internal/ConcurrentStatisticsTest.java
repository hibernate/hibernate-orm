/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

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

	private static final String TRIVIAL_REGION_NAME = "noname";

	private ConcurrentStatisticsImpl statistics;
	private SessionFactory sessionFactory;

	@Before
	public void setUp() {
		sessionFactory = sessionFactory();
		statistics = new ConcurrentStatisticsImpl( (SessionFactoryImplementor) sessionFactory );
	}

	@After
	public void tearDown() {
		sessionFactory.close();
	}

	@Test
	public void testThatGetSecondLevelCacheStatisticsWhenSecondLevelCacheIsNotEnabledReturnsNull() {
		final ConcurrentSecondLevelCacheStatisticsImpl secondLevelCacheStatistics = statistics
				.getSecondLevelCacheStatistics( TRIVIAL_REGION_NAME );
		assertThat( secondLevelCacheStatistics, is( nullValue() ) );
	}
}
