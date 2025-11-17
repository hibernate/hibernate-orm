/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.functional.cache;

import org.hibernate.LockMode;
import org.hibernate.ScrollableResults;
import org.hibernate.community.dialect.CacheDialect;
import org.hibernate.orm.test.legacy.Blobber;
import org.hibernate.orm.test.legacy.Broken;
import org.hibernate.orm.test.legacy.Fixed;
import org.hibernate.orm.test.legacy.Simple;
import org.hibernate.orm.test.legacy.Single;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.getLobHelper;

/**
 * Tests for function support on CacheSQL...
 *
 * @author Jonathan Levinson
 */
@RequiresDialect(value = CacheDialect.class)
@DomainModel(
		xmlMappings = {
				"legacy/AltSimple.hbm.xml",
				"legacy/Broken.hbm.xml",
				"legacy/Blobber.hbm.xml",
				"dialect/functional/cache/TestInterSystemsFunctionsClass.hbm.xml"
		}
)
@SessionFactory
public class SQLFunctionsInterSystemsTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testDialectSQLFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple simple = new Simple( 10L );
					simple.setName( "Simple Dialect Function Test" );
					simple.setAddress( "Simple Address" );
					simple.setPay( 45.8F );
					simple.setCount( 2 );
					session.persist( simple );

					// Test to make sure allocating a specified object operates correctly.
					assertThat( session.createQuery(
							"select new org.hibernate.test.legacy.S(s.count, s.address) from Simple s" ).list() )
							.hasSize( 1 );

					// Quick check the base dialect functions operate correctly
					assertThat( session.createQuery( "select max(s.count) from Simple s" ).list() )
							.hasSize( 1 );
					assertThat( session.createQuery( "select count(*) from Simple s" ).list() )
							.hasSize( 1 );

					List rset = session.createQuery(
									"select s.name, sysdate, floor(s.pay), round(s.pay,0) from Simple s" )
							.list();
					assertThat( (((Object[]) rset.get( 0 ))[0]) )
							.describedAs( "Name string should have been returned" )
							.isNotNull();
					assertThat( (((Object[]) rset.get( 0 ))[1]) )
							.describedAs( "Todays Date should have been returned" )
							.isNotNull();
					assertThat( ((Object[]) rset.get( 0 ))[2] )
							.describedAs( "floor(45.8) result was incorrect " )
							.isEqualTo( 45 );
					assertThat( ((Object[]) rset.get( 0 ))[3] )
							.describedAs( "round(45.8) result was incorrect " )
							.isEqualTo( 46F );

					simple.setPay( -45.8F );
					simple = session.merge( simple );

					// Test type conversions while using nested functions (Float to Int).
					rset = session.createQuery( "select abs(round(s.pay,0)) from Simple s" ).list();
					assertThat( rset.get( 0 ) )
							.describedAs( "abs(round(-45.8)) result was incorrect " ).isEqualTo( 46F );

					// Test a larger depth 3 function example - Not a useful combo other than for testing
					assertThat( session.createQuery( "select floor(round(sysdate,1)) from Simple s" ).list() )
							.hasSize( 1 );

					// Test the oracle standard NVL funtion as a test of multi-param functions...
					simple.setPay( null );
					simple = session.merge( simple );
					Double value = session.createQuery(
									"select mod( nvl(s.pay, 5000), 2 ) from Simple as s where s.id = 10", Double.class ).list()
							.get( 0 );
					assertThat( value.intValue() ).isEqualTo( 0 );

					// Test the hsql standard MOD funtion as a test of multi-param functions...
					value = session.createQuery( "select MOD(s.count, 2) from Simple as s where s.id = 10",
									Double.class )
							.list()
							.get( 0 );
					assertThat( value.intValue() ).isEqualTo( 0 );

					session.remove( simple );
				}
		);
	}

	public void testSetProperties(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple simple = new Simple( Long.valueOf( 10 ) );
					simple.setName( "Simple 1" );
					session.persist( simple );
					Query<Simple> q = session.createQuery( "from Simple s where s.name=:name and s.count=:count",
							Simple.class );
					q.setProperties( simple );
					assertThat( q.list().get( 0 ) ).isSameAs( simple );
					//misuse of "Single" as a propertyobject, but it was the first testclass i found with a collection ;)
					Single single = new Single() { // trivial hack to test properties with arrays.
						@SuppressWarnings({"unchecked"})
						String[] getStuff() {
							return (String[]) getSeveral().toArray( new String[0] );
						}
					};

					List<String> l = new ArrayList<>();
					l.add( "Simple 1" );
					l.add( "Slimeball" );
					single.setSeveral( l );
					q = session.createQuery( "from Simple s where s.name in (:several)", Simple.class );
					q.setProperties( single );
					assertThat( q.list().get( 0 ) ).isSameAs( simple );


					q = session.createQuery( "from Simple s where s.name in (:stuff)", Simple.class );
					q.setProperties( single );
					assertThat( q.list().get( 0 ) ).isSameAs( simple );
					session.remove( simple );
				}
		);
	}

	public void testBroken(SessionFactoryScope scope) {
		Broken broken = new Fixed();
		scope.inTransaction(
				s -> {
					broken.setId( 123L );
					broken.setOtherId( "foobar" );
					s.persist( broken );
					s.flush();
					broken.setTimestamp( new Date() );
				}
		);

		Broken b = scope.fromTransaction(
				session -> session.merge( broken )
		);

		Broken b1 = scope.fromTransaction(
				session ->
						session.getReference( Broken.class, b )
		);

		scope.inTransaction(
				session ->
						session.remove( b1 )
		);
	}

	public void testNothinToUpdate(SessionFactoryScope scope) {
		Simple s = new Simple( 10L );
		scope.inTransaction(
				session -> {
					s.setName( "Simple 1" );
					session.persist( s );
				}
		);
		Simple simple = scope.fromTransaction(
				session ->
						session.merge( s )
		);

		scope.inTransaction(
				session ->
						session.remove( session.merge( simple ) )
		);
	}

	public void testCachedQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple simple = new Simple( Long.valueOf( 10 ) );
					simple.setName( "Simple 1" );
					session.persist( simple );
				}
		);

		Simple s = scope.fromTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s where s.name=?", Simple.class );
					q.setCacheable( true );
					q.setParameter( 0, "Simple 1" );
					assertThat( q.list() ).hasSize( 1 );

					q = session.createQuery( "from Simple s where s.name=:name", Simple.class );
					q.setCacheable( true );
					q.setParameter( "name", "Simple 1" );
					assertThat( q.list() ).hasSize( 1 );
					Simple simple = q.list().get( 0 );

					q.setParameter( "name", "Simple 2" );
					assertThat( q.list() ).hasSize( 0 );
					simple.setName( "Simple 2" );
					assertThat( q.list() ).hasSize( 1 );
					return simple;

				}
		);
		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s where s.name=:name", Simple.class );
					q.setParameter( "name", "Simple 2" );
					q.setCacheable( true );
					assertThat( q.list() ).hasSize( 1 );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( session.merge( s ) )
		);
		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s where s.name=?", Simple.class );
					q.setCacheable( true );
					q.setParameter( 0, "Simple 1" );
					assertThat( q.list() ).hasSize( 0 );

				}
		);
	}

	public void testCachedQueryRegion(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple simple = new Simple( Long.valueOf( 10 ) );
					simple.setName( "Simple 1" );
					session.persist( simple );
				}
		);

		Simple s = scope.fromTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s where s.name=?", Simple.class );
					q.setCacheRegion( "foo" );
					q.setCacheable( true );
					q.setParameter( 0, "Simple 1" );
					assertThat( q.list() ).hasSize( 1 );

					q = session.createQuery( "from Simple s where s.name=:name", Simple.class );
					q.setCacheRegion( "foo" );
					q.setCacheable( true );
					q.setParameter( "name", "Simple 1" );
					assertThat( q.list() ).hasSize( 1 );
					Simple simple = q.list().get( 0 );

					q.setParameter( "name", "Simple 2" );
					assertThat( q.list() ).hasSize( 0 );
					simple.setName( "Simple 2" );
					assertThat( q.list() ).hasSize( 1 );
					return simple;
				}
		);

		scope.inTransaction(
				session ->
						session.remove( session.merge( s ) )

		);

		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s where s.name=?", Simple.class );
					q.setCacheRegion( "foo" );
					q.setCacheable( true );
					q.setParameter( 0, "Simple 1" );
					assertThat( q.list() ).hasSize( 0 );
				}
		);
	}

	public void testSQLFunctions(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Simple simple = new Simple( 10L );
					simple.setName( "Simple 1" );
					s.persist( simple );

					s.createQuery( "from Simple s where repeat('foo', 3) = 'foofoofoo'", Simple.class ).list();
					s.createQuery( "from Simple s where repeat(s.name, 3) = 'foofoofoo'", Simple.class ).list();
					s.createQuery( "from Simple s where repeat( lower(s.name), (3 + (1-1)) / 2) = 'foofoofoo'",
							Simple.class ).list();

					assertThat(
							s.createQuery( "from Simple s where upper( s.name ) ='SIMPLE 1'", Simple.class )
									.list() ).hasSize( 1 );
					assertThat(
							s.createQuery(
									"from Simple s where not( upper( s.name ) ='yada' or 1=2 or 'foo'='bar' or not('foo'='foo') or 'foo' like 'bar' )",
									Simple.class ).list() ).hasSize( 1 );

					assertThat( s.createQuery( "from Simple s where lower( s.name || ' foo' ) ='simple 1 foo'",
							Simple.class ).list() ).hasSize( 1 );
					assertThat( s.createQuery( "from Simple s where lower( concat(s.name, ' foo') ) ='simple 1 foo'",
							Simple.class ).list() ).hasSize( 1 );

					Simple other = new Simple( 20L );
					other.setName( "Simple 2" );
					other.setCount( 12 );
					simple.setOther( other );
					s.persist( other );
					//s.find("from Simple s where s.name ## 'cat|rat|bag'");
					assertThat( s.createQuery( "from Simple s where upper( s.other.name ) ='SIMPLE 2'", Simple.class )
							.list() ).hasSize( 1 );
					assertThat(
							s.createQuery(
											"from Simple s where not ( upper( s.other.name ) ='SIMPLE 2', Simple.class )",
											Simple.class )
									.list() ).hasSize( 0 );
					assertThat(
							s.createQuery(
									"select distinct s from Simple s where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2",
									Simple.class
							).list() ).hasSize( 1 );
					assertThat(
							s.createQuery(
									"select s from Simple s where ( ( s.other.count + 3 ) = (15*2)/2 and s.count = 69) or ( ( s.other.count + 2 ) / 7 ) = 2 order by s.other.count",
									Simple.class
							).list() ).hasSize( 1 );
					Simple min = new Simple( 30L );
					min.setCount( -1 );
					s.persist( min );

					assertThat(
							s.createQuery( "from Simple s where s.count > ( select min(sim.count) from Simple sim )",
											Simple.class )
									.list() ).hasSize( 2 );
					s.getTransaction().commit();
					s.beginTransaction();
					assertThat(
							s.createQuery(
									"from Simple s where s = some( select sim from Simple sim where sim.count>=0 ) and s.count >= 0",
									Simple.class
							).list() ).hasSize( 2 );
					assertThat(
							s.createQuery(
									"from Simple s where s = some( select sim from Simple sim where sim.other.count=s.other.count ) and s.other.count > 0",
									Simple.class
							).list() ).hasSize( 1 );

					List list = s.createQuery(
									"select sum(s.count) from Simple s group by s.count having sum(s.count) > 10" )
							.list();
					assertThat( list ).hasSize( 1 );
					assertThat( list.get( 0 ) ).isEqualTo( 12L );
					list = s.createQuery( "select s.count from Simple s group by s.count having s.count = 12" ).list();
					assertThat( list ).isNotEmpty();

					s.createQuery(
							"select s.id, s.count, count(t), max(t.date) from Simple s, Simple t where s.count = t.count group by s.id, s.count order by s.count"
					).list();

					Query<Simple> q = s.createQuery( "from Simple s", Simple.class );
					q.setMaxResults( 10 );
					assertThat( q.list() ).hasSize( 3 );
					q = s.createQuery( "from Simple s", Simple.class );
					q.setMaxResults( 1 );
					assertThat( q.list() ).hasSize( 1 );
					q = s.createQuery( "from Simple s", Simple.class );
					assertThat( q.list() ).hasSize( 3 );
					q = s.createQuery( "from Simple s where s.name = ?", Simple.class );
					q.setParameter( 0, "Simple 1" );
					assertThat( q.list() ).hasSize( 1 );
					q = s.createQuery( "from Simple s where s.name = ? and upper(s.name) = ?", Simple.class );
					q.setParameter( 1, "SIMPLE 1" );
					q.setParameter( 0, "Simple 1" );
					q.setFirstResult( 0 );
					assertThat( q.list() ).isNotEmpty();
					q = s.createQuery(
							"from Simple s where s.name = :foo and upper(s.name) = :bar or s.count=:count or s.count=:count + 1",
							Simple.class );
					q.setParameter( "bar", "SIMPLE 1" );
					q.setParameter( "foo", "Simple 1" );
					q.setParameter( "count", 69 );
					q.setFirstResult( 0 );
					assertThat( q.list() ).isNotEmpty();
					q = s.createQuery( "select s.id from Simple s", Simple.class );
					q.setFirstResult( 1 );
					q.setMaxResults( 2 );
					list = q.list();
					for ( Object l : list ) {
						assertThat( l ).isInstanceOf( Long.class );
					}
//		int i=0;
//		while ( list.hasNext() ) {
//			assertTrue( list.next() instanceof Long );
//			i++;
//		}
					assertThat( q.list() ).hasSize( 2 );
					q = s.createQuery( "select all s, s.other from Simple s where s = :s" );
					q.setParameter( "s", simple );
					assertThat( q.list() ).hasSize( 1 );


					q = s.createQuery( "from Simple s where s.name in (:name_list) and s.count > :count",
							Simple.class );
					HashSet<String> set = new HashSet<>();
					set.add( "Simple 1" );
					set.add( "foo" );
					q.setParameterList( "name_list", set );
					q.setParameter( "count", -1 );
					assertThat( q.list() ).hasSize( 1 );

					try (ScrollableResults<Simple> sr = s.createQuery( "from Simple s", Simple.class ).scroll()) {
						sr.next();
						sr.get();
					}

					s.remove( other );
					s.remove( simple );
					s.remove( min );
				}
		);
	}

	public void testBlobClob(SessionFactoryScope scope) {
		Blobber blobber = new Blobber();
		scope.inTransaction(
				session -> {
					blobber.setBlob( getLobHelper().createBlob( "foo/bar/baz".getBytes() ) );
					blobber.setClob( getLobHelper().createClob( "foo/bar/baz" ) );
					session.persist( blobber );
					//s.refresh(b);
					//assertTrue( b.getClob() instanceof ClobImpl );
					session.flush();
					session.refresh( blobber );
					//b.getBlob().setBytes( 2, "abc".getBytes() );
					try {
						blobber.getClob().getSubString( 2, 3 );
					}
					catch (SQLException e) {
						throw new RuntimeException( e );
					}
					//b.getClob().setString(2, "abc");
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Blobber b = session.getReference( Blobber.class, blobber.getId() );
					Blobber b2 = new Blobber();
					session.persist( b2 );
					b2.setBlob( b.getBlob() );
					b.setBlob( null );
					//assertTrue( b.getClob().getSubString(1, 3).equals("fab") );
					try {
						b.getClob().getSubString( 1, 6 );
					}
					catch (SQLException e) {
						throw new RuntimeException( e );
					}
					//b.getClob().setString(1, "qwerty");
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Blobber b = session.getReference( Blobber.class, blobber.getId() );
					b.setClob( getLobHelper().createClob( "xcvfxvc xcvbx cvbx cvbx cvbxcvbxcvbxcvb" ) );
					session.flush();
				}
		);

		scope.inTransaction(
				session -> {
					Blobber b = session.getReference( Blobber.class, blobber.getId() );
					try {
						assertThat( b.getClob().getSubString( 1, 7 ) ).isEqualTo( "xcvfxvc" );
					}
					catch (SQLException e) {
						throw new RuntimeException( e );
					}
					//b.getClob().setString(5, "1234567890");
					session.flush();
				}
		);
	}

	public void testSqlFunctionAsAlias(SessionFactoryScope scope) {
		String functionName = locateAppropriateDialectFunctionNameForAliasTest();
		String query = "select " + functionName + " from Simple as " + functionName + " where " + functionName + ".id = 10";

		scope.inTransaction(
				session -> {
					Simple simple = new Simple( 10L );
					simple.setName( "Simple 1" );
					session.persist( simple );
				}
		);

		scope.inTransaction(
				session -> {
					List result = session.createQuery( query ).list();
					assertThat( result ).hasSize( 1 );
					assertThat( result.get( 0 ) ).isInstanceOf( Simple.class );
					session.remove( result.get( 0 ) );
				}
		);
	}

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

	public void testCachedQueryOnInsert(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Simple simple = new Simple( 10L );
					simple.setName( "Simple 1" );
					session.persist( simple );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s", Simple.class );
					List<Simple> list = q.setCacheable( true ).list();
					assertThat( list ).hasSize( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s", Simple.class );
					List<Simple> list = q.setCacheable( true ).list();
					assertThat( list ).hasSize( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					Simple simple2 = new Simple( 12L );
					simple2.setCount( 133 );
					session.persist( simple2 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s", Simple.class );
					List<Simple> list = q.setCacheable( true ).list();
					assertThat( list ).hasSize( 2 );
				}
		);

		scope.inTransaction(
				session -> {
					Query<Simple> q = session.createQuery( "from Simple s", Simple.class );
					List<Simple> list = q.setCacheable( true ).list();
					assertThat( list ).hasSize( 2 );
					for ( Object o : list ) {
						session.remove( o );
					}
				}
		);

	}

	public void testInterSystemsFunctions(SessionFactoryScope scope) {
		Calendar cal = new GregorianCalendar();
		cal.set( 1977, 6, 3, 0, 0, 0 );
		java.sql.Timestamp testvalue = new java.sql.Timestamp( cal.getTimeInMillis() );
		testvalue.setNanos( 0 );
		Calendar cal3 = new GregorianCalendar();
		cal3.set( 1976, 2, 3, 0, 0, 0 );
		java.sql.Timestamp testvalue3 = new java.sql.Timestamp( cal3.getTimeInMillis() );
		testvalue3.setNanos( 0 );

		scope.inTransaction(
				session -> {
					try {
						session.doWork(
								connection -> {
									Statement stmt = session.getJdbcCoordinator()
											.getStatementPreparer()
											.createStatement();
									session.getJdbcCoordinator().getResultSetReturn()
											.executeUpdate( stmt,
													"DROP FUNCTION spLock FROM TestInterSystemsFunctionsClass" );
								}
						);
					}
					catch (Exception ex) {
						System.out.println( "as we expected stored procedure sp does not exist when we drop it" );

					}
					session.getTransaction().commit();

					session.beginTransaction();
					session.doWork(
							connection -> {
								Statement stmt = session.getJdbcCoordinator()
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
								session.getJdbcCoordinator().getResultSetReturn().executeUpdate(
										stmt,
										create_function
								);
							}
					);
					session.getTransaction().commit();

					session.beginTransaction();

					TestInterSystemsFunctionsClass object = new TestInterSystemsFunctionsClass( 10L );
					object.setDateText( "1977-07-03" );
					object.setDate1( testvalue );
					object.setDate3( testvalue3 );
					session.persist( object );
				}
		);

		scope.inTransaction(
				s2 -> {
					TestInterSystemsFunctionsClass test = s2.get( TestInterSystemsFunctionsClass.class, 10L );
					assertThat( test.getDate1() ).isEqualTo( testvalue );
					test = s2.byId( TestInterSystemsFunctionsClass.class )
							.with( LockMode.NONE )
							.load( 10L );
					assertThat( test.getDate1() ).isEqualTo( testvalue );
					Date value = s2.createQuery(
									"select nvl(o.date,o.dateText) from TestInterSystemsFunctionsClass as o", Date.class )
							.list()
							.get( 0 );
					assertThat( value ).isEqualTo( testvalue );
					Object nv = s2.createQuery(
									"select nullif(o.dateText,o.dateText) from TestInterSystemsFunctionsClass as o",
									Object.class )
							.list()
							.get( 0 );
					assertThat( nv ).isNull();
					String dateText = s2.createQuery(
									"select nvl(o.dateText,o.date) from TestInterSystemsFunctionsClass as o", String.class
							).list()
							.get( 0 );
					assertThat( dateText ).isEqualTo( "1977-07-03" );
					value = s2.createQuery(
									"select ifnull(o.date,o.date1) from TestInterSystemsFunctionsClass as o", Date.class )
							.list()
							.get( 0 );
					assertThat( value ).isEqualTo( testvalue );
					value = s2.createQuery(
									"select ifnull(o.date3,o.date,o.date1) from TestInterSystemsFunctionsClass as o",
									Date.class )
							.list()
							.get( 0 );
					assertThat( value ).isEqualTo( testvalue );
					Integer pos = s2.createQuery(
									"select position('07', o.dateText) from TestInterSystemsFunctionsClass as o",
									Integer.class )
							.list()
							.get( 0 );
					assertThat( pos ).isEqualTo( 6 );
					String st = s2.createQuery(
									"select convert(o.date1, SQL_TIME) from TestInterSystemsFunctionsClass as o", String.class )
							.list()
							.get( 0 );
					assertThat( st ).isEqualTo( "00:00:00" );
					Time tm = s2.createQuery(
									"select cast(o.date1, time) from TestInterSystemsFunctionsClass as o", Time.class
							).list()
							.get( 0 );
					assertThat( tm.toString() ).isEqualTo( "00:00:00" );
					Double diff = s2.createQuery(
									"select timestampdiff(SQL_TSI_FRAC_SECOND, o.date3, o.date1) from TestInterSystemsFunctionsClass as o",
									Double.class
							).list()
							.get( 0 );
					assertThat( diff ).isNotEqualTo( 0.0 );
					diff = s2.createQuery(
									"select timestampdiff(SQL_TSI_MONTH, o.date3, o.date1) from TestInterSystemsFunctionsClass as o",
									Double.class
							).list()
							.get( 0 );
					assertThat( diff ).isEqualTo( 16.0 );
					diff = s2.createQuery(
									"select timestampdiff(SQL_TSI_WEEK, o.date3, o.date1) from TestInterSystemsFunctionsClass as o",
									Double.class
							).list()
							.get( 0 );
					assertThat( diff ).isGreaterThanOrEqualTo( 16 * 4 );
					diff = s2.createQuery(
									"select timestampdiff(SQL_TSI_YEAR, o.date3, o.date1) from TestInterSystemsFunctionsClass as o",
									Double.class
							).list()
							.get( 0 );
					assertThat( diff ).isEqualTo( 1.0 );
				}
		);

	}

}
