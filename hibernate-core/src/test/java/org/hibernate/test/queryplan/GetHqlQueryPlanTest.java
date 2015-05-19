/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryplan;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Tests for HQL query plans
 *
 * @author Gail Badner
 */
public class GetHqlQueryPlanTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[]{
			"queryplan/filter-defs.hbm.xml",
			"queryplan/Joined.hbm.xml"
		};
	}

	protected Map getEnabledFilters(Session s) {
		return ( ( SessionImplementor ) s ).getLoadQueryInfluencers().getEnabledFilters();
	}

	@Test
	public void testHqlQueryPlan() {
		Session s = openSession();
		QueryPlanCache cache = ( ( SessionImplementor ) s ).getFactory().getQueryPlanCache();
		assertTrue( getEnabledFilters( s ).isEmpty() );

		HQLQueryPlan plan1 = cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) );
		HQLQueryPlan plan2 = cache.getHQLQueryPlan( "from Person where name is null", false, getEnabledFilters( s ) );
		HQLQueryPlan plan3 = cache.getHQLQueryPlan( "from Person where name = :name", false, getEnabledFilters( s ) );
		HQLQueryPlan plan4 = cache.getHQLQueryPlan( "from Person where name = ?", false, getEnabledFilters( s ) );

		assertNotSame( plan1, plan2 );
		assertNotSame( plan1, plan3 );
		assertNotSame( plan1, plan4 );
		assertNotSame( plan2, plan3 );
		assertNotSame( plan2, plan4 );
		assertNotSame( plan3, plan4 );

		assertSame( plan1, cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) ) );
		assertSame( plan2, cache.getHQLQueryPlan( "from Person where name is null", false, getEnabledFilters( s ) ) );
		assertSame( plan3, cache.getHQLQueryPlan( "from Person where name = :name", false, getEnabledFilters( s ) ) );
		assertSame( plan4, cache.getHQLQueryPlan( "from Person where name = ?", false, getEnabledFilters( s ) ) );

		s.close();
	}

	@Test
	public void testHqlQueryPlanWithEnabledFilter() {
		Session s = openSession();
		QueryPlanCache cache = ( (SessionImplementor) s ).getFactory().getQueryPlanCache();

		HQLQueryPlan plan1A = cache.getHQLQueryPlan( "from Person", true, getEnabledFilters( s ) );
		HQLQueryPlan plan1B = cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) );

		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'F' ) );
		HQLQueryPlan plan2A = cache.getHQLQueryPlan( "from Person", true, getEnabledFilters( s ) );
		HQLQueryPlan plan2B = cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) );

		s.disableFilter( "sex" );
		HQLQueryPlan plan3A = cache.getHQLQueryPlan( "from Person", true, getEnabledFilters( s ) );
		HQLQueryPlan plan3B = cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) );

		s.enableFilter( "sex" ).setParameter( "sexCode", Character.valueOf( 'M' ) );
		HQLQueryPlan plan4A = cache.getHQLQueryPlan( "from Person", true, getEnabledFilters( s ) );
		HQLQueryPlan plan4B = cache.getHQLQueryPlan( "from Person", false, getEnabledFilters( s ) );

		assertSame( plan1A, plan3A );
		assertSame( plan1B, plan3B );
		assertSame( plan2A, plan4A );
		assertSame( plan2B, plan4B );

		assertNotSame( plan1A, plan1B );
		assertNotSame( plan1A, plan2A );
		assertNotSame( plan1A, plan2B );
		assertNotSame( plan1B, plan2A );
		assertNotSame( plan1B, plan2B );

		s.close();
	}
}
