/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.queryplan;

import java.util.Map;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
