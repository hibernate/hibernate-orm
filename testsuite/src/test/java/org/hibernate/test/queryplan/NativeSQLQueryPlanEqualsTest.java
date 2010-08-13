/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.queryplan;

import org.hibernate.engine.query.QueryPlanCache;
import org.hibernate.engine.query.NativeSQLQueryPlan;
import org.hibernate.engine.query.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.query.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * Tests equals() for NativeSQLQueryReturn implementations.
 *
 * @author Michael Stevens
 */
public class NativeSQLQueryPlanEqualsTest extends FunctionalTestCase {
	public NativeSQLQueryPlanEqualsTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] {};
	}

	public void testNativeSQLQuerySpecEquals() {
		QueryPlanCache cache = new QueryPlanCache( sfi() );
		NativeSQLQuerySpecification firstSpec = createSpec();

		NativeSQLQuerySpecification secondSpec = createSpec();
		
		NativeSQLQueryPlan firstPlan = cache.getNativeSQLQueryPlan(firstSpec);
		NativeSQLQueryPlan secondPlan = cache.getNativeSQLQueryPlan(secondSpec);
		
		assertEquals(firstPlan, secondPlan);
		
	}

	private NativeSQLQuerySpecification createSpec() {
		String blah = "blah";
		String select = "select blah from blah";
		NativeSQLQueryReturn[] queryReturns = 
			new NativeSQLQueryScalarReturn[] {new NativeSQLQueryScalarReturn(blah, sfi().getTypeResolver().basic( "int" ) ) };
		NativeSQLQuerySpecification spec = new NativeSQLQuerySpecification(select,
			queryReturns, null);
		return spec;
	}
}
