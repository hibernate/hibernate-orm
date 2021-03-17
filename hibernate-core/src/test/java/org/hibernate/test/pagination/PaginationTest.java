/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.pagination;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gavin King
 */
public class PaginationTest extends BaseNonConfigCoreFunctionalTestCase {
	public static final int NUMBER_OF_TEST_ROWS = 100;

	@Override
	public String[] getMappings() {
		return new String[] { "pagination/DataPoint.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportLimitCheck.class,
			comment = "Dialect does not support limit"
	)
	public void testLimit() {
		prepareTestData();

		Session session = openSession();
		session.beginTransaction();

		int count;

		count = generateBaseHQLQuery( session )
				.setMaxResults( 5 )
				.list()
				.size();
		assertEquals( 5, count );

		count = generateBaseCriteria( session )
				.setMaxResults( 18 )
				.list()
				.size();
		assertEquals( 18, count );

		count = generateBaseSQLQuery( session )
				.setMaxResults( 13 )
				.list()
				.size();
		assertEquals( 13, count );

		session.getTransaction().commit();
		session.close();

		cleanupTestData();
	}

	@Test
	public void testOffset() {
		prepareTestData();
		Session session = openSession();
		session.beginTransaction();
		List result;

		result = generateBaseHQLQuery( session )
				.setFirstResult( 3 )
				.list();
		DataPoint firstDataPointHQL = (DataPoint) result.get( 0 );

		result = generateBaseCriteria( session )
				.setFirstResult( 3 )
				.list();
		DataPoint firstDataPointCriteria = (DataPoint) result.get( 0 );

		assertEquals( "The first entry should be the same in HQL and Criteria", firstDataPointHQL, firstDataPointHQL );
		assertEquals( "Wrong first result", 3, firstDataPointCriteria.getSequence() );

		session.getTransaction().commit();
		session.close();
		cleanupTestData();
	}

	/**
	 * @author <a href="mailto:piotr.findeisen@gmail.com">Piotr Findeisen</a>
	 */
	@Test
	@TestForIssue( jiraKey = "HHH-951" )
	@RequiresDialectFeature(
			value = DialectChecks.SupportLimitCheck.class,
			comment = "Dialect does not support limit"
	)
	public void testLimitWithExpreesionAndFetchJoin() {
		Session session = openSession();
		session.beginTransaction();

		String hql = "SELECT b, 1 FROM DataMetaPoint b inner join fetch b.dataPoint dp";
		session.createQuery(hql)
				.setMaxResults(3)
				// This should not fail
				.list();

		HQLQueryPlan queryPlan = new HQLQueryPlan(hql, false, Collections.EMPTY_MAP, sessionFactory());
		String sqlQuery = queryPlan.getTranslators()[0]
				.collectSqlStrings().get(0);

		session.getTransaction().commit();
		session.close();

		Matcher matcher = Pattern.compile(
				"(?is)\\b(?<column>\\w+\\.\\w+)\\s+as\\s+(?<alias>\\w+)\\b.*\\k<column>\\s+as\\s+\\k<alias>")
				.matcher(sqlQuery);
		if (matcher.find()) {
			fail(format("Column %s mapped to alias %s twice in generated SQL: %s", matcher.group("column"),
					matcher.group("alias"), sqlQuery));
		}
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportLimitAndOffsetCheck.class,
			comment = "Dialect does not support limit+offset"
	)
	public void testLimitOffset() {
		prepareTestData();

		Session session = openSession();
		session.beginTransaction();

		List result;

		result = generateBaseHQLQuery( session )
				.setFirstResult( 0 )
				.setMaxResults( 20 )
				.list();
		assertEquals( 20, result.size() );
		assertEquals( 0, ( (DataPoint) result.get( 0 ) ).getSequence() );
		assertEquals( 1, ( (DataPoint) result.get( 1 ) ).getSequence() );

		result = generateBaseCriteria( session )
				.setFirstResult( 1 )
				.setMaxResults( 20 )
				.list();
		assertEquals( 20, result.size() );
		assertEquals( 1, ( (DataPoint) result.get( 0 ) ).getSequence() );
		assertEquals( 2, ( (DataPoint) result.get( 1 ) ).getSequence() );

		result = generateBaseCriteria( session )
				.setFirstResult( 99 )
				.setMaxResults( Integer.MAX_VALUE - 200 )
				.list();
		assertEquals( 1, result.size() );
		assertEquals( 99, ( (DataPoint) result.get( 0 ) ).getSequence() );

		result = session.createQuery( "select distinct description from DataPoint order by description" )
				.setFirstResult( 2 )
				.setMaxResults( 3 )
				.list();
		assertEquals( 3, result.size() );
		assertEquals( "Description: 2", result.get( 0 ) );
		assertEquals( "Description: 3", result.get( 1 ) );
		assertEquals( "Description: 4", result.get( 2 ) );

		result = session.createSQLQuery( "select description, xval, yval from DataPoint order by xval, yval" )
				.setFirstResult( 2 )
				.setMaxResults( 5 )
				.list();
		assertEquals( 5, result.size() );
		Object[] row = (Object[]) result.get( 0 );
		assertTrue( row[0] instanceof String );

		result = session.createSQLQuery( "select * from DataPoint order by xval, yval" )
				.setFirstResult( 2 )
				.setMaxResults( 5 )
				.list();
		assertEquals( 5, result.size() );


		session.getTransaction().commit();
		session.close();

		cleanupTestData();
	}

	private Query generateBaseHQLQuery(Session session) {
		return session.createQuery( "select dp from DataPoint dp order by dp.sequence" );
	}

	private Criteria generateBaseCriteria(Session session) {
		return session.createCriteria( DataPoint.class )
				.addOrder( Order.asc( "sequence" ) );
	}

	private SQLQuery generateBaseSQLQuery(Session session) {
		return session.createSQLQuery( "select id, seqval, xval, yval, description from DataPoint order by seqval" )
				.addEntity( DataPoint.class );
	}

	private void prepareTestData() {
		Session session = openSession();
		session.beginTransaction();
		for ( int i = 0; i < NUMBER_OF_TEST_ROWS; i++ ) {
			DataPoint dataPoint = new DataPoint();
			dataPoint.setSequence( i );
			dataPoint.setDescription( "data point #" + i );
			BigDecimal x = new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN );
			dataPoint.setX( x );
			dataPoint.setY( new BigDecimal( Math.cos( x.doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dataPoint.setDescription( "Description: " + i % 5 );
			session.save( dataPoint );
		}
		session.getTransaction().commit();
		session.close();
	}

	public void cleanupTestData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete DataPoint" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}

