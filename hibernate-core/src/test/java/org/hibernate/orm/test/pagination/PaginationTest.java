/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/pagination/DataPoint.hbm.xml"
		}
)
public class PaginationTest {
	public static final int NUMBER_OF_TEST_ROWS = 100;

	@Test
	@SessionFactory
	public void testLimit(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int count;

					count = generateBaseHQLQuery( session )
							.setMaxResults( 5 )
							.list()
							.size();
					assertEquals( 5, count );

					count = generateBaseSelectionQuery( session )
							.setMaxResults( 5 )
							.list()
							.size();
					assertEquals( 5, count );

					count = generateBaseQuery( session )
							.setMaxResults( 18 )
							.list()
							.size();
					assertEquals( 18, count );

					count = generateBaseSQLQuery( session )
							.setMaxResults( 13 )
							.list()
							.size();
					assertEquals( 13, count );
				}
		);
	}

	@Test
	@SessionFactory
	public void testOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List result;

					result = generateBaseHQLQuery( session )
							.setFirstResult( 3 )
							.list();
					DataPoint firstDataPointHQL = (DataPoint) result.get( 0 );

					result = generateBaseQuery( session )
							.setFirstResult( 3 )
							.list();
					DataPoint firstDataPointCriteria = (DataPoint) result.get( 0 );

					assertEquals(
							firstDataPointHQL,
							firstDataPointCriteria,
							"The first entry should be the same in HQL and Criteria"
					);
					assertEquals( 3, firstDataPointCriteria.getSequence(), "Wrong first result" );

					result = generateBaseSelectionQuery( session )
							.setFirstResult( 3 )
							.list();
					firstDataPointHQL = (DataPoint) result.get( 0 );

					assertEquals(
							firstDataPointHQL,
							firstDataPointCriteria,
							"The first entry should be the same in HQL and Criteria"
					);
				}
		);
	}

	@Test
	@SessionFactory
	public void testLimitOffset(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List result;

					result = generateBaseHQLQuery( session )
							.setFirstResult( 0 )
							.setMaxResults( 20 )
							.list();
					assertEquals( 20, result.size() );
					assertEquals( 0, ( (DataPoint) result.get( 0 ) ).getSequence() );
					assertEquals( 1, ( (DataPoint) result.get( 1 ) ).getSequence() );

					result = generateBaseSelectionQuery( session )
							.setFirstResult( 0 )
							.setMaxResults( 20 )
							.list();
					assertEquals( 20, result.size() );
					assertEquals( 0, ( (DataPoint) result.get( 0 ) ).getSequence() );
					assertEquals( 1, ( (DataPoint) result.get( 1 ) ).getSequence() );

					result = generateBaseQuery( session )
							.setFirstResult( 1 )
							.setMaxResults( 20 )
							.list();
					assertEquals( 20, result.size() );
					assertEquals( 1, ( (DataPoint) result.get( 0 ) ).getSequence() );
					assertEquals( 2, ( (DataPoint) result.get( 1 ) ).getSequence() );

					result = generateBaseQuery( session )
							.setFirstResult( 99 )
							.setMaxResults( Integer.MAX_VALUE - 200 )
							.list();
					assertEquals( 1, result.size() );
					assertEquals( 99, ( (DataPoint) result.get( 0 ) ).getSequence() );

					result = session.createQuery( "select distinct description from DataPoint order by description", String.class )
							.setFirstResult( 2 )
							.setMaxResults( 3 )
							.list();
					assertEquals( 3, result.size() );
					assertEquals( "Description: 2", result.get( 0 ) );
					assertEquals( "Description: 3", result.get( 1 ) );
					assertEquals( "Description: 4", result.get( 2 ) );

					result = session.createNativeQuery(
							"select description, xval, yval from DataPoint order by xval, yval", Object[].class )
							.setFirstResult( 2 )
							.setMaxResults( 5 )
							.list();
					assertEquals( 5, result.size() );
					Object[] row = (Object[]) result.get( 0 );
					assertTrue( row[0] instanceof String );

					result = session.createNativeQuery( "select * from DataPoint order by xval, yval", Object[].class )
							.setFirstResult( 2 )
							.setMaxResults( 5 )
							.list();
					assertEquals( 5, result.size() );

				}
		);
	}

	private Query generateBaseHQLQuery(Session session) {
		return session.createQuery( "select dp from DataPoint dp order by dp.sequence", DataPoint.class );
	}

	private SelectionQuery generateBaseSelectionQuery(Session session) {
		return session.createSelectionQuery( "select dp from DataPoint dp order by dp.sequence", DataPoint.class );
	}

	private Query generateBaseQuery(Session session) {
		CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
		CriteriaQuery<DataPoint> criteria = criteriaBuilder.createQuery( DataPoint.class );
		Root<DataPoint> root = criteria.from( DataPoint.class );
		return session.createQuery( criteria.orderBy( criteriaBuilder.asc( root.get( "sequence" ) ) ) );
	}

	private NativeQuery generateBaseSQLQuery(Session session) {
		return session.createNativeQuery( "select id, seqval, xval, yval, description from DataPoint order by seqval", Object[].class )
				.addEntity( DataPoint.class );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < NUMBER_OF_TEST_ROWS; i++ ) {
						DataPoint dataPoint = new DataPoint();
						dataPoint.setSequence( i );
						BigDecimal x = new BigDecimal( i * 0.1d ).setScale( 19, RoundingMode.DOWN );
						dataPoint.setX( x );
						dataPoint.setY( new BigDecimal( Math.cos( x.doubleValue() ) ).setScale(
								19,
								RoundingMode.DOWN
						) );
						dataPoint.setDescription( "Description: " + i % 5 );
						session.persist( dataPoint );
					}
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
