/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.test.pagination;

import java.math.BigDecimal;
import java.util.List;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.SQLQuery;
import org.hibernate.Query;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class PaginationTest extends FunctionalTestCase {
	public static final int ROWS = 100;

	public PaginationTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "pagination/DataPoint.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( PaginationTest.class );
	}

	public void testLimit() {
		if ( ! getDialect().supportsLimit() ) {
			reportSkip( "Dialect does not support limit" );
			return;
		}

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

	public void testLimitOffset() {
		if ( ! getDialect().supportsLimitOffset() ) {
			reportSkip( "Dialect does not support limit+offset" );
			return;
		}

		prepareTestData();

		Session session = openSession();
		session.beginTransaction();

		List result;

		result = generateBaseHQLQuery( session )
				.setFirstResult( 0 )
				.setMaxResults( 20 )
				.list();
		assertEquals( 20, result.size() );
		assertEquals( 0, ( ( DataPoint ) result.get( 0 ) ).getSequence() );
		assertEquals( 1, ( ( DataPoint ) result.get( 1 ) ).getSequence() );

		result = generateBaseCriteria( session )
				.setFirstResult( 1 )
				.setMaxResults( 20 )
				.list();
		assertEquals( 20, result.size() );
		assertEquals( 1, ( ( DataPoint ) result.get( 0 ) ).getSequence() );
		assertEquals( 2, ( ( DataPoint ) result.get( 1 ) ).getSequence() );

		result = generateBaseCriteria( session )
				.setFirstResult( 99 )
				.setMaxResults( Integer.MAX_VALUE - 200 )
				.list();
		assertEquals( 1, result.size() );
		assertEquals( 99, ( ( DataPoint ) result.get( 0 ) ).getSequence() );

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
		for ( int i = 0; i < ROWS; i++ ) {
			DataPoint dataPoint = new DataPoint();
			dataPoint.setSequence( i );
			dataPoint.setDescription( "data point #" + i );
			BigDecimal x = new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN );
			dataPoint.setX( x );
			dataPoint.setY( new BigDecimal( Math.cos( x.doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			session.save( dataPoint );
		}
		session.getTransaction().commit();
		session.close();
	}

	private void cleanupTestData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete DataPoint" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	private void reportSkip(String message) {
		reportSkip( message, "pagination support" );
	}
}

