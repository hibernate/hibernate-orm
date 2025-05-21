/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.functional.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.community.dialect.CacheDialect;
import org.hibernate.query.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.orm.test.legacy.Blobber;
import org.hibernate.orm.test.legacy.Broken;
import org.hibernate.orm.test.legacy.Fixed;
import org.hibernate.orm.test.legacy.Simple;
import org.hibernate.orm.test.legacy.Single;
import org.junit.Test;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for function support on CacheSQL...
 *
 * @author Jonathan Levinson
 */
@RequiresDialect( value = CacheDialect.class )
public class SQLFunctionsInterSystemsTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[] {
				"legacy/AltSimple.hbm.xml",
				"legacy/Broken.hbm.xml",
				"legacy/Blobber.hbm.xml",
				"dialect/functional/cache/TestInterSystemsFunctionsClass.hbm.xml"
		};
	}

	@Test
	public void testDialectSQLFunctions() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Simple simple = new Simple( Long.valueOf( 10 ) );
		simple.setName("Simple Dialect Function Test");
		simple.setAddress("Simple Address");
		simple.setPay(new Float(45.8));
		simple.setCount(2);
		s.persist( simple );

		// Test to make sure allocating a specified object operates correctly.
		assertTrue(
				s.createQuery( "select new org.hibernate.test.legacy.S(s.count, s.address) from Simple s" ).list().size() == 1
		);

		// Quick check the base dialect functions operate correctly
		assertTrue(
				s.createQuery( "select max(s.count) from Simple s" ).list().size() == 1
		);
		assertTrue(
				s.createQuery( "select count(*) from Simple s" ).list().size() == 1
		);

		List rset = s.createQuery( "select s.name, sysdate, floor(s.pay), round(s.pay,0) from Simple s" ).list();
		assertNotNull("Name string should have been returned",(((Object[])rset.get(0))[0]));
		assertNotNull("Todays Date should have been returned",(((Object[])rset.get(0))[1]));
		assertEquals("floor(45.8) result was incorrect ", new Integer(45), ( (Object[]) rset.get(0) )[2] );
		assertEquals("round(45.8) result was incorrect ", new Float(46), ( (Object[]) rset.get(0) )[3] );

		simple.setPay(new Float(-45.8));
		simple = s.merge(simple);

		// Test type conversions while using nested functions (Float to Int).
		rset = s.createQuery( "select abs(round(s.pay,0)) from Simple s" ).list();
		assertEquals("abs(round(-45.8)) result was incorrect ", new Float(46), rset.get(0));

		// Test a larger depth 3 function example - Not a useful combo other than for testing
		assertTrue(
				s.createQuery( "select floor(round(sysdate,1)) from Simple s" ).list().size() == 1
		);

		// Test the oracle standard NVL funtion as a test of multi-param functions...
		simple.setPay(null);
		simple = s.merge(simple);
		Double value = (Double) s.createQuery("select mod( nvl(s.pay, 5000), 2 ) from Simple as s where s.id = 10").list().get(0);
		assertTrue( 0 == value.intValue() );

		// Test the hsql standard MOD funtion as a test of multi-param functions...
		value = (Double) s.createQuery( "select MOD(s.count, 2) from Simple as s where s.id = 10" )
				.list()
				.get(0);
		assertTrue( 0 == value.intValue() );

		s.remove(simple);
		t.commit();
		s.close();
	}

	public void testSetProperties()  {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf( 10 ) );
		simple.setName("Simple 1");
		s.persist( simple );
		Query q = s.createQuery("from Simple s where s.name=:name and s.count=:count");
		q.setProperties(simple);
		assertTrue( q.list().get(0)==simple );
		//misuse of "Single" as a propertyobject, but it was the first testclass i found with a collection ;)
		Single single = new Single() { // trivial hack to test properties with arrays.
			@SuppressWarnings( {"unchecked"})
			String[] getStuff() {
				return (String[]) getSeveral().toArray(new String[getSeveral().size()]);
			}
		};

		List l = new ArrayList();
		l.add("Simple 1");
		l.add("Slimeball");
		single.setSeveral(l);
		q = s.createQuery("from Simple s where s.name in (:several)");
		q.setProperties(single);
		assertTrue( q.list().get(0)==simple );


		q = s.createQuery("from Simple s where s.name in (:stuff)");
		q.setProperties(single);
		assertTrue( q.list().get(0)==simple );
		s.remove(simple);
		t.commit();
		s.close();
	}

	public void testBroken() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Broken b = new Fixed();
		b.setId( Long.valueOf( 123 ));
		b.setOtherId("foobar");
		s.persist(b);
		s.flush();
		b.setTimestamp( new Date() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		b = s.merge(b);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		b = s.getReference( Broken.class, b );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.remove(b);
		t.commit();
		s.close();
	}

	public void testNothinToUpdate() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf(10) );
		simple.setName("Simple 1");
		s.persist( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		simple = s.merge( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		simple = s.merge( simple );
		s.remove(simple);
		t.commit();
		s.close();
	}

	public void testCachedQuery() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf(10) );
		simple.setName("Simple 1");
		s.persist( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Query q = s.createQuery("from Simple s where s.name=?");
		q.setCacheable(true);
		q.setParameter(0, "Simple 1");
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		q = s.createQuery("from Simple s where s.name=:name");
		q.setCacheable(true);
		q.setParameter("name", "Simple 1");
		assertTrue( q.list().size()==1 );
		simple = (Simple) q.list().get(0);

		q.setParameter("name", "Simple 2");
		assertTrue( q.list().size()==0 );
		assertTrue( q.list().size()==0 );
		simple.setName("Simple 2");
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s where s.name=:name");
		q.setParameter("name", "Simple 2");
		q.setCacheable(true);
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		simple = s.merge( simple );
		s.remove(simple);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s where s.name=?");
		q.setCacheable(true);
		q.setParameter(0, "Simple 1");
		assertTrue( q.list().size()==0 );
		assertTrue( q.list().size()==0 );
		t.commit();
		s.close();
	}

	public void testCachedQueryRegion() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf(10) );
		simple.setName("Simple 1");
		s.persist( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Query q = s.createQuery("from Simple s where s.name=?");
		q.setCacheRegion("foo");
		q.setCacheable(true);
		q.setParameter(0, "Simple 1");
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		q = s.createQuery("from Simple s where s.name=:name");
		q.setCacheRegion("foo");
		q.setCacheable(true);
		q.setParameter("name", "Simple 1");
		assertTrue( q.list().size()==1 );
		simple = (Simple) q.list().get(0);

		q.setParameter("name", "Simple 2");
		assertTrue( q.list().size()==0 );
		assertTrue( q.list().size()==0 );
		simple.setName("Simple 2");
		assertTrue( q.list().size()==1 );
		assertTrue( q.list().size()==1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		simple = s.merge( simple );
		s.remove(simple);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s where s.name=?");
		q.setCacheRegion("foo");
		q.setCacheable(true);
		q.setParameter(0, "Simple 1");
		assertTrue( q.list().size()==0 );
		assertTrue( q.list().size()==0 );
		t.commit();
		s.close();
	}

	public void testSQLFunctions() {
		try(Session s = openSession()) {
			Transaction t = s.beginTransaction();
			Simple simple = new Simple( Long.valueOf( 10 ) );
			simple.setName( "Simple 1" );
			s.persist( simple );

			s.createQuery( "from Simple s where repeat('foo', 3) = 'foofoofoo'" ).list();
			s.createQuery( "from Simple s where repeat(s.name, 3) = 'foofoofoo'" ).list();
			s.createQuery( "from Simple s where repeat( lower(s.name), (3 + (1-1)) / 2) = 'foofoofoo'" ).list();

			assertTrue(
					s.createQuery( "from Simple s where upper( s.name ) ='SIMPLE 1'" ).list().size() == 1
			);
			assertTrue(
					s.createQuery(
							"from Simple s where not( upper( s.name ) ='yada' or 1=2 or 'foo'='bar' or not('foo'='foo') or 'foo' like 'bar' )"
					).list()
							.size() == 1
			);

			assertTrue(
					s.createQuery( "from Simple s where lower( s.name || ' foo' ) ='simple 1 foo'" ).list().size() == 1
			);
			assertTrue(
					s.createQuery( "from Simple s where lower( concat(s.name, ' foo') ) ='simple 1 foo'" )
							.list()
							.size() == 1
			);

			Simple other = new Simple( Long.valueOf( 20 ) );
			other.setName( "Simple 2" );
			other.setCount( 12 );
			simple.setOther( other );
			s.persist( other );
			//s.find("from Simple s where s.name ## 'cat|rat|bag'");
			assertTrue(
					s.createQuery( "from Simple s where upper( s.other.name ) ='SIMPLE 2'" ).list().size() == 1
			);
			assertTrue(
					s.createQuery( "from Simple s where not ( upper( s.other.name ) ='SIMPLE 2' )" ).list().size() == 0
			);
			assertTrue(
					s.createQuery(
							"select distinct s from Simple s where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2"
					).list()
							.size() == 1
			);
			assertTrue(
					s.createQuery(
							"select s from Simple s where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2 order by s.other.count"
					).list()
							.size() == 1
			);
			Simple min = new Simple( Long.valueOf( 30 ) );
			min.setCount( -1 );
			s.persist( min );

			assertTrue(
					s.createQuery( "from Simple s where s.count > ( select min(sim.count) from Simple sim )" )
							.list()
							.size() == 2
			);
			t.commit();
			t = s.beginTransaction();
			assertTrue(
					s.createQuery(
							"from Simple s where s = some( select sim from Simple sim where sim.count>=0 ) and s.count >= 0"
					).list()
							.size() == 2
			);
			assertTrue(
					s.createQuery(
							"from Simple s where s = some( select sim from Simple sim where sim.other.count=s.other.count ) and s.other.count > 0"
					).list()
							.size() == 1
			);

			List list = s.createQuery( "select sum(s.count) from Simple s group by s.count having sum(s.count) > 10" )
					.list();
			assertEquals( 1, list.size() );
			assertEquals( Long.valueOf( 12 ), list.get( 0 ) );
			list = s.createQuery( "select s.count from Simple s group by s.count having s.count = 12" ).list();
			assertFalse( list.isEmpty() );

			s.createQuery(
					"select s.id, s.count, count(t), max(t.date) from Simple s, Simple t where s.count = t.count group by s.id, s.count order by s.count"
			).list();

			Query q = s.createQuery( "from Simple s" );
			q.setMaxResults( 10 );
			assertTrue( q.list().size() == 3 );
			q = s.createQuery( "from Simple s" );
			q.setMaxResults( 1 );
			assertTrue( q.list().size() == 1 );
			q = s.createQuery( "from Simple s" );
			assertTrue( q.list().size() == 3 );
			q = s.createQuery( "from Simple s where s.name = ?" );
			q.setParameter( 0, "Simple 1" );
			assertTrue( q.list().size() == 1 );
			q = s.createQuery( "from Simple s where s.name = ? and upper(s.name) = ?" );
			q.setParameter( 1, "SIMPLE 1" );
			q.setParameter( 0, "Simple 1" );
			q.setFirstResult( 0 );
			assertFalse( q.list().isEmpty() );
			q = s.createQuery(
					"from Simple s where s.name = :foo and upper(s.name) = :bar or s.count=:count or s.count=:count + 1" );
			q.setParameter( "bar", "SIMPLE 1" );
			q.setParameter( "foo", "Simple 1" );
			q.setParameter( "count", 69 );
			q.setFirstResult( 0 );
			assertFalse( q.list().isEmpty() );
			q = s.createQuery( "select s.id from Simple s" );
			q.setFirstResult( 1 );
			q.setMaxResults( 2 );
			list = q.list();
			for ( Object l : list ) {
				assertTrue( l instanceof Long );
			}
//		int i=0;
//		while ( list.hasNext() ) {
//			assertTrue( list.next() instanceof Long );
//			i++;
//		}
			assertEquals( 2, list.size() );
			q = s.createQuery( "select all s, s.other from Simple s where s = :s" );
			q.setParameter( "s", simple );
			assertTrue( q.list().size() == 1 );


			q = s.createQuery( "from Simple s where s.name in (:name_list) and s.count > :count", Simple.class );
			HashSet set = new HashSet();
			set.add( "Simple 1" );
			set.add( "foo" );
			q.setParameterList( "name_list", set );
			q.setParameter( "count", new Integer( -1 ) );
			assertTrue( q.list().size() == 1 );

			try (ScrollableResults sr = s.createQuery( "from Simple s" ).scroll()) {
				sr.next();
				sr.get();
			}

			s.remove( other );
			s.remove( simple );
			s.remove( min );
			t.commit();
		}

	}

	public void testBlobClob() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Blobber b = new Blobber();
		b.setBlob( getLobHelper().createBlob( "foo/bar/baz".getBytes() ) );
		b.setClob( getLobHelper().createClob("foo/bar/baz") );
		s.persist(b);
		//s.refresh(b);
		//assertTrue( b.getClob() instanceof ClobImpl );
		s.flush();
		s.refresh(b);
		//b.getBlob().setBytes( 2, "abc".getBytes() );
		log.debug("levinson: just bfore b.getClob()");
		b.getClob().getSubString(2, 3);
		//b.getClob().setString(2, "abc");
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		b = s.getReference( Blobber.class, b.getId() );
		Blobber b2 = new Blobber();
		s.persist(b2);
		b2.setBlob( b.getBlob() );
		b.setBlob(null);
		//assertTrue( b.getClob().getSubString(1, 3).equals("fab") );
		b.getClob().getSubString(1, 6);
		//b.getClob().setString(1, "qwerty");
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		b = s.getReference( Blobber.class, b.getId() );
		b.setClob( getLobHelper().createClob("xcvfxvc xcvbx cvbx cvbx cvbxcvbxcvbxcvb") );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		b = s.getReference( Blobber.class, b.getId() );
		assertTrue( b.getClob().getSubString(1, 7).equals("xcvfxvc") );
		//b.getClob().setString(5, "1234567890");
		s.flush();
		s.getTransaction().commit();
		s.close();
	}

	public void testSqlFunctionAsAlias() {
		String functionName = locateAppropriateDialectFunctionNameForAliasTest();
		if (functionName == null) {
			log.info("Dialect does not list any no-arg functions");
			return;
		}

		log.info("Using function named [" + functionName + "] for 'function as alias' test");
		String query = "select " + functionName + " from Simple as " + functionName + " where " + functionName + ".id = 10";

		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf(10) );
		simple.setName("Simple 1");
		s.persist( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List result = s.createQuery( query ).list();
		assertTrue( result.size() == 1 );
		assertTrue(result.get(0) instanceof Simple);
		s.remove( result.get(0) );
		t.commit();
		s.close();
	}

	@SuppressWarnings( {"ForLoopReplaceableByForEach"})
	private String locateAppropriateDialectFunctionNameForAliasTest() {
//		for (Iterator itr = getDialect().getFunctions().entrySet().iterator(); itr.hasNext(); ) {
//			final Map.Entry entry = (Map.Entry) itr.next();
//			final SQLFunction function = (SQLFunction) entry.getValue();
//			if ( !function.hasArguments() && !function.hasParenthesesIfNoArguments() ) {
//				return (String) entry.getKey();
//			}
//		}
		return null;
	}

	public void testCachedQueryOnInsert() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Simple simple = new Simple( Long.valueOf(10) );
		simple.setName("Simple 1");
		s.persist( simple );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Query q = s.createQuery("from Simple s");
		List list = q.setCacheable(true).list();
		assertTrue( list.size()==1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s");
		list = q.setCacheable(true).list();
		assertTrue( list.size()==1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Simple simple2 = new Simple( Long.valueOf(12) );
		simple2.setCount(133);
		s.persist( simple2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s");
		list = q.setCacheable(true).list();
		assertTrue( list.size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		q = s.createQuery("from Simple s");
		list = q.setCacheable(true).list();
		assertTrue( list.size()==2 );
		for ( Object o : list ) {
			s.remove( o );
		}
		t.commit();
		s.close();

	}

	public void testInterSystemsFunctions() throws Exception {
		Calendar cal = new GregorianCalendar();
		cal.set(1977,6,3,0,0,0);
		java.sql.Timestamp testvalue = new java.sql.Timestamp(cal.getTimeInMillis());
		testvalue.setNanos(0);
		Calendar cal3 = new GregorianCalendar();
		cal3.set(1976,2,3,0,0,0);
		java.sql.Timestamp testvalue3 = new java.sql.Timestamp(cal3.getTimeInMillis());
		testvalue3.setNanos(0);

		final Session s = openSession();
		s.beginTransaction();
		try {
			s.doWork(
					new Work() {
						@Override
						public void execute(Connection connection) throws SQLException {
							Statement stmt = ((SessionImplementor)s).getJdbcCoordinator().getStatementPreparer().createStatement();
							((SessionImplementor)s).getJdbcCoordinator().getResultSetReturn().executeUpdate( stmt, "DROP FUNCTION spLock FROM TestInterSystemsFunctionsClass" );
						}
					}
			);
		}
		catch (Exception ex) {
			System.out.println("as we expected stored procedure sp does not exist when we drop it");

		}
		s.getTransaction().commit();

		s.beginTransaction();
		s.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						Statement stmt = ( (SessionImplementor) s ).getJdbcCoordinator()
								.getStatementPreparer()
								.createStatement();
						String create_function = "CREATE FUNCTION SQLUser.TestInterSystemsFunctionsClass_spLock\n" +
								"     ( INOUT pHandle %SQLProcContext, \n" +
								"       ROWID INTEGER \n" +
								" )\n" +
								" FOR User.TestInterSystemsFunctionsClass " +
								"    PROCEDURE\n" +
								"    RETURNS INTEGER\n" +
								"    LANGUAGE OBJECTSCRIPT\n" +
								"    {\n" +
								"        q 0\n" +
								"     }";
						( (SessionImplementor) s ).getJdbcCoordinator().getResultSetReturn().executeUpdate(
								stmt,
								create_function
						);
					}
				}
		);
		s.getTransaction().commit();

		s.beginTransaction();

		TestInterSystemsFunctionsClass object = new TestInterSystemsFunctionsClass( Long.valueOf( 10 ) );
		object.setDateText( "1977-07-03" );
		object.setDate1( testvalue );
		object.setDate3( testvalue3 );
		s.persist( object );
		s.getTransaction().commit();
		s.close();

		Session s2 = openSession();
		s2.beginTransaction();
		TestInterSystemsFunctionsClass test = s2.get(TestInterSystemsFunctionsClass.class, 10L );
		assertTrue( test.getDate1().equals(testvalue));
		test = (TestInterSystemsFunctionsClass) s2.byId( TestInterSystemsFunctionsClass.class )
				.with( LockMode.NONE )
				.load( 10L );
		assertTrue( test.getDate1().equals(testvalue));
		Date value = (Date) s2.createQuery( "select nvl(o.date,o.dateText) from TestInterSystemsFunctionsClass as o" )
				.list()
				.get(0);
		assertTrue( value.equals(testvalue));
		Object nv = s2.createQuery( "select nullif(o.dateText,o.dateText) from TestInterSystemsFunctionsClass as o" )
				.list()
				.get(0);
		assertTrue( nv == null);
		String dateText = (String) s2.createQuery(
				"select nvl(o.dateText,o.date) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue( dateText.equals("1977-07-03"));
		value = (Date) s2.createQuery( "select ifnull(o.date,o.date1) from TestInterSystemsFunctionsClass as o" )
				.list()
				.get(0);
		assertTrue( value.equals(testvalue));
		value = (Date) s2.createQuery( "select ifnull(o.date3,o.date,o.date1) from TestInterSystemsFunctionsClass as o" )
				.list()
				.get(0);
		assertTrue( value.equals(testvalue));
		Integer pos = (Integer) s2.createQuery(
				"select position('07', o.dateText) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue(pos.intValue() == 6);
		String st = (String) s2.createQuery( "select convert(o.date1, SQL_TIME) from TestInterSystemsFunctionsClass as o" )
				.list()
				.get(0);
		assertTrue( st.equals("00:00:00"));
		java.sql.Time tm = (java.sql.Time) s2.createQuery(
				"select cast(o.date1, time) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue( tm.toString().equals("00:00:00"));
		Double diff = (Double) s2.createQuery(
				"select timestampdiff(SQL_TSI_FRAC_SECOND, o.date3, o.date1) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue(diff.doubleValue() != 0.0);
		diff = (Double) s2.createQuery(
				"select timestampdiff(SQL_TSI_MONTH, o.date3, o.date1) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue(diff.doubleValue() == 16.0);
		diff = (Double) s2.createQuery(
				"select timestampdiff(SQL_TSI_WEEK, o.date3, o.date1) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue(diff.doubleValue() >= 16*4);
		diff = (Double) s2.createQuery(
				"select timestampdiff(SQL_TSI_YEAR, o.date3, o.date1) from TestInterSystemsFunctionsClass as o"
		).list()
				.get(0);
		assertTrue(diff.doubleValue() == 1.0);

		s2.getTransaction().commit();
		s2.close();
	}

}
