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

import org.junit.Test;

import org.hibernate.engine.query.spi.NativeSQLQueryPlan;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Tests equals() for NativeSQLQueryReturn implementations.
 *
 * @author Michael Stevens
 */
public class NativeSQLQueryPlanEqualsTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] {};
	}

	@Test
	public void testNativeSQLQuerySpecEquals() {
		QueryPlanCache cache = new QueryPlanCache( sessionFactory() );
		NativeSQLQuerySpecification firstSpec = createSpec();

		NativeSQLQuerySpecification secondSpec = createSpec();
		
		NativeSQLQueryPlan firstPlan = cache.getNativeSQLQueryPlan(firstSpec);
		NativeSQLQueryPlan secondPlan = cache.getNativeSQLQueryPlan(secondSpec);
		
		assertEquals(firstPlan, secondPlan);
		
	}

	private NativeSQLQuerySpecification createSpec() {
		String blah = "blah";
		String select = "select blah from blah";
		NativeSQLQueryReturn[] queryReturns = new NativeSQLQueryScalarReturn[] {
				new NativeSQLQueryScalarReturn( blah, sessionFactory().getTypeResolver().basic( "int" ) )
		};
		return new NativeSQLQuerySpecification( select, queryReturns, null );
	}
}
