/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cid;

import java.util.Calendar;
import java.util.Collections;
import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.exception.SQLGrammarException;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class CompositeIdCountEntityTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "cid/Customer.hbm.xml", "cid/Order.hbm.xml", "cid/LineItem.hbm.xml", "cid/Product.hbm.xml" };
	}

	@Test
	public void testNonDistinctCountOfEntityWithCompositeId() {
		// the check here is all based on whether we had commas in the expressions inside the count
		final HQLQueryPlan plan = sessionFactory().getQueryInterpretationCache().getHQLQueryPlan(
				"select count(o) from Order o",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();

		final int countExpressionListStart = generatedSql.indexOf( "count(" );
		final int countExpressionListEnd = generatedSql.indexOf( ")", countExpressionListStart );
		final String countExpressionFragment = generatedSql.substring( countExpressionListStart+6, countExpressionListEnd+1 );
		final boolean hadCommas = countExpressionFragment.contains( "," );

		// set up the expectation based on Dialect...
		final boolean expectCommas = sessionFactory().getDialect().supportsTupleCounts();

		assertEquals( expectCommas, hadCommas );
	}

	@Test
	@SkipForDialect(value = Oracle8iDialect.class, comment = "Cannot count distinct over multiple columns in Oracle")
	@SkipForDialect(value = SQLServerDialect.class, comment = "Cannot count distinct over multiple columns in SQL Server")
	public void testDistinctCountOfEntityWithCompositeId() {
		// today we do not account for Dialects supportsTupleDistinctCounts() is false.  though really the only
		// "option" there is to throw an error.
		final HQLQueryPlan plan = sessionFactory().getQueryInterpretationCache().getHQLQueryPlan(
				"select count(distinct o) from Order o",
				false,
				Collections.EMPTY_MAP
		);
		assertEquals( 1, plan.getTranslators().length );
		final QueryTranslator translator = plan.getTranslators()[0];
		final String generatedSql = translator.getSQLString();
		System.out.println( "Generated SQL : " + generatedSql );

		final int countExpressionListStart = generatedSql.indexOf( "count(" );
		final int countExpressionListEnd = generatedSql.indexOf( ")", countExpressionListStart );
		final String countExpressionFragment = generatedSql.substring( countExpressionListStart+6, countExpressionListEnd+1 );
		assertTrue( countExpressionFragment.startsWith( "distinct" ) );
		assertTrue( countExpressionFragment.contains( "," ) );

		Session s = openSession();
		s.beginTransaction();
		Customer c = new Customer();
		c.setCustomerId( "1" );
		c.setAddress("123 somewhere");
		c.setName("Brett");
		Order o1 = new Order( c );
		o1.setOrderDate( Calendar.getInstance() );
		Order o2 = new Order( c );
		o2.setOrderDate( Calendar.getInstance() );
		s.persist( c );
		s.persist( o1 );
		s.persist( o2 );
		s.getTransaction().commit();
		s.clear();

		s.beginTransaction();
		try {
			long count = ( Long ) s.createQuery( "select count(distinct o) FROM Order o" ).uniqueResult();
			if ( ! getDialect().supportsTupleDistinctCounts() ) {
				fail( "expected PersistenceException caused by SQLGrammarException" );
			}
			assertEquals( 2l, count );
		}
		catch ( PersistenceException e ) {
			if ( ! (e.getCause() instanceof SQLGrammarException ) || getDialect().supportsTupleDistinctCounts() ) {
				throw e;
			}
		}
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery("delete from Order").executeUpdate();
		s.createQuery("delete from Customer").executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

}
