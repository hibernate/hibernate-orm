/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.function.SQLFunction;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.SkipLog;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests HQL and Criteria queries using DB columns having the same name as registered functions.
 *
 * @author Gail Badner
 */
@SkipForDialects({
	@SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-6426"),
	@SkipForDialect( value = PostgresPlusDialect.class, comment = "Almost all of the tests result in 'ambiguous column' errors.")
})
public class FunctionNameAsColumnTest  extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
				"hql/FunctionNamesAsColumns.hbm.xml"
		};
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "false" );
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionHQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
		e.setLower( 3 );
		e.setUpper( " abc " );
		s.persist( e );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		e = ( EntityWithArgFunctionAsColumn ) s.createQuery( "from EntityWithArgFunctionAsColumn" ).uniqueResult();
		assertEquals( 3, e.getLower() );
		assertEquals( " abc ", e.getUpper() );
		t.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
		e.setLower( 3 );
		e.setUpper( " abc " );
		s.persist( e );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		e = ( EntityWithArgFunctionAsColumn ) s.createCriteria( EntityWithArgFunctionAsColumn.class ).uniqueResult();
		assertEquals( 3, e.getLower() );
		assertEquals( " abc ", e.getUpper() );
		t.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionHQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
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
		s.save( holder1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createQuery(
				"from EntityWithFunctionAsColumnHolder h left join fetch h.entityWithArgFunctionAsColumns " +
						"left join fetch h.nextHolder left join fetch h.nextHolder.entityWithArgFunctionAsColumns " +
						"where h.nextHolder is not null" )
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
		assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
		e1 = ( EntityWithArgFunctionAsColumn ) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
		assertEquals( 3, e1.getLower() );
		assertEquals( " abc ", e1.getUpper() );
		assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
		e2 = ( EntityWithArgFunctionAsColumn ) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns().iterator().next();
		assertEquals( 999, e2.getLower() );
		assertEquals( " xyz ", e2.getUpper() );
		t.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
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
		s.save( holder1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createCriteria( EntityWithFunctionAsColumnHolder.class )
				.add( Restrictions.isNotNull( "nextHolder" ))
				.setFetchMode( "entityWithArgFunctionAsColumns", FetchMode.JOIN )
				.setFetchMode( "nextHolder", FetchMode.JOIN )
				.setFetchMode( "nextHolder.entityWithArgFunctionAsColumns", FetchMode.JOIN )
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
		assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
		e1 = ( EntityWithArgFunctionAsColumn ) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
		assertEquals( 3, e1.getLower() );
		assertEquals( " abc ", e1.getUpper() );
		assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
		e2 = ( EntityWithArgFunctionAsColumn ) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns().iterator().next();
		assertEquals( 999, e2.getLower() );
		assertEquals( " xyz ", e2.getUpper() );
		t.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testGetMultiColumnSameNameAsNoArgFunctionHQL() throws Exception {
		SQLFunction function = sessionFactory().getSqlFunctionRegistry().findSQLFunction( "current_date" );
		if ( function == null || function.hasParenthesesIfNoArguments() ) {
			SkipLog.reportSkip( "current_date reuires ()", "tests noarg function that does not require ()" );
			return;
		}

		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
		e1.setCurrentDate( "blah blah blah" );
		EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
		e2.setCurrentDate( "yadda yadda yadda" );
		EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
		holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
		EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
		holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
		holder1.setNextHolder( holder2 );
		s.save( holder1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createQuery(
				"from EntityWithFunctionAsColumnHolder h left join fetch h.entityWithNoArgFunctionAsColumns " +
						"left join fetch h.nextHolder left join fetch h.nextHolder.entityWithNoArgFunctionAsColumns " +
						"where h.nextHolder is not null" )
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( holder1.getEntityWithNoArgFunctionAsColumns() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns() ) );
		assertEquals( 1, holder1.getEntityWithNoArgFunctionAsColumns().size() );
		t.commit();
		s.close();

		e1 = ( EntityWithNoArgFunctionAsColumn ) holder1.getEntityWithNoArgFunctionAsColumns().iterator().next();
		assertEquals( "blah blah blah", e1.getCurrentDate() );
		assertEquals( 1, holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
		e2 = ( EntityWithNoArgFunctionAsColumn ) ( holder1.getNextHolder() ).getEntityWithNoArgFunctionAsColumns().iterator().next();
		assertEquals( "yadda yadda yadda", e2.getCurrentDate() );

		cleanup();
	}

	@Test
	public void testGetMultiColumnSameNameAsNoArgFunctionCriteria() {
		SQLFunction function = sessionFactory().getSqlFunctionRegistry().findSQLFunction( "current_date" );
		if ( function == null || function.hasParenthesesIfNoArguments() ) {
			SkipLog.reportSkip( "current_date reuires ()", "tests noarg function that does not require ()" );
			return;
		}

		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
		e1.setCurrentDate( "blah blah blah" );
		EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
		e2.setCurrentDate( "yadda yadda yadda" );
		EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
		holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
		EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
		holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
		holder1.setNextHolder( holder2 );
		s.save( holder1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		holder1 = ( EntityWithFunctionAsColumnHolder ) s.createCriteria( EntityWithFunctionAsColumnHolder.class )
				.add( Restrictions.isNotNull( "nextHolder" ))
				.setFetchMode( "entityWithNoArgFunctionAsColumns", FetchMode.JOIN )
				.setFetchMode( "nextHolder", FetchMode.JOIN )
				.setFetchMode( "nextHolder.entityWithNoArgFunctionAsColumns", FetchMode.JOIN )
				.uniqueResult();
		assertTrue( Hibernate.isInitialized( holder1.getEntityWithNoArgFunctionAsColumns() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
		assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns() ) );
		assertEquals( 1, holder1.getEntityWithNoArgFunctionAsColumns().size() );
		e1 = ( EntityWithNoArgFunctionAsColumn ) holder1.getEntityWithNoArgFunctionAsColumns().iterator().next();
		assertEquals( "blah blah blah", e1.getCurrentDate() );
		assertEquals( 1, holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
		e2 = ( EntityWithNoArgFunctionAsColumn ) ( holder1.getNextHolder() ).getEntityWithNoArgFunctionAsColumns().iterator().next();
		assertEquals( "yadda yadda yadda", e2.getCurrentDate() );
		t.commit();
		s.close();

		cleanup();
	}

	@Test
	public void testNoArgFcnAndColumnSameNameAsNoArgFunctionHQL() {
		SQLFunction function = sessionFactory().getSqlFunctionRegistry().findSQLFunction( "current_date" );
		if ( function == null || function.hasParenthesesIfNoArguments() ) {
			SkipLog.reportSkip( "current_date reuires ()", "tests noarg function that does not require ()" );
			return;
		}

		Session s = openSession();
		Transaction t = s.beginTransaction();
		EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
		e1.setCurrentDate( "blah blah blah" );
		EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
		e2.setCurrentDate( "yadda yadda yadda" );
		EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
		holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
		EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
		holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
		holder1.setNextHolder( holder2 );
		s.save( holder1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery(
						"select str(current_date), currentDate from EntityWithNoArgFunctionAsColumn"
				).list();
		assertEquals( 2, results.size() );
		assertEquals( ( ( Object[] ) results.get( 0 ) )[ 0 ], ( ( Object[] ) results.get( 1 ) )[ 0 ] );
		assertTrue( ! ( ( Object[] ) results.get( 0 ) )[ 0 ].equals( ( ( Object[] ) results.get( 0 ) )[ 1 ] ) );
		assertTrue( ! ( ( Object[] ) results.get( 1 ) )[ 0 ].equals( ( ( Object[] ) results.get( 1 ) )[ 1 ] ) );
		assertTrue( ( ( Object[] ) results.get( 0 ) )[ 1 ].equals( e1.getCurrentDate() ) ||
				     ( ( Object[] ) results.get( 0 ) )[ 1 ].equals( e2.getCurrentDate() ) );
		assertTrue( ( ( Object[] ) results.get( 1 ) )[ 1 ].equals( e1.getCurrentDate() ) ||
				     ( ( Object[] ) results.get( 1 ) )[ 1 ].equals( e2.getCurrentDate() ) );
		assertFalse( ( ( Object[] ) results.get( 0 ) )[ 1 ].equals( ( ( Object[] ) results.get( 1 ) )[ 1 ] ) );
		t.commit();
		s.close();

		cleanup();
	}

	private void cleanup() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery( "delete from EntityWithArgFunctionAsColumn" ).executeUpdate();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from EntityWithNoArgFunctionAsColumn" ).executeUpdate();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from EntityWithFunctionAsColumnHolder where nextHolder is not null" ).executeUpdate();
		s.createQuery( "delete from EntityWithFunctionAsColumnHolder" ).executeUpdate();
		t.commit();
		s.close();
	}
}
