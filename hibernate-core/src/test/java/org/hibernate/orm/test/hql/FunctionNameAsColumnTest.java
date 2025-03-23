/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseASEDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests HQL and Criteria queries using DB columns having the same name as registered functions.
 *
 * @author Gail Badner
 */
@SkipForDialect(value = SybaseASEDialect.class, jiraKey = "HHH-6426")
@SkipForDialect(value = PostgresPlusDialect.class, comment = "Almost all of the tests result in 'ambiguous column' errors.")
public class FunctionNameAsColumnTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}
	@Override
	public String[] getMappings() {
		return new String[] {
				"hql/FunctionNamesAsColumns.hbm.xml"
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, false );
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionHQL() {
		inTransaction(
				s -> {
					EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
					e.setLower( 3 );
					e.setUpper( " abc " );
					s.persist( e );
				}
		);

		inTransaction(
				s -> {
					EntityWithArgFunctionAsColumn e = (EntityWithArgFunctionAsColumn) s.createQuery(
							"from EntityWithArgFunctionAsColumn" ).uniqueResult();
					assertEquals( 3, e.getLower() );
					assertEquals( " abc ", e.getUpper() );
				}
		);
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionCriteria() {
		inTransaction(
				s -> {
					EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
					e.setLower( 3 );
					e.setUpper( " abc " );
					s.persist( e );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<EntityWithArgFunctionAsColumn> criteria = criteriaBuilder.createQuery(
							EntityWithArgFunctionAsColumn.class );
					criteria.from( EntityWithArgFunctionAsColumn.class );

					EntityWithArgFunctionAsColumn e = s.createQuery( criteria ).uniqueResult();

//		e = ( EntityWithArgFunctionAsColumn ) s.createCriteria( EntityWithArgFunctionAsColumn.class ).uniqueResult();
					assertEquals( 3, e.getLower() );
					assertEquals( " abc ", e.getUpper() );
				}
		);
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionHQL() {
		inTransaction(
				s -> {
					EntityWithArgFunctionAsColumn e1 = new EntityWithArgFunctionAsColumn();
					e1.setLower( 3 );
					e1.setUpper( " abc " );
					EntityWithArgFunctionAsColumn e2 = new EntityWithArgFunctionAsColumn();
					e2.setLower( 999 );
					e2.setUpper( " xyz " );
					EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
					holder1.getEntityWithArgFunctionAsColumns().add( e1 );
					EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
					holder2.getEntityWithArgFunctionAsColumns().add( e2 );
					holder1.setNextHolder( holder2 );
					s.persist( holder1 );
				}
		);

		inTransaction(
				s -> {
					EntityWithFunctionAsColumnHolder holder1 = (EntityWithFunctionAsColumnHolder) s.createQuery(
							"from EntityWithFunctionAsColumnHolder h left join fetch h.entityWithArgFunctionAsColumns " +
									"left join fetch h.nextHolder left join fetch h.nextHolder.entityWithArgFunctionAsColumns " +
									"where h.nextHolder is not null" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
					assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
					assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
					assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
					EntityWithArgFunctionAsColumn e1 = (EntityWithArgFunctionAsColumn) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
					assertEquals( 3, e1.getLower() );
					assertEquals( " abc ", e1.getUpper() );
					assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
					EntityWithArgFunctionAsColumn e2 = (EntityWithArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns()
							.iterator()
							.next();
					assertEquals( 999, e2.getLower() );
					assertEquals( " xyz ", e2.getUpper() );
				}
		);
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionCriteria() {
		inTransaction(
				s -> {
					EntityWithArgFunctionAsColumn e1 = new EntityWithArgFunctionAsColumn();
					e1.setLower( 3 );
					e1.setUpper( " abc " );
					EntityWithArgFunctionAsColumn e2 = new EntityWithArgFunctionAsColumn();
					e2.setLower( 999 );
					e2.setUpper( " xyz " );
					EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
					holder1.getEntityWithArgFunctionAsColumns().add( e1 );
					EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
					holder2.getEntityWithArgFunctionAsColumns().add( e2 );
					holder1.setNextHolder( holder2 );
					s.persist( holder1 );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<EntityWithFunctionAsColumnHolder> criteria = criteriaBuilder.createQuery(
							EntityWithFunctionAsColumnHolder.class );
					Root<EntityWithFunctionAsColumnHolder> root = criteria.from( EntityWithFunctionAsColumnHolder.class );
					root.fetch( "entityWithArgFunctionAsColumns", JoinType.LEFT );
					root.fetch( "nextHolder", JoinType.LEFT )
							.fetch( "entityWithArgFunctionAsColumns", JoinType.LEFT );
					criteria.where( criteriaBuilder.isNotNull( root.get( "nextHolder" ) ) );

					EntityWithFunctionAsColumnHolder holder1 = s.createQuery( criteria ).uniqueResult();

//		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createCriteria( EntityWithFunctionAsColumnHolder.class )
//				.add( Restrictions.isNotNull( "nextHolder" ))
//				.setFetchMode( "entityWithArgFunctionAsColumns", FetchMode.JOIN )
//				.setFetchMode( "nextHolder", FetchMode.JOIN )
//				.setFetchMode( "nextHolder.entityWithArgFunctionAsColumns", FetchMode.JOIN )
//				.uniqueResult();
					assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
					assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
					assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
					assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
					EntityWithArgFunctionAsColumn e1 = (EntityWithArgFunctionAsColumn) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
					assertEquals( 3, e1.getLower() );
					assertEquals( " abc ", e1.getUpper() );
					assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
					EntityWithArgFunctionAsColumn e2 = (EntityWithArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns()
							.iterator()
							.next();
					assertEquals( 999, e2.getLower() );
					assertEquals( " xyz ", e2.getUpper() );
				}
		);
	}

	@Test
	public void testGetMultiColumnSameNameAsNoArgFunctionHQL() {
		Assume.assumeFalse(
				"current_date requires () but test is for noarg function that does not require ()",
				sessionFactory().getJdbcServices().getDialect().currentDate().contains( "(" )
		);

		inTransaction(
				s -> {
					EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
					e1.setCurrentDate( "blah blah blah" );
					EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
					e2.setCurrentDate( "yadda yadda yadda" );
					EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
					holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
					EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
					holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
					holder1.setNextHolder( holder2 );
					s.persist( holder1 );
				}
		);

		Session s = openSession();
		try {
			EntityWithFunctionAsColumnHolder holder1 = (EntityWithFunctionAsColumnHolder) s.createQuery(
					"from EntityWithFunctionAsColumnHolder h left join fetch h.entityWithNoArgFunctionAsColumns " +
							"left join fetch h.nextHolder left join fetch h.nextHolder.entityWithNoArgFunctionAsColumns " +
							"where h.nextHolder is not null" )
					.uniqueResult();
			assertTrue( Hibernate.isInitialized( holder1.getEntityWithNoArgFunctionAsColumns() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns() ) );
			assertEquals( 1, holder1.getEntityWithNoArgFunctionAsColumns().size() );
			s.close();

			EntityWithNoArgFunctionAsColumn e1 = (EntityWithNoArgFunctionAsColumn) holder1.getEntityWithNoArgFunctionAsColumns().iterator().next();
			assertEquals( "blah blah blah", e1.getCurrentDate() );
			assertEquals( 1, holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
			EntityWithNoArgFunctionAsColumn e2 = (EntityWithNoArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithNoArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( "yadda yadda yadda", e2.getCurrentDate() );
		}catch (Exception e){
			if (s.getTransaction().isActive()){
				s.getTransaction().rollback();
			}
			throw e;
		}finally {
			if(s.isOpen()) {
				s.close();
			}
		}
	}

	@Test
	public void testGetMultiColumnSameNameAsNoArgFunctionCriteria() {
		Assume.assumeFalse(
				"current_date requires () but test is for noarg function that does not require ()",
				sessionFactory().getJdbcServices().getDialect().currentDate().contains( "(" )
		);

		EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
		EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();

		inTransaction(
				s -> {
					EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
					e1.setCurrentDate( "blah blah blah" );
					EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
					e2.setCurrentDate( "yadda yadda yadda" );
					holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
					holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
					holder1.setNextHolder( holder2 );
					s.persist( holder1 );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<EntityWithFunctionAsColumnHolder> criteria = criteriaBuilder.createQuery(
							EntityWithFunctionAsColumnHolder.class );
					Root<EntityWithFunctionAsColumnHolder> root = criteria.from( EntityWithFunctionAsColumnHolder.class );
					root.fetch( "entityWithNoArgFunctionAsColumns", JoinType.LEFT );
					root.fetch( "nextHolder", JoinType.LEFT )
							.fetch( "entityWithNoArgFunctionAsColumns", JoinType.LEFT );
					criteria.where( criteriaBuilder.isNotNull( root.get( "nextHolder" ) ) );
					EntityWithFunctionAsColumnHolder holder = s.createQuery( criteria ).uniqueResult();
//		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createCriteria( EntityWithFunctionAsColumnHolder.class )
//				.add( Restrictions.isNotNull( "nextHolder" ))
//				.setFetchMode( "entityWithNoArgFunctionAsColumns", FetchMode.JOIN )
//				.setFetchMode( "nextHolder", FetchMode.JOIN )
//				.setFetchMode( "nextHolder.entityWithNoArgFunctionAsColumns", FetchMode.JOIN )
//				.uniqueResult();
					assertTrue( Hibernate.isInitialized( holder.getEntityWithNoArgFunctionAsColumns() ) );
					assertTrue( Hibernate.isInitialized( holder.getNextHolder() ) );
					assertTrue( Hibernate.isInitialized( holder.getNextHolder()
																.getEntityWithNoArgFunctionAsColumns() ) );
					assertEquals( 1, holder.getEntityWithNoArgFunctionAsColumns().size() );
					EntityWithNoArgFunctionAsColumn e1 = (EntityWithNoArgFunctionAsColumn) holder.getEntityWithNoArgFunctionAsColumns()
							.iterator()
							.next();
					assertEquals( "blah blah blah", e1.getCurrentDate() );
					assertEquals( 1, holder.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
					EntityWithNoArgFunctionAsColumn e2 = (EntityWithNoArgFunctionAsColumn) ( holder.getNextHolder() ).getEntityWithNoArgFunctionAsColumns()
							.iterator()
							.next();
					assertEquals( "yadda yadda yadda", e2.getCurrentDate() );

				}
		);
	}

	@Test
	public void testNoArgFcnAndColumnSameNameAsNoArgFunctionHQL() {
		Assume.assumeFalse(
				"current_date requires () but test is for noarg function that does not require ()",
				sessionFactory().getJdbcServices().getDialect().currentDate().contains( "(" )
		);

		EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
		EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();

		inTransaction(
				s -> {
					e1.setCurrentDate( "blah blah blah" );
					e2.setCurrentDate( "yadda yadda yadda" );
					EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
					holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
					EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
					holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
					holder1.setNextHolder( holder2 );
					s.persist( holder1 );
				}
		);

		inTransaction(
				s -> {
					List results = s.createQuery(
							"select str(current_date), currentDate from EntityWithNoArgFunctionAsColumn"
					).list();
					assertEquals( 2, results.size() );
					assertEquals( ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0] );
					assertTrue( !( (Object[]) results.get( 0 ) )[0].equals( ( (Object[]) results.get( 0 ) )[1] ) );
					assertTrue( !( (Object[]) results.get( 1 ) )[0].equals( ( (Object[]) results.get( 1 ) )[1] ) );
					assertTrue( ( (Object[]) results.get( 0 ) )[1].equals( e1.getCurrentDate() ) ||
										( (Object[]) results.get( 0 ) )[1].equals( e2.getCurrentDate() ) );
					assertTrue( ( (Object[]) results.get( 1 ) )[1].equals( e1.getCurrentDate() ) ||
										( (Object[]) results.get( 1 ) )[1].equals( e2.getCurrentDate() ) );
					assertFalse( ( (Object[]) results.get( 0 ) )[1].equals( ( (Object[]) results.get( 1 ) )[1] ) );

				}
		);
	}

	@After
	public void cleanup() {
		inTransaction(
				s -> {
					s.createQuery( "delete from EntityWithArgFunctionAsColumn" ).executeUpdate();

				}
		);

		inTransaction(
				s -> {
					s.createQuery( "delete from EntityWithNoArgFunctionAsColumn" ).executeUpdate();

				}
		);

		inTransaction(
				s -> {
					s.createQuery( "delete from EntityWithFunctionAsColumnHolder where nextHolder is not null" )
							.executeUpdate();
					s.createQuery( "delete from EntityWithFunctionAsColumnHolder" ).executeUpdate();
				}
		);
	}
}
