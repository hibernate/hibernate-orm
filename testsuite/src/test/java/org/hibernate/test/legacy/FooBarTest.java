//$Id: FooBarTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.DriverManagerConnectionProvider;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InterbaseDialect;
import org.hibernate.dialect.MckoiDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
//import org.hibernate.dialect.Oracle9Dialect;
//import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PointbaseDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SAPDBDialect;
import org.hibernate.dialect.Sybase11Dialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TimesTenDialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
//import org.hibernate.mapping.RootClass;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.hibernate.util.JoinedIterator;
import org.hibernate.util.SerializationHelper;


public class FooBarTest extends LegacyTestCase {

	public FooBarTest(String arg) {
		super(arg);
	}

	public String[] getMappings() {
		return new String[] {
			"legacy/FooBar.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Fumm.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Immutable.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Vetoer.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/Location.hbm.xml",
			"legacy/Stuff.hbm.xml",
			"legacy/Container.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/XY.hbm.xml"
		};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( FooBarTest.class );
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

	public void testSaveOrUpdateCopyAny() throws Exception {
		Session s = openSession();
		Bar bar = new Bar();
		One one = new One();
		bar.setObject(one);
		s.save(bar);
		GlarchProxy g = bar.getComponent().getGlarch();
		bar.getComponent().setGlarch(null);
		s.delete(g);
		s.flush();
		assertTrue( s.contains(one) );
		s.connection().commit();
		s.close();

		s = openSession();
		Bar bar2 = (Bar) s.saveOrUpdateCopy(bar);
		s.flush();
		s.delete(bar2);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testRefreshProxy() throws Exception {
		Session s = openSession();
		Glarch g = new Glarch();
		Serializable gid = s.save(g);
		s.flush();
		s.clear();
		GlarchProxy gp = (GlarchProxy) s.load(Glarch.class, gid);
		gp.getName(); //force init
		s.refresh(gp);
		s.delete(gp);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testOnCascadeDelete() throws Exception {

		if ( ! supportsCircularCascadeDelete() ) {
			return;
		}

		Session s = openSession();
		Baz baz = new Baz();
		baz.subs = new ArrayList();
		Baz sub = new Baz();
		sub.superBaz = baz;
		baz.subs.add(sub);
		s.save(baz);
		s.flush();
		assertTrue( s.createQuery("from Baz").list().size()==2 );
		s.connection().commit();
		s.delete(baz);
		s.flush();
		s.connection().commit();
		assertTrue( s.createQuery("from Baz").list().size()==0 );
		s.connection().commit();
		s.close();
	}

	public void testRemoveFromIdbag() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setByteBag( new ArrayList() );
		byte[] bytes = { 12, 13 };
		baz.getByteBag().add( new byte[] { 10, 45 } );
		baz.getByteBag().add(bytes);
		baz.getByteBag().add( new byte[] { 1, 11 } );
		baz.getByteBag().add( new byte[] { 12 } );
		s.save(baz);
		s.flush();
		baz.getByteBag().remove(bytes);
		s.flush();
		baz.getByteBag().add(bytes);
		s.flush();
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testLoad() throws Exception {
		Session s = openSession();
		Qux q = new Qux();
		s.save(q);
		BarProxy b = new Bar();
		s.save(b);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		q = (Qux) s.load(Qux.class, q.getKey() );
		b = (BarProxy) s.load( Foo.class, b.getKey() );
		b.getKey();
		assertFalse( Hibernate.isInitialized(b) );
		b.getBarString();
		assertTrue( Hibernate.isInitialized(b) );
		BarProxy b2 = (BarProxy) s.load( Bar.class, new String( b.getKey() ) );
		Qux q2 = (Qux) s.load( Qux.class, q.getKey() );
		assertTrue( "loaded same object", q==q2 );
		assertTrue( "loaded same object", b==b2 );
		assertTrue( Math.round( b.getFormula() ) == b.getInt()/2 );
		s.delete(q2);
		s.delete(b2);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testJoin() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		foo.setJoinedProp("foo");
		s.save(foo);
		s.flush();
		foo.setJoinedProp("bar");
		s.flush();
		String fid = foo.getKey();
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Foo foo2 = new Foo();
		foo2.setJoinedProp("foo");
		s.save(foo2);
		s.find("select foo.id from Foo foo where foo.joinedProp = 'foo'");
		assertNull( s.get(Foo.class, fid) );
		s.delete(foo2);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testDereferenceLazyCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setFooSet( new HashSet() );
		Foo foo = new Foo();
		baz.getFooSet().add(foo);
		s.save(foo);
		s.save(baz);
		foo.setBytes( "foobar".getBytes() );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo = (Foo) s.get( Foo.class, foo.getKey() );
		assertTrue( Hibernate.isInitialized( foo.getBytes() ) );
		assertTrue( foo.getBytes().length==6 );
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertTrue( baz.getFooSet().size()==1 );
		s.flush();
		s.connection().commit();
		s.close();

		getSessions().evictCollection("org.hibernate.test.legacy.Baz.fooSet");

		s = openSession();
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertFalse( Hibernate.isInitialized( baz.getFooSet() ) );
		baz.setFooSet(null);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo = (Foo) s.get( Foo.class, foo.getKey() );
		assertTrue( foo.getBytes().length==6 );
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertFalse( Hibernate.isInitialized( baz.getFooSet() ) );
		assertTrue( baz.getFooSet().size()==0 );
		s.delete(baz);
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testMoveLazyCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Baz baz2 = new Baz();
		baz.setFooSet( new HashSet() );
		Foo foo = new Foo();
		baz.getFooSet().add(foo);
		s.save(foo);
		s.save(baz);
		s.save(baz2);
		foo.setBytes( "foobar".getBytes() );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo = (Foo) s.get( Foo.class, foo.getKey() );
		assertTrue( Hibernate.isInitialized( foo.getBytes() ) );
		assertTrue( foo.getBytes().length==6 );
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertTrue( baz.getFooSet().size()==1 );
		s.flush();
		s.connection().commit();
		s.close();

		getSessions().evictCollection("org.hibernate.test.legacy.Baz.fooSet");

		s = openSession();
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertFalse( Hibernate.isInitialized( baz.getFooSet() ) );
		baz2 = (Baz) s.get( Baz.class, baz2.getCode() );
		baz2.setFooSet( baz.getFooSet() );
		baz.setFooSet(null);
		assertFalse( Hibernate.isInitialized( baz2.getFooSet() ) );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo = (Foo) s.get( Foo.class, foo.getKey() );
		assertTrue( foo.getBytes().length==6 );
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		baz2 = (Baz) s.get( Baz.class, baz2.getCode() );
		assertFalse( Hibernate.isInitialized( baz.getFooSet() ) );
		assertTrue( baz.getFooSet().size()==0 );
		assertTrue( Hibernate.isInitialized( baz2.getFooSet() ) ); //fooSet has batching enabled
		assertTrue( baz2.getFooSet().size()==1 );
		s.delete(baz);
		s.delete(baz2);
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testCriteriaCollection() throws Exception {
		Session s = openSession();
		Baz bb = (Baz) s.createCriteria(Baz.class).uniqueResult();
		assertTrue(bb==null);
		Baz baz = new Baz();
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		Baz b = (Baz) s.createCriteria(Baz.class).uniqueResult();
		assertTrue( Hibernate.isInitialized( b.getTopGlarchez() ) );
		assertTrue( b.getTopGlarchez().size()==0 );
		s.delete(b);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testQuery() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Foo foo = new Foo();
		s.save(foo);
		Foo foo2 = new Foo();
		s.save(foo2);
		foo.setFoo(foo2);

		List list = s.find("from Foo foo inner join fetch foo.foo");
		Foo foof = (Foo) list.get(0);
		assertTrue( Hibernate.isInitialized( foof.getFoo() ) );

		list = s.find("from Baz baz left outer join fetch baz.fooToGlarch");

		list = s.find(
			"select foo, bar from Foo foo left outer join foo.foo bar where foo = ?",
			foo,
			Hibernate.entity(Foo.class)
		);
		Object[] row1 = (Object[]) list.get(0);
		assertTrue( row1[0]==foo && row1[1]==foo2 );

		s.find("select foo.foo.foo.string from Foo foo where foo.foo = 'bar'");
		s.find("select foo.foo.foo.foo.string from Foo foo where foo.foo = 'bar'");
		s.find("select foo from Foo foo where foo.foo.foo = 'bar'");
		s.find("select foo.foo.foo.foo.string from Foo foo where foo.foo.foo = 'bar'");
		s.find("select foo.foo.foo.string from Foo foo where foo.foo.foo.foo.string = 'bar'");
		if ( ! (getDialect() instanceof HSQLDialect) ) s.find("select foo.string from Foo foo where foo.foo.foo.foo = foo.foo.foo");
		s.find("select foo.string from Foo foo where foo.foo.foo = 'bar' and foo.foo.foo.foo = 'baz'");
		s.find("select foo.string from Foo foo where foo.foo.foo.foo.string = 'a' and foo.foo.string = 'b'");

		s.find("from Bar bar, foo in elements(bar.baz.fooArray)");

		//s.find("from Baz as baz where baz.topComponents[baz].name = 'bazzz'");

		if ( (getDialect() instanceof DB2Dialect) && !(getDialect() instanceof DerbyDialect) ) {
			s.find("from Foo foo where lower( foo.foo.string ) = 'foo'");
			s.find("from Foo foo where lower( (foo.foo.string || 'foo') || 'bar' ) = 'foo'");
			s.find("from Foo foo where repeat( (foo.foo.string || 'foo') || 'bar', 2 ) = 'foo'");
			s.find("from Bar foo where foo.foo.integer is not null and repeat( (foo.foo.string || 'foo') || 'bar', (5+5)/2 ) = 'foo'");
			s.find("from Bar foo where foo.foo.integer is not null or repeat( (foo.foo.string || 'foo') || 'bar', (5+5)/2 ) = 'foo'");
		}
		if (getDialect() instanceof SybaseDialect) {  
			s.iterate("select baz from Baz as baz join baz.fooArray foo group by baz order by sum(foo.float)");
		}

		s.find("from Foo as foo where foo.component.glarch.name is not null");
		s.find("from Foo as foo left outer join foo.component.glarch as glarch where glarch.name = 'foo'");

		list = s.find("from Foo");
		assertTrue( list.size()==2 && list.get(0) instanceof FooProxy );
		list = s.find("from Foo foo left outer join foo.foo");
		assertTrue( list.size()==2 && ( (Object[]) list.get(0) )[0] instanceof FooProxy );

		s.createQuery("from Bar, Bar").list();
		s.createQuery("from Foo, Bar").list();
		s.find("from Baz baz left join baz.fooToGlarch, Bar bar join bar.foo");
		s.find("from Baz baz left join baz.fooToGlarch join baz.fooSet");
		s.find("from Baz baz left join baz.fooToGlarch join fetch baz.fooSet foo left join fetch foo.foo");

		list = s.find("from Foo foo where foo.string='osama bin laden' and foo.boolean = true order by foo.string asc, foo.component.count desc");
		assertTrue( "empty query", list.size()==0 );
		Iterator iter = s.iterate("from Foo foo where foo.string='osama bin laden' order by foo.string asc, foo.component.count desc");
		assertTrue( "empty iterator", !iter.hasNext() );

		list = s.find("select foo.foo from Foo foo");
		assertTrue( "query", list.size()==1 );
		assertTrue( "returned object", list.get(0)==foo.getFoo() );
		foo.getFoo().setFoo(foo);
		foo.setString("fizard");
		//The following test is disabled for databases with no subselects...also for Interbase (not sure why).
		if ( 
				!(getDialect() instanceof MySQLDialect) && 
				!(getDialect() instanceof HSQLDialect) && 
				!(getDialect() instanceof MckoiDialect) && 
				!(getDialect() instanceof SAPDBDialect) && 
				!(getDialect() instanceof PointbaseDialect) &&
				!(getDialect() instanceof DerbyDialect)
		)  {
			// && !db.equals("weblogic") {
			if ( !( getDialect() instanceof InterbaseDialect ) ) {
				list = s.find("from Foo foo where ? = some elements(foo.component.importantDates)", new Date(), Hibernate.DATE);
				assertTrue( "component query", list.size()==2 );
			}
			if( !( getDialect() instanceof TimesTenDialect)) {
				list = s.find("from Foo foo where size(foo.component.importantDates) = 3"); //WAS: 4
				assertTrue( "component query", list.size()==2 );
				list = s.find("from Foo foo where 0 = size(foo.component.importantDates)");
				assertTrue( "component query", list.size()==0 );
			}
			list = s.find("from Foo foo where exists elements(foo.component.importantDates)");
			assertTrue( "component query", list.size()==2 );
			s.find("from Foo foo where not exists (from Bar bar where bar.id = foo.id)");

			s.find("select foo.foo from Foo foo where foo = some(select x from Foo x where x.long > foo.foo.long)");
			s.find("select foo.foo from Foo foo where foo = some(from Foo x where (x.long > foo.foo.long))");
			if ( !( getDialect() instanceof TimesTenDialect)) {
				s.find("select foo.foo from Foo foo where foo.long = some( select max(x.long) from Foo x where (x.long > foo.foo.long) group by x.foo )");
			}
			s.find("from Foo foo where foo = some(select x from Foo x where x.long > foo.foo.long) and foo.foo.string='baz'");
			s.find("from Foo foo where foo.foo.string='baz' and foo = some(select x from Foo x where x.long > foo.foo.long)");
			s.find("from Foo foo where foo = some(select x from Foo x where x.long > foo.foo.long)");

			s.iterate("select foo.string, foo.date, foo.foo.string, foo.id from Foo foo, Baz baz where foo in elements(baz.fooArray) and foo.string like 'foo'");
		}
		list = s.find("from Foo foo where foo.component.count is null order by foo.component.count");
		assertTrue( "component query", list.size()==0 );
		list = s.find("from Foo foo where foo.component.name='foo'");
		assertTrue( "component query", list.size()==2 );
		list = s.find("select distinct foo.component.name, foo.component.name from Foo foo where foo.component.name='foo'");
		assertTrue( "component query", list.size()==1 );
		list = s.find("select distinct foo.component.name, foo.id from Foo foo where foo.component.name='foo'");
		assertTrue( "component query", list.size()==2 );
		list = s.find("select foo.foo from Foo foo");
		assertTrue( "query", list.size()==2 );
		list = s.find("from Foo foo where foo.id=?", foo.getKey(), Hibernate.STRING);
		assertTrue( "id query", list.size()==1 );
		list = s.find("from Foo foo where foo.key=?", foo.getKey(), Hibernate.STRING);
		assertTrue( "named id query", list.size()==1 );
		assertTrue( "id query", list.get(0)==foo );
		list = s.find("select foo.foo from Foo foo where foo.string='fizard'");
		assertTrue( "query", list.size()==1 );
		assertTrue( "returned object", list.get(0)==foo.getFoo() );
		list = s.find("from Foo foo where foo.component.subcomponent.name='bar'");
		assertTrue( "components of components", list.size()==2 );
		list = s.find("select foo.foo from Foo foo where foo.foo.id=?", foo.getFoo().getKey(), Hibernate.STRING);
		assertTrue( "by id query", list.size()==1 );
		assertTrue( "by id returned object", list.get(0)==foo.getFoo() );

		s.find( "from Foo foo where foo.foo = ?", foo.getFoo(), Hibernate.entity(Foo.class) );

		assertTrue( !s.iterate("from Bar bar where bar.string='a string' or bar.string='a string'").hasNext() );

		iter = s.iterate(
			"select foo.component.name, elements(foo.component.importantDates) from Foo foo where foo.foo.id=?",
			foo.getFoo().getKey(),
			Hibernate.STRING
		);
		int i=0;
		while ( iter.hasNext() ) {
			i++;
			Object[] row = (Object[]) iter.next();
			assertTrue( row[0] instanceof String && ( row[1]==null || row[1] instanceof Date ) );
		}
		assertTrue(i==3); //WAS: 4
		iter = s.iterate(
			"select max( elements(foo.component.importantDates) ) from Foo foo group by foo.id"
		);
		assertTrue( iter.next() instanceof Date );

		list = s.find(
			"select foo.foo.foo.foo from Foo foo, Foo foo2 where"
			+ " foo = foo2.foo and not not ( not foo.string='fizard' )"
			+ " and foo2.string between 'a' and (foo.foo.string)"
			+ ( ( getDialect() instanceof HSQLDialect || getDialect() instanceof InterbaseDialect || getDialect() instanceof TimesTenDialect)?
				" and ( foo2.string in ( 'fiz', 'blah') or 1=1 )"
				:
				" and ( foo2.string in ( 'fiz', 'blah', foo.foo.string, foo.string, foo2.string ) )"
			)
		);
		assertTrue( "complex query", list.size()==1 );
		assertTrue( "returned object", list.get(0)==foo );
		foo.setString("from BoogieDown  -tinsel town  =!@#$^&*())");
		list = s.find("from Foo foo where foo.string='from BoogieDown  -tinsel town  =!@#$^&*())'");
		assertTrue( "single quotes", list.size()==1 );
		list = s.find("from Foo foo where not foo.string='foo''bar'");
		assertTrue( "single quotes", list.size()==2 );
		list = s.find("from Foo foo where foo.component.glarch.next is null");
		assertTrue( "query association in component", list.size()==2 );
		Bar bar = new Bar();
		Baz baz = new Baz();
		baz.setDefaults();
		bar.setBaz(baz);
		baz.setManyToAny( new ArrayList() );
		baz.getManyToAny().add(bar);
		baz.getManyToAny().add(foo);
		s.save(bar);
		s.save(baz);
		list = s.find(" from Bar bar where bar.baz.count=667 and bar.baz.count!=123 and not bar.baz.name='1-E-1'");
		assertTrue( "query many-to-one", list.size()==1 );
		list = s.find(" from Bar i where i.baz.name='Bazza'");
		assertTrue( "query many-to-one", list.size()==1 );

		Iterator rs = s.iterate("select count(distinct foo.foo) from Foo foo");
		assertTrue( "count", ( (Long) rs.next() ).longValue()==2 );
		assertTrue( !rs.hasNext() );
		rs = s.iterate("select count(foo.foo.boolean) from Foo foo");
		assertTrue( "count", ( (Long) rs.next() ).longValue()==2 );
		assertTrue( !rs.hasNext() );
		rs = s.iterate("select count(*), foo.int from Foo foo group by foo.int");
		assertTrue( "count(*) group by", ( (Object[]) rs.next() )[0].equals( new Long(3) ) );
		assertTrue( !rs.hasNext() );
		rs = s.iterate("select sum(foo.foo.int) from Foo foo");
		assertTrue( "sum", ( (Long) rs.next() ).longValue()==4 );
		assertTrue( !rs.hasNext() );
		rs = s.iterate("select count(foo) from Foo foo where foo.id=?", foo.getKey(), Hibernate.STRING);
		assertTrue( "id query count", ( (Long) rs.next() ).longValue()==1 );
		assertTrue( !rs.hasNext() );

		list = s.find( "from Foo foo where foo.boolean = ?", new Boolean(true), Hibernate.BOOLEAN );

		list = s.find("select new Foo(fo.x) from Fo fo");
		list = s.find("select new Foo(fo.integer) from Foo fo");

		list = s.createQuery("select new Foo(fo.x) from Foo fo")
			//.setComment("projection test")
			.setCacheable(true)
			.list();
		assertTrue(list.size()==3);
		list = s.createQuery("select new Foo(fo.x) from Foo fo")
			//.setComment("projection test 2")
			.setCacheable(true)
			.list();
		assertTrue(list.size()==3);

		rs = s.iterate("select new Foo(fo.x) from Foo fo");
		assertTrue( "projection iterate (results)", rs.hasNext() );
		assertTrue( "projection iterate (return check)", Foo.class.isAssignableFrom( rs.next().getClass() ) );

		ScrollableResults sr = s.createQuery("select new Foo(fo.x) from Foo fo").scroll();
		assertTrue( "projection scroll (results)", sr.next() );
		assertTrue( "projection scroll (return check)", Foo.class.isAssignableFrom( sr.get(0).getClass() ) );

		list = s.find("select foo.long, foo.component.name, foo, foo.foo from Foo foo");
		rs = list.iterator();
		int count=0;
		while ( rs.hasNext() ) {
			count++;
			Object[] row = (Object[]) rs.next();
			assertTrue( row[0] instanceof Long );
			assertTrue( row[1] instanceof String );
			assertTrue( row[2] instanceof Foo );
			assertTrue( row[3] instanceof Foo );
		}
		assertTrue(count!=0);
		list = s.find("select avg(foo.float), max(foo.component.name), count(distinct foo.id) from Foo foo");
		rs = list.iterator();
		count=0;
		while ( rs.hasNext() ) {
			count++;
			Object[] row = (Object[]) rs.next();
			assertTrue( row[0] instanceof Double );
			assertTrue( row[1] instanceof String );
			assertTrue( row[2] instanceof Long );
		}
		assertTrue(count!=0);
		list = s.find("select foo.long, foo.component, foo, foo.foo from Foo foo");
		rs = list.iterator();
		count=0;
		while ( rs.hasNext() ) {
			count++;
			Object[] row = (Object[]) rs.next();
			assertTrue( row[0] instanceof Long );
			assertTrue( row[1] instanceof FooComponent );
			assertTrue( row[2] instanceof Foo );
			assertTrue( row[3] instanceof Foo );
		}
		assertTrue(count!=0);

		s.save( new Holder("ice T") );
		s.save( new Holder("ice cube") );

		assertTrue( s.find("from java.lang.Object as o").size()==15 );
		assertTrue( s.find("from Named").size()==7 );
		assertTrue( s.find("from Named n where n.name is not null").size()==4 );
		iter = s.iterate("from Named n");
		while ( iter.hasNext() ) {
			assertTrue( iter.next() instanceof Named );
		}

		s.save( new Holder("bar") );
		iter = s.iterate("from Named n0, Named n1 where n0.name = n1.name");
		int cnt = 0;
		while ( iter.hasNext() ) {
			Object[] row = (Object[]) iter.next();
			if ( row[0]!=row[1] ) cnt++;
		}
		if ( !(getDialect() instanceof HSQLDialect) ) {
			assertTrue(cnt==2);
			assertTrue( s.find("from Named n0, Named n1 where n0.name = n1.name").size()==7 );
		}

		Query qu = s.createQuery("from Named n where n.name = :name");
		qu.getReturnTypes();
		qu.getNamedParameters();

		iter = s.iterate("from java.lang.Object");
		int c = 0;
		while ( iter.hasNext() ) {
			iter.next();
			c++;
		}
		assertTrue(c==16);

		s.iterate("select baz.code, min(baz.count) from Baz baz group by baz.code");

		iter = s.iterate("selecT baz from Baz baz where baz.stringDateMap['foo'] is not null or baz.stringDateMap['bar'] = ?", new Date(), Hibernate.DATE);
		assertFalse( iter.hasNext() );
		list = s.find("select baz from Baz baz where baz.stringDateMap['now'] is not null");
		assertTrue( list.size()==1 );
		list = s.find("select baz from Baz baz where baz.stringDateMap['now'] is not null and baz.stringDateMap['big bang'] < baz.stringDateMap['now']");
		assertTrue( list.size()==1 );
		list = s.find("select index(date) from Baz baz join baz.stringDateMap date");
		System.out.println(list);
		assertTrue( list.size()==2 );

		s.find("from Foo foo where foo.integer not between 1 and 5 and foo.string not in ('cde', 'abc') and foo.string is not null and foo.integer<=3");

		s.find("from Baz baz inner join baz.collectionComponent.nested.foos foo where foo.string is null");
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof MckoiDialect) && !(getDialect() instanceof SAPDBDialect) && !(getDialect() instanceof PointbaseDialect) )  {
			s.find("from Baz baz inner join baz.fooSet where '1' in (from baz.fooSet foo where foo.string is not null)");
			s.find("from Baz baz where 'a' in elements(baz.collectionComponent.nested.foos) and 1.0 in elements(baz.collectionComponent.nested.floats)");
			s.find("from Baz baz where 'b' in elements(baz.collectionComponent.nested.foos) and 1.0 in elements(baz.collectionComponent.nested.floats)");
		}

		s.find("from Foo foo join foo.foo where foo.foo in ('1','2','3')");
		if ( !(getDialect() instanceof HSQLDialect) ) s.find("from Foo foo left join foo.foo where foo.foo in ('1','2','3')");
		s.find("select foo.foo from Foo foo where foo.foo in ('1','2','3')");
		s.find("select foo.foo.string from Foo foo where foo.foo in ('1','2','3')");
		s.find("select foo.foo.string from Foo foo where foo.foo.string in ('1','2','3')");
		s.find("select foo.foo.long from Foo foo where foo.foo.string in ('1','2','3')");
		s.find("select count(*) from Foo foo where foo.foo.string in ('1','2','3') or foo.foo.long in (1,2,3)");
		s.find("select count(*) from Foo foo where foo.foo.string in ('1','2','3') group by foo.foo.long");

		s.find("from Foo foo1 left join foo1.foo foo2 left join foo2.foo where foo1.string is not null");
		s.find("from Foo foo1 left join foo1.foo.foo where foo1.string is not null");
		s.find("from Foo foo1 left join foo1.foo foo2 left join foo1.foo.foo foo3 where foo1.string is not null");

		s.find("select foo.formula from Foo foo where foo.formula > 0");

		int len = s.find("from Foo as foo join foo.foo as foo2 where foo2.id >'a' or foo2.id <'a'").size();
		assertTrue(len==2);

		s.delete("from Holder");
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		baz = (Baz) s.createQuery("from Baz baz left outer join fetch baz.manyToAny").uniqueResult();
		assertTrue( Hibernate.isInitialized( baz.getManyToAny() ) );
		assertTrue( baz.getManyToAny().size()==2 );
		BarProxy barp = (BarProxy) baz.getManyToAny().get(0);
		s.find("from Baz baz join baz.manyToAny");
		assertTrue( s.find("select baz from Baz baz join baz.manyToAny a where index(a) = 0").size()==1 );

		FooProxy foop = (FooProxy) s.get( Foo.class, foo.getKey() );
		assertTrue( foop == baz.getManyToAny().get(1) );

		barp.setBaz(baz);
		assertTrue( s.find("select bar from Bar bar where bar.baz.stringDateMap['now'] is not null").size()==1 );
		assertTrue( s.find("select bar from Bar bar join bar.baz b where b.stringDateMap['big bang'] < b.stringDateMap['now'] and b.stringDateMap['now'] is not null").size()==1 );
		assertTrue( s.find("select bar from Bar bar where bar.baz.stringDateMap['big bang'] < bar.baz.stringDateMap['now'] and bar.baz.stringDateMap['now'] is not null").size()==1 );

		list = s.find("select foo.string, foo.component, foo.id from Bar foo");
		assertTrue ( ( (FooComponent) ( (Object[]) list.get(0) )[1] ).getName().equals("foo") );
		list = s.find("select elements(baz.components) from Baz baz");
		assertTrue( list.size()==2 );
		list = s.find("select bc.name from Baz baz join baz.components bc");
		assertTrue( list.size()==2 );
		//list = s.find("select bc from Baz baz join baz.components bc");

		s.createQuery("from Foo foo where foo.integer < 10 order by foo.string").setMaxResults(12).list();

		s.delete(barp);
		s.delete(baz);
		s.delete( foop.getFoo() );
		s.delete(foop);
		txn.commit();
		s.close();
	}

	public void testCascadeDeleteDetached() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		List list = new ArrayList();
		list.add( new Fee() );
		baz.setFees(list);
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		s.connection().commit();
		s.close();

		assertFalse( Hibernate.isInitialized( baz.getFees() ) );

		s = openSession();
		s.delete(baz);
		s.flush();
		assertFalse( s.iterate("from Fee").hasNext() );
		s.connection().commit();
		s.close();

		s = openSession();
		baz = new Baz();
		list = new ArrayList();
		list.add( new Fee() );
		list.add( new Fee() );
		baz.setFees(list);
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		Hibernate.initialize( baz.getFees() );
		s.connection().commit();
		s.close();

		assertTrue( baz.getFees().size()==2 );

		s = openSession();
		s.delete(baz);
		s.flush();
		assertFalse( s.iterate("from Fee").hasNext() );
		s.connection().commit();
		s.close();

	}

	public void testForeignKeys() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Foo foo = new Foo();
		List bag = new ArrayList();
		bag.add(foo);
		baz.setIdFooBag(bag);
		baz.setFoo(foo);
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testNonlazyCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.createCriteria(Baz.class)
			//.setComment("criteria test")
			.setFetchMode("stringDateMap", FetchMode.EAGER)
			.uniqueResult();
		assertTrue( Hibernate.isInitialized( baz.getFooToGlarch() ) );
		assertTrue( Hibernate.isInitialized( baz.getFooComponentToFoo() ) );
		assertTrue( !Hibernate.isInitialized( baz.getStringSet() ) );
		assertTrue( Hibernate.isInitialized( baz.getStringDateMap() ) );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testReuseDeletedCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		s.flush();
		s.delete(baz);
		Baz baz2 = new Baz();
		baz2.setStringArray( new String[] {"x-y-z"} );
		s.save(baz2);
		s.flush();
		s.connection().commit();
		s.close();

		baz2.setStringSet( baz.getStringSet() );
		baz2.setStringArray( baz.getStringArray() );
		baz2.setFooArray( baz.getFooArray() );

		s = openSession();
		s.update(baz2);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		assertTrue( baz2.getStringArray().length==3 );
		assertTrue( baz2.getStringSet().size()==3 );
		s.delete(baz2);
		s.flush();
		s.connection().commit();
		s.close();


	}

	public void testPropertyRef() throws Exception {
		Session s = openSession();
		Holder h = new Holder();
		h.setName("foo");
		Holder h2 = new Holder();
		h2.setName("bar");
		h.setOtherHolder(h2);
		Serializable hid = s.save(h);
		Qux q = new Qux();
		q.setHolder(h2);
		Serializable qid = s.save(q);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		h = (Holder) s.load(Holder.class, hid);
		assertEquals( h.getName(), "foo");
		assertEquals( h.getOtherHolder().getName(), "bar");
		Object[] res = (Object[]) s.find("from Holder h join h.otherHolder oh where h.otherHolder.name = 'bar'").get(0);
		assertTrue( res[0]==h );
		q = (Qux) s.get(Qux.class, qid);
		assertTrue( q.getHolder() == h.getOtherHolder() );
		s.delete(h);
		s.delete(q);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testQueryCollectionOfValues() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		Glarch g = new Glarch();
		Serializable gid = s.save(g);

		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) /*&& !(dialect instanceof MckoiDialect)*/ && !(getDialect() instanceof SAPDBDialect) && !(getDialect() instanceof PointbaseDialect) && !(getDialect() instanceof TimesTenDialect) ) {
			s.filter( baz.getFooArray(), "where size(this.bytes) > 0");
			s.filter( baz.getFooArray(), "where 0 in elements(this.bytes)");
		}
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		//s.find("from Baz baz where baz.fooSet.string = 'foo'");
		//s.find("from Baz baz where baz.fooArray.string = 'foo'");
		//s.find("from Baz baz where baz.fooSet.foo.string = 'foo'");
		//s.find("from Baz baz join baz.fooSet.foo foo where foo.string = 'foo'");
		s.find("from Baz baz join baz.fooSet foo join foo.foo.foo foo2 where foo2.string = 'foo'");
		s.find("from Baz baz join baz.fooArray foo join foo.foo.foo foo2 where foo2.string = 'foo'");
		s.find("from Baz baz join baz.stringDateMap date where index(date) = 'foo'");
		s.find("from Baz baz join baz.topGlarchez g where index(g) = 'A'");
		s.find("select index(g) from Baz baz join baz.topGlarchez g");

		assertTrue( s.find("from Baz baz left join baz.stringSet").size()==3 );
		baz = (Baz) s.find("from Baz baz join baz.stringSet str where str='foo'").get(0);
		assertTrue( !Hibernate.isInitialized( baz.getStringSet() ) );
		baz = (Baz) s.find("from Baz baz left join fetch baz.stringSet").get(0);
		assertTrue( Hibernate.isInitialized( baz.getStringSet() ) );
		assertTrue( s.find("from Baz baz join baz.stringSet string where string='foo'").size()==1 );
		assertTrue( s.find("from Baz baz inner join baz.components comp where comp.name='foo'").size()==1 );
		//List bss = s.find("select baz, ss from Baz baz inner join baz.stringSet ss");
		s.find("from Glarch g inner join g.fooComponents comp where comp.fee is not null");
		s.find("from Glarch g inner join g.fooComponents comp join comp.fee fee where fee.count > 0");
		s.find("from Glarch g inner join g.fooComponents comp where comp.fee.count is not null");

		s.delete(baz);
		//s.delete("from Glarch g");
		s.delete( s.get(Glarch.class, gid) );
		s.flush();

		s.connection().commit();
		s.close();

	}

	public void testBatchLoad() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		SortedSet stringSet = new TreeSet();
		stringSet.add("foo");
		stringSet.add("bar");
		Set fooSet = new HashSet();
		for (int i=0; i<3; i++) {
			Foo foo = new Foo();
			s.save(foo);
			fooSet.add(foo);
		}
		baz.setFooSet(fooSet);
		baz.setStringSet(stringSet);
		s.save(baz);
		Baz baz2 = new Baz();
		fooSet = new HashSet();
		for (int i=0; i<2; i++) {
			Foo foo = new Foo();
			s.save(foo);
			fooSet.add(foo);
		}
		baz2.setFooSet(fooSet);
		s.save(baz2);
		Baz baz3 = new Baz();
		stringSet = new TreeSet();
		stringSet.add("foo");
		stringSet.add("baz");
		baz3.setStringSet(stringSet);
		s.save(baz3);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz3 = (Baz) s.load( Baz.class, baz3.getCode() );
		assertFalse( Hibernate.isInitialized(baz.getFooSet()) || Hibernate.isInitialized(baz2.getFooSet()) || Hibernate.isInitialized(baz3.getFooSet()) );
		assertFalse( Hibernate.isInitialized(baz.getStringSet()) || Hibernate.isInitialized(baz2.getStringSet()) || Hibernate.isInitialized(baz3.getStringSet()) );
		assertTrue( baz.getFooSet().size()==3 );
		assertTrue( Hibernate.isInitialized(baz.getFooSet()) && Hibernate.isInitialized(baz2.getFooSet()) && Hibernate.isInitialized(baz3.getFooSet()));
		assertTrue( baz2.getFooSet().size()==2 );
		assertTrue( baz3.getStringSet().contains("baz") );
		assertTrue( Hibernate.isInitialized(baz.getStringSet()) && Hibernate.isInitialized(baz2.getStringSet()) && Hibernate.isInitialized(baz3.getStringSet()));
		assertTrue( baz.getStringSet().size()==2 && baz2.getStringSet().size()==0 );
		s.delete(baz);
		s.delete(baz2);
		s.delete(baz3);
		Iterator iter = new JoinedIterator( new Iterator[] { baz.getFooSet().iterator(), baz2.getFooSet().iterator() } );
		while ( iter.hasNext() ) s.delete( iter.next() );
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testFetchInitializedCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Collection fooBag = new ArrayList();
		fooBag.add( new Foo() );
		fooBag.add( new Foo() );
		baz.setFooBag(fooBag);
		s.save(baz);
		s.flush();
		fooBag = baz.getFooBag();
		s.find("from Baz baz left join fetch baz.fooBag");
		assertTrue( fooBag==baz.getFooBag() );
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		Object bag = baz.getFooBag();
		assertFalse( Hibernate.isInitialized(bag) );
		s.find("from Baz baz left join fetch baz.fooBag");
		assertTrue( bag==baz.getFooBag() );
		assertTrue( baz.getFooBag().size()==2 );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testLateCollectionAdd() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		List l = new ArrayList();
		baz.setStringList(l);
		l.add("foo");
		Serializable id = s.save(baz);
		l.add("bar");
		s.flush();
		l.add("baz");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, id);
		assertTrue( baz.getStringList().size()==3 && baz.getStringList().contains("bar") );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testUpdate() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		s.save(foo);
		s.flush();
		s.connection().commit();
		s.close();

		foo = (Foo) SerializationHelper.deserialize( SerializationHelper.serialize(foo) );

		s = openSession();
		FooProxy foo2 = (FooProxy) s.load( Foo.class, foo.getKey() );
		foo2.setString("dirty");
		foo2.setBoolean( new Boolean(false) );
		foo2.setBytes( new byte[] { 1,2,3} );
		foo2.setDate(null);
		foo2.setShort( new Short("69") );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo2.setString("dirty again");
		s.update(foo2);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo2.setString("dirty again 2");
		s.update(foo2);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Foo foo3 = new Foo();
		s.load( foo3, foo.getKey() );
		// There is an interbase bug that causes null integers to return as 0, also numeric precision is <= 15
		assertTrue( "update", foo2.equalsFoo(foo3) );
		s.delete(foo3);
		s.delete("from Glarch");
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testListRemove() throws Exception {
		Session s = openSession();
		Baz b = new Baz();
		List stringList = new ArrayList();
		List feeList = new ArrayList();
		b.setFees(feeList);
		b.setStringList(stringList);
		feeList.add( new Fee() );
		feeList.add( new Fee() );
		feeList.add( new Fee() );
		feeList.add( new Fee() );
		stringList.add("foo");
		stringList.add("bar");
		stringList.add("baz");
		stringList.add("glarch");
		s.save(b);
		s.flush();
		stringList.remove(1);
		feeList.remove(1);
		s.flush();
		s.evict(b);
		s.refresh(b);
		assertTrue( b.getFees().size()==3 );
		stringList = b.getStringList();
		assertTrue(
			stringList.size()==3 &&
			"baz".equals( stringList.get(1) ) &&
			"foo".equals( stringList.get(0) )
		);
		s.delete(b);
		s.delete("from Fee");
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testFetchInitializedCollectionDupe() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Collection fooBag = new ArrayList();
		fooBag.add( new Foo() );
		fooBag.add( new Foo() );
		baz.setFooBag(fooBag);
		s.save(baz);
		s.flush();
		fooBag = baz.getFooBag();
		s.find("from Baz baz left join fetch baz.fooBag");
		assertTrue( Hibernate.isInitialized(fooBag) );
		assertTrue( fooBag==baz.getFooBag() );
		assertTrue( baz.getFooBag().size()==2 );
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		Object bag = baz.getFooBag();
		assertFalse( Hibernate.isInitialized(bag) );
		s.find("from Baz baz left join fetch baz.fooBag");
		assertTrue( Hibernate.isInitialized(bag) );
		assertTrue( bag==baz.getFooBag() );
		assertTrue( baz.getFooBag().size()==2 );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testSortables() throws Exception {
		Session s = openSession();
		Baz b = new Baz();
		b.setName("name");
		SortedSet ss = new TreeSet();
		ss.add( new Sortable("foo") );
		ss.add( new Sortable("bar") );
		ss.add( new Sortable("baz") );
		b.setSortablez(ss);
		s.save(b);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Criteria cr = s.createCriteria(Baz.class);
		cr.setFetchMode("topGlarchez", FetchMode.LAZY);
		List result = cr
			.addOrder( Order.asc("name") )
			.list();
		assertTrue( result.size()==1 );
		b = (Baz) result.get(0);
		assertTrue( b.getSortablez().size()==3 );
		assertEquals( ( (Sortable) b.getSortablez().iterator().next() ).getName(), "bar" );
		s.connection().commit();
		s.close();

		s = openSession();
		result = s.createQuery("from Baz baz left join fetch baz.sortablez order by baz.name asc")
			.list();
		b = (Baz) result.get(0);
		assertTrue( b.getSortablez().size()==3 );
		assertEquals( ( (Sortable) b.getSortablez().iterator().next() ).getName(), "bar" );
		s.connection().commit();
		s.close();

		s = openSession();
		result = s.createQuery("from Baz baz order by baz.name asc")
			.list();
		b = (Baz) result.get(0);
		assertTrue( b.getSortablez().size()==3 );
		assertEquals( ( (Sortable) b.getSortablez().iterator().next() ).getName(), "bar" );
		s.delete(b);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testFetchList() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		s.save(baz);
		Foo foo = new Foo();
		s.save(foo);
		Foo foo2 = new Foo();
		s.save(foo2);
		s.flush();
		List list = new ArrayList();
		for ( int i=0; i<5; i++ ) {
			Fee fee = new Fee();
			list.add(fee);
		}
		baz.setFees(list);
		list = s.find("from Foo foo, Baz baz left join fetch baz.fees");
		assertTrue( Hibernate.isInitialized( ( (Baz) ( (Object[]) list.get(0) )[1] ).getFees() ) );
		s.delete(foo);
		s.delete(foo2);
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testBagOneToMany() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		List list = new ArrayList();
		baz.setBazez(list);
		list.add( new Baz() );
		s.save(baz);
		s.flush();
		list.add( new Baz() );
		s.flush();
		list.add( 0, new Baz() );
		s.flush();
		s.delete( list.remove(1) );
		s.flush();
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testQueryLockMode() throws Exception {

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Bar bar = new Bar();
		s.save(bar);
		s.flush();
		bar.setString("changed");
		Baz baz = new Baz();
		baz.setFoo(bar);
		s.save(baz);
		Query q = s.createQuery("from Foo foo, Bar bar");
		if ( !(getDialect() instanceof DB2Dialect) ) {
			q.setLockMode("bar", LockMode.UPGRADE);
		}
		Object[] result = (Object[]) q.uniqueResult();
		Object b = result[0];
		assertTrue( s.getCurrentLockMode(b)==LockMode.WRITE && s.getCurrentLockMode( result[1] )==LockMode.WRITE );
		tx.commit();
		s.disconnect();

		s.reconnect();
		tx = s.beginTransaction();
		assertTrue( s.getCurrentLockMode(b)==LockMode.NONE );
		s.find("from Foo foo");
		assertTrue( s.getCurrentLockMode(b)==LockMode.NONE );
		q = s.createQuery("from Foo foo");
		q.setLockMode("foo", LockMode.READ);
		q.list();
		assertTrue( s.getCurrentLockMode(b)==LockMode.READ);
		s.evict(baz);
		tx.commit();
		s.disconnect();
		
		s.reconnect();
		tx = s.beginTransaction();
		assertTrue( s.getCurrentLockMode(b)==LockMode.NONE );
		s.delete( s.load( Baz.class, baz.getCode() ) );
		assertTrue( s.getCurrentLockMode(b)==LockMode.NONE );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		q = s.createQuery("from Foo foo, Bar bar, Bar bar2");
		if ( !(getDialect() instanceof DB2Dialect) ) {
			q.setLockMode("bar", LockMode.UPGRADE);
		}
		q.setLockMode("bar2", LockMode.READ);
		result = (Object[]) q.list().get(0);
		if ( !(getDialect() instanceof DB2Dialect) ) {
			assertTrue( s.getCurrentLockMode( result[0] )==LockMode.UPGRADE && s.getCurrentLockMode( result[1] )==LockMode.UPGRADE );
		}
		s.delete( result[0] );
		tx.commit();
		s.close();
	}

	public void testManyToManyBag() throws Exception {

		Session s = openSession();
		Baz baz = new Baz();
		Serializable id = s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, id);
		baz.getFooBag().add( new Foo() );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, id);
		assertTrue( !Hibernate.isInitialized( baz.getFooBag() ) );
		assertTrue( baz.getFooBag().size()==1 );
		if ( !(getDialect() instanceof HSQLDialect) ) assertTrue( Hibernate.isInitialized( baz.getFooBag().iterator().next() ) );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testIdBag() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		s.save(baz);
		List l = new ArrayList();
		List l2 = new ArrayList();
		baz.setIdFooBag(l);
		baz.setByteBag(l2);
		l.add( new Foo() );
		l.add( new Bar() );
		byte[] bytes = "ffo".getBytes();
		l2.add(bytes);
		l2.add( "foo".getBytes() );
		s.flush();
		l.add( new Foo() );
		l.add( new Bar() );
		l2.add( "bar".getBytes() );
		s.flush();
		s.delete( l.remove(3) );
		bytes[1]='o';
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, baz.getCode());
		assertTrue( baz.getIdFooBag().size()==3 );
		assertTrue( baz.getByteBag().size()==3 );
		bytes = "foobar".getBytes();
		Iterator iter = baz.getIdFooBag().iterator();
		while ( iter.hasNext() ) s.delete( iter.next() );
		baz.setIdFooBag(null);
		baz.getByteBag().add(bytes);
		baz.getByteBag().add(bytes);
		assertTrue( baz.getByteBag().size()==5 );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, baz.getCode());
		assertTrue( baz.getIdFooBag().size()==0 );
		assertTrue( baz.getByteBag().size()==5 );
		baz.getIdFooBag().add( new Foo() );
		iter = baz.getByteBag().iterator();
		iter.next();
		iter.remove();
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load(Baz.class, baz.getCode());
		assertTrue( baz.getIdFooBag().size()==1 );
		assertTrue( baz.getByteBag().size()==4 );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	private boolean isOuterJoinFetchingDisabled() {
		return new Integer(0).equals( ( (SessionFactoryImplementor) getSessions() ).getSettings().getMaximumFetchDepth() );
	}

	public void testForceOuterJoin() throws Exception {

		if ( isOuterJoinFetchingDisabled() ) return;

		Session s = openSession();
		Glarch g = new Glarch();
		FooComponent fc = new FooComponent();
		fc.setGlarch(g);
		FooProxy f = new Foo();
		FooProxy f2 = new Foo();
		f.setComponent(fc);
		f.setFoo(f2);
		s.save(f2);
		Serializable id = s.save(f);
		Serializable gid = s.getIdentifier( f.getComponent().getGlarch() );
		s.flush();
		s.connection().commit();
		s.close();

		getSessions().evict(Foo.class);

		s = openSession();
		f = (FooProxy) s.load(Foo.class, id);
		assertFalse( Hibernate.isInitialized(f) );
		assertTrue( Hibernate.isInitialized( f.getComponent().getGlarch() ) ); //outer-join="true"
		assertFalse( Hibernate.isInitialized( f.getFoo() ) ); //outer-join="auto"
		assertEquals( s.getIdentifier( f.getComponent().getGlarch() ), gid );
		s.delete(f);
		s.delete( f.getFoo() );
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testEmptyCollection() throws Exception {
		Session s = openSession();
		Serializable id = s.save( new Baz() );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		Baz baz = (Baz) s.load(Baz.class, id);
		Set foos = baz.getFooSet();
		assertTrue( foos.size()==0 );
		Foo foo = new Foo();
		foos.add(foo);
		s.save(foo);
		s.flush();
		s.delete(foo);
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testOneToOneGenerator() throws Exception {
		Session s = openSession();
		X x = new X();
		Y y = new Y();
		x.setY(y);
		y.setTheX(x);
		x.getXxs().add( new X.XX(x) );
		x.getXxs().add( new X.XX(x) );
		Serializable id = s.save(y);
		assertEquals( id, s.save(x) );
		s.flush();
		assertTrue( s.contains(y) && s.contains(x) );
		s.connection().commit();
		s.close();
		assertEquals( new Long(x.getId()), y.getId() );

		s = openSession();
		x = new X();
		y = new Y();
		x.setY(y);
		y.setTheX(x);
		x.getXxs().add( new X.XX(x) );
		s.save(y);
		s.flush();
		assertTrue( s.contains(y) && s.contains(x) );
		s.connection().commit();
		s.close();
		assertEquals( new Long(x.getId()), y.getId() );

		s = openSession();
		x = new X();
		y = new Y();
		x.setY(y);
		y.setTheX(x);
		x.getXxs().add( new X.XX(x) );
		x.getXxs().add( new X.XX(x) );
		id = s.save(x);
		assertEquals( id, y.getId() );
		assertEquals( id, new Long( x.getId() ) );
		s.flush();
		assertTrue( s.contains(y) && s.contains(x) );
		s.delete("from X x");
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testLimit() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		for ( int i=0; i<10; i++ ) s.save( new Foo() );
		Iterator iter = s.createQuery("from Foo foo")
			.setMaxResults(4)
			.setFirstResult(2)
			.iterate();
		int count=0;
		while ( iter.hasNext() ) {
			iter.next();
			count++;
		}
		assertTrue(count==4);
		iter = s.createQuery("select distinct foo from Foo foo")
			.setMaxResults(2)
			.setFirstResult(2)
			.list()
			.iterator();
		count=0;
		while ( iter.hasNext() ) {
			iter.next();
			count++;
		}
		assertTrue(count==2);
		iter = s.createQuery("select distinct foo from Foo foo")
		.setMaxResults(3)
		.list()
		.iterator();
		count=0;
		while ( iter.hasNext() ) {
			iter.next();
			count++;
		}
		assertTrue(count==3);
		assertTrue( s.delete("from Foo foo")==10 );
		txn.commit();
		s.close();
	}

	public void testCustom() throws Exception {
		GlarchProxy g = new Glarch();
		Multiplicity m = new Multiplicity();
		m.count = 12;
		m.glarch = (Glarch) g;
		g.setMultiple(m);
		Session s = openSession();
		Serializable gid = s.save(g);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (Glarch) s.find("from Glarch g where g.multiple.count=12").get(0);
		s.connection().commit();
		s.close();
		
		s = openSession();
		g = (Glarch) s.find("from Glarch g where g.multiple.glarch=g and g.multiple.count=12").get(0);
		assertTrue( g.getMultiple()!=null );
		assertEquals( g.getMultiple().count, 12 );
		assertSame(g.getMultiple().glarch, g);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getMultiple()!=null );
		assertEquals( g.getMultiple().count, 12 );
		assertSame(g.getMultiple().glarch, g);
		s.delete(g);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testSaveAddDelete() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Set bars = new HashSet();
		baz.setCascadingBars(bars);
		s.save(baz);
		s.flush();
		baz.getCascadingBars().add( new Bar() );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testNamedParams() throws Exception {
		Bar bar = new Bar();
		Bar bar2 = new Bar();
		bar.setName("Bar");
		bar2.setName("Bar Two");
		bar.setX(10);
		bar2.setX(1000);Baz baz = new Baz();
		baz.setCascadingBars( new HashSet() );
		baz.getCascadingBars().add(bar);
		bar.setBaz(baz);

		Session s = openSession();
		Transaction txn = s.beginTransaction();
		s.save(baz);
		s.save(bar2);

		List list = s.find("from Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like 'Bar %'");
		Object row = list.iterator().next();
		assertTrue( row instanceof Object[] && ( (Object[]) row ).length==3 );

		Query q = s.createQuery("select bar, b from Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like 'Bar%'");
		list = q.list();
		if ( !(getDialect() instanceof SAPDBDialect) ) assertTrue( list.size()==2 );

		q = s.createQuery("select bar, b from Bar bar left join bar.baz baz left join baz.cascadingBars b where ( bar.name in (:nameList) or bar.name in (:nameList) ) and bar.string = :stringVal");
		HashSet nameList = new HashSet();
		nameList.add("bar");
		nameList.add("Bar");
		nameList.add("Bar Two");
		q.setParameterList("nameList", nameList);
		q.setParameter("stringVal", "a string");
		list = q.list();
		if ( !(getDialect() instanceof SAPDBDialect) ) assertTrue( list.size()==2 );

		try {
			q.setParameterList("nameList", (Collection)null);
			fail("Should throw an queryexception when passing a null!");
		} catch (QueryException qe) {
			//should happen
		}


		if (dialectSupportsEmptyInList("HQL 'x in (:name)'  with EMPTY_LIST.")) { 
				q.setParameterList("nameList", Collections.EMPTY_LIST);
			list = q.list();
			assertTrue( list.size()==0 );
		}

		q = s.createQuery("select bar, b from Bar bar inner join bar.baz baz inner join baz.cascadingBars b where bar.name like 'Bar%'");
		Object result = q.uniqueResult();
		assertTrue( result!=null );
		q = s.createQuery("select bar, b from Bar bar left join bar.baz baz left join baz.cascadingBars b where bar.name like :name and b.name like :name");
		q.setString("name", "Bar%");
		list = q.list();
		assertTrue( list.size()==1 );


		// This test added for issue HB-297 - there is an named parameter in the Order By clause
		q = s.createQuery("select bar from Bar bar order by ((bar.x - :valueX)*(bar.x - :valueX))");
		q.setInteger("valueX", bar.getX()+1);
		list = q.list();
		assertTrue( ((Bar)list.get(0)).getX() == bar.getX());
		q.setInteger("valueX", bar2.getX()+1);
		list = q.list();
		assertTrue( ((Bar)list.get(0)).getX() == bar2.getX());

		s.delete(baz);
		s.delete(bar2);
		txn.commit();
		s.close();
	}

	public void testParameterCheck() throws HibernateException {
		Session s = openSession();
		try {
			Query q = s.createQuery("select bar from Bar as bar where bar.x > :myX");
			q.list();
			fail("Should throw QueryException for missing myX");
		}
		catch (QueryException iae) {
			// should happen
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			Query q = s.createQuery("select bar from Bar as bar where bar.x > ?");
			q.list();
			fail("Should throw QueryException for missing ?");
		}
		catch (QueryException iae) {
			// should happen
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			Query q = s.createQuery("select bar from Bar as bar where bar.x > ? or bar.short = 1 or bar.string = 'ff ? bb'");
			q.setInteger(0, 1);
			q.list();
		}
		catch (QueryException iae) {
			fail("Should not throw QueryException for missing ?");
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			Query q = s.createQuery("select bar from Bar as bar where bar.string = ' ? ' or bar.string = '?'");
			q.list();
		}
		catch (QueryException iae) {
			fail("Should not throw QueryException for ? in quotes");
		}
		finally {
			s.close();
		}

		s = openSession();
		try {
			Query q = s.createQuery("select bar from Bar as bar where bar.string = ? or bar.string = ? or bar.string = ?");
			q.setParameter(0, "bull");
			q.setParameter(2, "shit");
			q.list();
			fail("should throw exception telling me i have not set parameter 1");
		}
		catch (QueryException iae) {
			// should happen!
		}
		finally {
			s.close();
		}
	}
	public void testDyna() throws Exception {
		Session s = openSession();
		GlarchProxy g = new Glarch();
		g.setName("G");
		Serializable id = s.save(g);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, id);
		assertTrue( g.getName().equals("G") );
		assertTrue( g.getDynaBean().get("foo").equals("foo") && g.getDynaBean().get("bar").equals( new Integer(66) ) );
		assertTrue( ! (g instanceof Glarch) );
		g.getDynaBean().put("foo", "bar");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, id);
		assertTrue( g.getDynaBean().get("foo").equals("bar") && g.getDynaBean().get("bar").equals( new Integer(66) ) );
		g.setDynaBean(null);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, id);
		assertTrue( g.getDynaBean()==null );
		s.delete(g);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testFindByCriteria() throws Exception {
		if ( getDialect() instanceof DB2Dialect ) return;
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Foo f = new Foo();
		s.save(f);
		s.flush();

		List list = s.createCriteria(Foo.class)
			.add( Restrictions.eq( "integer", f.getInteger() ) )
			.add( Restrictions.eqProperty("integer", "integer") )
			.add( Restrictions.like( "string", f.getString().toUpperCase() ).ignoreCase() )
			.add( Restrictions.in( "boolean", new Boolean[] { f.getBoolean(), f.getBoolean() } ) )
			.setFetchMode("foo", FetchMode.JOIN)
			.setFetchMode("baz", FetchMode.SELECT)
			.setFetchMode("abstracts", FetchMode.JOIN)
			.list();
		assertTrue( list.size()==1 && list.get(0)==f );

		list = s.createCriteria(Foo.class).add(
				Restrictions.disjunction()
					.add( Restrictions.eq( "integer", f.getInteger() ) )
					.add( Restrictions.like( "string", f.getString() ) )
					.add( Restrictions.eq( "boolean", f.getBoolean() ) )
			)
			.add( Restrictions.isNotNull("boolean") )
			.list();
		assertTrue( list.size()==1 && list.get(0)==f );

		Foo example = new Foo();
		example.setString("a STRing");
		list = s.createCriteria(Foo.class).add(
			Example.create(example)
				.excludeZeroes()
				.ignoreCase()
				.excludeProperty("bool")
				.excludeProperty("char")
				.excludeProperty("yesno")
			)
			.list();
		assertTrue( "Example API without like did not work correctly, size was " + list.size(), list.size()==1 && list.get(0)==f );
		example.setString("rin");

		list = s.createCriteria(Foo.class).add(
			Example.create(example)
				.excludeZeroes()
				.enableLike(MatchMode.ANYWHERE)
				.excludeProperty("bool")
				.excludeProperty("char")
				.excludeProperty("yesno")
			)
			.list();
		assertTrue( "Example API without like did not work correctly, size was " + list.size(), list.size()==1 && list.get(0)==f );

		list = s.createCriteria(Foo.class)
			.add( Restrictions.or(
					Restrictions.and(
					Restrictions.eq( "integer", f.getInteger() ),
					Restrictions.like( "string", f.getString() )
				),
				Restrictions.eq( "boolean", f.getBoolean() )
			) )
			.list();
		assertTrue( list.size()==1 && list.get(0)==f );
		list = s.createCriteria(Foo.class)
			.setMaxResults(5)
			.addOrder( Order.asc("date") )
			.list();
		assertTrue( list.size()==1 && list.get(0)==f );
		if(!(getDialect() instanceof TimesTenDialect)) {
			list = s.createCriteria(Foo.class).setMaxResults(0).list();
			assertTrue( list.size()==0 );
		}
		list = s.createCriteria(Foo.class)
			.setFirstResult(1)
			.addOrder( Order.asc("date") )
			.addOrder( Order.desc("string") )
			.list();
		assertTrue( list.size()==0 );
		list = s.createCriteria(Foo.class)
			.setFetchMode("component.importantDates", FetchMode.EAGER)
			.list();
		assertTrue( list.size()==3 );

		list = s.createCriteria(Foo.class)
			.setFetchMode("component.importantDates", FetchMode.EAGER)
			.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
			.list();
		assertTrue( list.size()==1 );

		f.setFoo( new Foo() );
		s.save( f.getFoo() );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		list = s.createCriteria(Foo.class)
			.add( Restrictions.eq( "integer", f.getInteger() ) )
			.add( Restrictions.like( "string", f.getString() ) )
			.add( Restrictions.in( "boolean", new Boolean[] { f.getBoolean(), f.getBoolean() } ) )
			.add( Restrictions.isNotNull("foo") )
			.setFetchMode("foo", FetchMode.EAGER)
			.setFetchMode("baz", FetchMode.LAZY)
			.setFetchMode("component.glarch", FetchMode.LAZY)
			.setFetchMode("foo.baz", FetchMode.LAZY)
			.setFetchMode("foo.component.glarch", FetchMode.LAZY)
			.list();
		f = (Foo) list.get(0);
		assertTrue( Hibernate.isInitialized( f.getFoo() ) );
		assertTrue( !Hibernate.isInitialized( f.getComponent().getGlarch() ) );

		s.save( new Bar() );
		list = s.createCriteria(Bar.class)
			.list();
		assertTrue( list.size()==1 );
		assertTrue( s.createCriteria(Foo.class).list().size()==3 );
		s.delete( list.get(0) );

		s.delete( f.getFoo() );
		s.delete(f);
		txn.commit();
		s.close();
	}

	public void testAfterDelete() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		s.save(foo);
		s.flush();
		s.delete(foo);
		s.save(foo);
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testCollectionWhere() throws Exception {
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
		Baz baz = new Baz();
		Foo[] arr = new Foo[10];
		arr[0] = foo1;
		arr[9] = foo2;
		Session s = openSession();
		s.save(foo1);
		s.save(foo2);
		baz.setFooArray(arr);
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		assertTrue( baz.getFooArray().length==1 );
		assertTrue( s.find("from Baz baz join baz.fooArray foo").size()==1 );
		assertTrue( s.find("from Foo foo").size()==2 );
		assertTrue( s.filter( baz.getFooArray(), "" ).size()==1 );
		//assertTrue( s.delete("from java.lang.Object o")==9 );
		s.delete("from Foo foo");
		String bazid = baz.getCode();
		s.delete(baz);
		int rows=s.connection().createStatement().executeUpdate(
			"delete from fooArray where id_='" + bazid + "' and i>=8"
		);
		assertTrue(rows==1);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testComponentParent() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		BarProxy bar = new Bar();
		bar.setBarComponent( new FooComponent() );
		Baz baz = new Baz();
		baz.setComponents( new FooComponent[] { new FooComponent(), new FooComponent() } );
		s.save(bar);
		s.save(baz);
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		bar = (BarProxy) s.load(Bar.class, bar.getKey());
		s.load(baz, baz.getCode());
		assertTrue( bar.getBarComponent().getParent()==bar );
		assertTrue( baz.getComponents()[0].getBaz()==baz && baz.getComponents()[1].getBaz()==baz );
		s.delete(baz);
		s.delete(bar);
		t.commit();
		s.close();
	}

	public void testCollectionCache() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		s.load( Baz.class, baz.getCode() );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void ntestAssociationId() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Bar bar = new Bar();
		String id = (String) s.save(bar);
		MoreStuff more = new MoreStuff();
		more.setName("More Stuff");
		more.setIntId(12);
		more.setStringId("id");
		Stuff stuf = new Stuff();
		stuf.setMoreStuff(more);
		more.setStuffs( new ArrayList() );
		more.getStuffs().add(stuf);
		stuf.setFoo(bar);
		stuf.setId(1234);
		stuf.setProperty( TimeZone.getDefault() );
		s.save(more);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertTrue( s.find(
			"from Stuff as s where s.foo.id = ? and s.id.id = ? and s.moreStuff.id.intId = ? and s.moreStuff.id.stringId = ?",
			new Object[] { bar, new Long(1234), new Integer(12), "id" },
			new Type[] { Hibernate.entity(Foo.class), Hibernate.LONG, Hibernate.INTEGER, Hibernate.STRING }
		).size()==1 );
		assertTrue( s.find(
			"from Stuff as s where s.foo.id = ? and s.id.id = ? and s.moreStuff.name = ?",
			new Object[] { bar, new Long(1234), "More Stuff" },
			new Type[] { Hibernate.entity(Foo.class), Hibernate.LONG, Hibernate.STRING }
		).size()==1 );
		s.find("from Stuff as s where s.foo.string is not null");
		assertTrue(
			s.find("from Stuff as s where s.foo > '0' order by s.foo").size()==1
		);
		//s.createCriteria(Stuff.class).createCriteria("id.foo").add( Expression.isNull("foo") ).list();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		FooProxy foo = (FooProxy) s.load(Foo.class, id);
		s.load(more, more);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Stuff stuff = new Stuff();
		stuff.setFoo(foo);
		stuff.setId(1234);
		stuff.setMoreStuff(more);
		s.load(stuff, stuff);
		assertTrue( stuff.getProperty().equals( TimeZone.getDefault() ) );
		assertTrue( stuff.getMoreStuff().getName().equals("More Stuff") );
		s.delete("from MoreStuff");
		s.delete("from Foo foo");
		t.commit();
		s.close();
	}

	public void testCascadeSave() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Baz baz = new Baz();
		List list = new ArrayList();
		list.add( new Fee() );
		list.add( new Fee() );
		baz.setFees(list);
		s.save(baz);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		assertTrue( baz.getFees().size()==2 );
		s.delete(baz);
		assertTrue( !s.iterate("from Fee fee").hasNext() );
		t.commit();
		s.close();

	}

	public void testCollectionsInSelect() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Foo[] foos = new Foo[] { null, new Foo() };
		s.save( foos[1] );
		Baz baz = new Baz();
		baz.setDefaults();
		baz.setFooArray(foos);
		s.save(baz);
		Baz baz2 = new Baz();
		baz2.setDefaults();
		s.save(baz2);

		Bar bar = new Bar();
		bar.setBaz(baz);
		s.save(bar);

		List list = s.find("select new Result(foo.string, foo.long, foo.integer) from Foo foo");
		assertTrue( list.size()==2 && ( list.get(0) instanceof Result ) && ( list.get(1) instanceof Result ) );
		/*list = s.find("select new Result( baz.name, foo.long, count(elements(baz.fooArray)) ) from Baz baz join baz.fooArray foo group by baz.name, foo.long");
		assertTrue( list.size()==1 && ( list.get(0) instanceof Result ) );
		Result r = ((Result) list.get(0) );
		assertEquals( r.getName(), baz.getName() );
		assertEquals( r.getCount(), 1 );
		assertEquals( r.getAmount(), foos[1].getLong().longValue() );*/
		list = s.find("select new Result( baz.name, max(foo.long), count(foo) ) from Baz baz join baz.fooArray foo group by baz.name");
		assertTrue( list.size()==1 && ( list.get(0) instanceof Result ) );
		Result r = ((Result) list.get(0) );
		assertEquals( r.getName(), baz.getName() );
		assertEquals( r.getCount(), 1 );
		assertTrue( r.getAmount() > 696969696969696000l );


		//s.find("select max( elements(bar.baz.fooArray) ) from Bar as bar");
		//The following test is disabled for databases with no subselects...also for Interbase (not sure why).
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) /*&& !(dialect instanceof MckoiDialect)*/ && !(getDialect() instanceof SAPDBDialect) && !(getDialect() instanceof PointbaseDialect) )  {
			s.find("select count(*) from Baz as baz where 1 in indices(baz.fooArray)");
			s.find("select count(*) from Bar as bar where 'abc' in elements(bar.baz.fooArray)");
			s.find("select count(*) from Bar as bar where 1 in indices(bar.baz.fooArray)");
			if ( !(getDialect() instanceof DB2Dialect) &&  !(getDialect() instanceof Oracle8iDialect ) && !( getDialect() instanceof SybaseDialect ) && !( getDialect() instanceof Sybase11Dialect ) && !( getDialect() instanceof SybaseASE15Dialect )) {
				// SybaseAnywhereDialect supports implicit conversions from strings to ints
				s.find("select count(*) from Bar as bar, bar.component.glarch.proxyArray as g where g.id in indices(bar.baz.fooArray)");
				s.find("select max( elements(bar.baz.fooArray) ) from Bar as bar, bar.component.glarch.proxyArray as g where g.id in indices(bar.baz.fooArray)");
			}
			s.find("select count(*) from Bar as bar where '1' in (from bar.component.glarch.proxyArray g where g.name='foo')");
			s.find("select count(*) from Bar as bar where '1' in (from bar.component.glarch.proxyArray g where g.name='foo')");
			s.find("select count(*) from Bar as bar left outer join bar.component.glarch.proxyArray as pg where '1' in (from bar.component.glarch.proxyArray)");
		}

		list = s.find("from Baz baz left join baz.fooToGlarch join fetch baz.fooArray foo left join fetch foo.foo");
		assertTrue( list.size()==1 && ( (Object[]) list.get(0) ).length==2 );

		s.find("select baz.name from Bar bar inner join bar.baz baz inner join baz.fooSet foo where baz.name = bar.string");
		s.find("SELECT baz.name FROM Bar AS bar INNER JOIN bar.baz AS baz INNER JOIN baz.fooSet AS foo WHERE baz.name = bar.string");

		if ( !( getDialect() instanceof HSQLDialect ) ) s.find("select baz.name from Bar bar join bar.baz baz left outer join baz.fooSet foo where baz.name = bar.string");

		s.find("select baz.name from Bar bar join bar.baz baz join baz.fooSet foo where baz.name = bar.string");
		s.find("SELECT baz.name FROM Bar AS bar JOIN bar.baz AS baz JOIN baz.fooSet AS foo WHERE baz.name = bar.string");

		if ( !( getDialect() instanceof HSQLDialect ) ) {
			s.find("select baz.name from Bar bar left join bar.baz baz left join baz.fooSet foo where baz.name = bar.string");
			s.find("select foo.string from Bar bar left join bar.baz.fooSet foo where bar.string = foo.string");
		}

		s.find("select baz.name from Bar bar left join bar.baz baz left join baz.fooArray foo where baz.name = bar.string");
		s.find("select foo.string from Bar bar left join bar.baz.fooArray foo where bar.string = foo.string");

		s.find("select bar.string, foo.string from Bar bar inner join bar.baz as baz inner join baz.fooSet as foo where baz.name = 'name'");
		s.find("select foo from Bar bar inner join bar.baz as baz inner join baz.fooSet as foo");
		s.find("select foo from Bar bar inner join bar.baz.fooSet as foo");

		s.find("select bar.string, foo.string from Bar bar join bar.baz as baz join baz.fooSet as foo where baz.name = 'name'");
		s.find("select foo from Bar bar join bar.baz as baz join baz.fooSet as foo");
		s.find("select foo from Bar bar join bar.baz.fooSet as foo");

		assertTrue( s.find("from Bar bar join bar.baz.fooArray foo").size()==1 );

		assertTrue( s.find("from Bar bar join bar.baz.fooSet foo").size()==0 );
		assertTrue( s.find("from Bar bar join bar.baz.fooArray foo").size()==1 );

		s.delete(bar);

		if ( getDialect() instanceof DB2Dialect || getDialect() instanceof PostgreSQLDialect ) {
			s.iterate("select one from One one join one.manies many group by one order by count(many)");
			s.iterate("select one from One one join one.manies many group by one having count(many) < 5");
		}

		s.find("from One one join one.manies many where one.id = 1 and many.id = 1");
		s.iterate("select one.id, elements(one.manies) from One one");
		s.iterate("select max( elements(one.manies) ) from One one");
		s.find("select one, elements(one.manies) from One one");
		//s.iterate("select one, max( elements(one.manies) ) from One one group by one");
		Iterator iter = s.iterate("select elements(baz.fooArray) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING);
		//WAS: assertTrue( iter.next()==null && iter.next()==foos[1] && !iter.hasNext() );
		assertTrue( iter.next()==foos[1] && !iter.hasNext() );
		list = s.find("select elements(baz.fooArray) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING);
		//WAS: assertTrue( list.size()==2 );
		assertTrue( list.size()==1 );
		iter = s.iterate("select indices(baz.fooArray) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING);
		//WAS: assertTrue( iter.next().equals( new Integer(0) ) && iter.next().equals( new Integer(1) ) && !iter.hasNext() );
		assertTrue( iter.next().equals( new Integer(1) ) && !iter.hasNext() );

		//assertTrue( s.iterate("select max( elements(baz.timeArray) ) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING).next() instanceof Time );
		//assertTrue( s.iterate("select max( elements(baz.stringSet) ) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING).next().equals("foo") );
		assertTrue( s.iterate("select size(baz.stringSet) from Baz baz where baz.id=?", baz.getCode(), Hibernate.STRING).next().equals( new Integer(3) ) );
		//s.find("from One one where sum one.manies.elements =0 or 1 = min one.manies.elements");

		s.find("from Foo foo where foo.component.glarch.id is not null");

		//iter = s.iterate("select baz, max( elements(baz.timeArray) ) from Baz baz group by baz");
		//while ( iter.hasNext() ) { Object[] arr = (Object[]) iter.next(); System.out.println( arr[0] + " " + arr[1] ); }
		iter = s.iterate("select baz, size(baz.stringSet), count( distinct elements(baz.stringSet) ), max( elements(baz.stringSet) ) from Baz baz group by baz");
		while ( iter.hasNext() ) { Object[] arr = (Object[]) iter.next(); System.out.println( arr[0] + " " + arr[1] + " " + arr[2] + " " + arr[3] ); }
		
		s.delete(baz);
		s.delete(baz2);
		s.delete( foos[1] );
		t.commit();
		s.close();
	}

	public void testNewFlushing() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		s.flush();
		baz.getStringArray()[0] = "a new value";
		Iterator iter = s.iterate("from Baz baz");//no flush
		assertTrue( iter.next()==baz );
		iter = s.iterate("select elements(baz.stringArray) from Baz baz");
		boolean found = false;
		while ( iter.hasNext() ) {
			if ( iter.next().equals("a new value") ) found = true;
		}
		assertTrue(found);
		baz.setStringArray(null);
		s.iterate("from Baz baz"); //no flush
		iter = s.iterate("select elements(baz.stringArray) from Baz baz");
		assertTrue( !iter.hasNext() );
		baz.getStringList().add("1E1");
		iter = s.iterate("from Foo foo");//no flush
		assertTrue( !iter.hasNext() );
		iter = s.iterate("select elements(baz.stringList) from Baz baz");
		found = false;
		while ( iter.hasNext() ) {
			if ( iter.next().equals("1E1") ) found = true;
		}
		assertTrue(found);
		baz.getStringList().remove("1E1");
		iter = s.iterate("select elements(baz.stringArray) from Baz baz"); //no flush
		iter = s.iterate("select elements(baz.stringList) from Baz baz");
		found = false;
		while ( iter.hasNext() ) {
			if ( iter.next().equals("1E1") ) found = true;
		}
		assertTrue(!found);

		List newList = new ArrayList();
		newList.add("value");
		baz.setStringList(newList);
		iter = s.iterate("from Foo foo");//no flush
		baz.setStringList(null);
		iter = s.iterate("select elements(baz.stringList) from Baz baz");
		assertTrue( !iter.hasNext() );

		baz.setStringList(newList);
		iter = s.iterate("from Foo foo");//no flush
		iter = s.iterate("select elements(baz.stringList) from Baz baz");
		assertTrue( iter.hasNext() );

		s.delete(baz);
		txn.commit();
		s.close();
	}

	public void testPersistCollections() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		assertTrue( ( (Long) s.iterate("select count(*) from Bar").next() ).longValue()==0 );
		assertTrue( s.iterate("select count(*) from Bar b").next().equals( new Long(0) ) );
		assertFalse( s.iterate("from Glarch g").hasNext() );

		Baz baz = new Baz();
		s.save(baz);
		baz.setDefaults();
		baz.setStringArray( new String[] { "stuff" } );
		Set bars = new HashSet();
		bars.add( new Bar() );
		baz.setCascadingBars(bars);
		HashMap sgm = new HashMap();
		sgm.put( "a", new Glarch() );
		sgm.put( "b", new Glarch() );
		baz.setStringGlarchMap(sgm);
		//System.out.println( s.print(baz) );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		assertTrue( ( (Long) s.iterate("select count(*) from Bar").next() ).longValue()==1 );
		baz = (Baz) ( (Object[]) s.find("select baz, baz from Baz baz").get(0) )[1];
		assertTrue( baz.getCascadingBars().size()==1 );
		//System.out.println( s.print(baz) );
		Foo foo = new Foo();
		s.save(foo);
		Foo foo2 = new Foo() ;
		s.save(foo2);
		baz.setFooArray( new Foo[] { foo, foo, null, foo2 } );
		baz.getFooSet().add(foo);
		baz.getCustoms().add( new String[] { "new", "custom" } );
		baz.setStringArray(null);
		baz.getStringList().set(0, "new value");
		baz.setStringSet( new TreeSet() );
		Time time = new java.sql.Time(12345);
		baz.getTimeArray()[2] = time;
		//System.out.println(time);

		assertTrue( baz.getStringGlarchMap().size()==1 );

		//The following test is disabled databases with no subselects
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) && !(getDialect() instanceof PointbaseDialect) )  {
			List list = s.find("select foo from Foo foo, Baz baz where foo in elements(baz.fooArray) and 3 = some elements(baz.intArray) and 4 > all indices(baz.intArray)");
			assertTrue( "collection.elements find", list.size()==2 );
		}
		if (!(getDialect() instanceof SAPDBDialect) ) { // SAPDB doesn't like distinct with binary type
			List list = s.find("select distinct foo from Baz baz join baz.fooArray foo");
			assertTrue( "collection.elements find", list.size()==2 );
		}

		List list = s.find("select foo from Baz baz join baz.fooSet foo");
		assertTrue( "association.elements find", list.size()==1 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		assertTrue( ( (Long) s.iterate("select count(*) from Bar").next() ).longValue()==1 );
		baz = (Baz) s.find("select baz from Baz baz order by baz").get(0);
		assertTrue( "collection of custom types - added element", baz.getCustoms().size()==4 && baz.getCustoms().get(0)!=null );
		assertTrue ( "component of component in collection", baz.getComponents()[1].getSubcomponent()!=null );
		assertTrue( baz.getComponents()[1].getBaz()==baz );
		assertTrue( "set of objects", ( (FooProxy) baz.getFooSet().iterator().next() ).getKey().equals( foo.getKey() ));
		assertTrue( "collection removed", baz.getStringArray().length==0 );
		assertTrue( "changed element", baz.getStringList().get(0).equals("new value"));
		assertTrue( "replaced set", baz.getStringSet().size()==0 );
		assertTrue( "array element change", baz.getTimeArray()[2]!=null );
		assertTrue( baz.getCascadingBars().size()==1 );
		//System.out.println( s.print(baz) );
		baz.getStringSet().add("two");
		baz.getStringSet().add("one");
		baz.getBag().add("three");
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		baz = (Baz) s.find("select baz from Baz baz order by baz").get(0);
		assertTrue( baz.getStringSet().size()==2 );
		assertTrue( baz.getStringSet().first().equals("one") );
		assertTrue( baz.getStringSet().last().equals("two") );
		assertTrue( baz.getBag().size()==5 );
		baz.getStringSet().remove("two");
		baz.getBag().remove("duplicate");
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		assertTrue( ( (Long) s.iterate("select count(*) from Bar").next() ).longValue()==1 );
		baz = (Baz) s.load(Baz.class, baz.getCode());
		assertTrue( baz.getCascadingBars().size()==1 );
		Bar bar = new Bar();
		Bar bar2 = new Bar();
		s.save(bar); s.save(bar2);
		baz.setTopFoos( new HashSet() );
		baz.getTopFoos().add(bar);
		baz.getTopFoos().add(bar2);
		assertTrue( baz.getCascadingBars().size()==1 );
		baz.setTopGlarchez( new TreeMap() );
		GlarchProxy g = new Glarch();
		s.save(g);
		baz.getTopGlarchez().put( new Character('G'), g );
		HashMap map = new HashMap();
		map.put(bar, g);
		map.put(bar2, g);
		baz.setFooToGlarch(map);
		map = new HashMap();
		map.put( new FooComponent("name", 123, null, null), bar );
		map.put( new FooComponent("nameName", 12, null, null), bar );
		baz.setFooComponentToFoo(map);
		map = new HashMap();
		map.put(bar, g);
		baz.setGlarchToFoo(map);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		baz = (Baz) s.find("select baz from Baz baz order by baz").get(0);
		assertTrue( baz.getCascadingBars().size()==1 );

		Session s2 = openSession();
		Transaction txn2 = s2.beginTransaction();
		assertTrue( ( (Long) s2.iterate("select count(*) from Bar").next() ).longValue()==3 );
		Baz baz2 = (Baz) s2.find("select baz from Baz baz order by baz").get(0);
		Object o = baz2.getFooComponentToFoo().get( new FooComponent("name", 123, null, null) );
		assertTrue(
			o==baz2.getFooComponentToFoo().get( new FooComponent("nameName", 12, null, null) ) && o!=null
		);
		txn2.commit();
		s2.close();

		assertTrue( Hibernate.isInitialized( baz.getFooToGlarch() ) );
		assertTrue( baz.getTopFoos().size()==2 );
		assertTrue( baz.getTopGlarchez().size()==1 );
		assertTrue( baz.getTopFoos().iterator().next()!=null );
		assertTrue( baz.getStringSet().size()==1 );
		assertTrue( baz.getBag().size()==4 );
		assertTrue( baz.getFooToGlarch().size()==2 );
		assertTrue( baz.getFooComponentToFoo().size()==2 );
		assertTrue( baz.getGlarchToFoo().size()==1 );
		Iterator iter = baz.getFooToGlarch().keySet().iterator();
		for (int i=0; i<2; i++ ) assertTrue( iter.next() instanceof BarProxy );
		FooComponent fooComp = (FooComponent) baz.getFooComponentToFoo().keySet().iterator().next();
		assertTrue(
			( (fooComp.getCount()==123 && fooComp.getName().equals("name"))
			|| (fooComp.getCount()==12 && fooComp.getName().equals("nameName")) )
			&& ( baz.getFooComponentToFoo().get(fooComp) instanceof BarProxy )
		);
		Glarch g2 = new Glarch();
		s.save(g2);
		g = (GlarchProxy) baz.getTopGlarchez().get( new Character('G') );
		baz.getTopGlarchez().put( new Character('H'), g );
		baz.getTopGlarchez().put( new Character('G'), g2 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		baz = (Baz) s.load(Baz.class, baz.getCode());
		assertTrue( baz.getTopGlarchez().size()==2 );
		assertTrue( baz.getCascadingBars().size()==1 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		assertTrue( ( (Long) s.iterate("select count(*) from Bar").next() ).longValue()==3 );
		baz = (Baz) s.find("select baz from Baz baz order by baz").get(0);
		assertTrue( baz.getTopGlarchez().size()==2 );
		assertTrue( baz.getCascadingBars().size()==1 );
		txn.commit();

		s.disconnect();

		s2 = (Session) SerializationHelper.deserialize( SerializationHelper.serialize(s) );
		s.close();

		s2.reconnect();
		txn2 = s2.beginTransaction();
		baz = (Baz) s2.load(Baz.class, baz.getCode());
		assertTrue( ( (Long) s2.iterate("select count(*) from Bar").next() ).longValue()==3 );
		s2.delete(baz);
		s2.delete( baz.getTopGlarchez().get( new Character('G') ) );
		s2.delete( baz.getTopGlarchez().get( new Character('H') ) );
		int rows = s2.connection().createStatement().executeUpdate("update " + getDialect().openQuote() + "glarchez" + getDialect().closeQuote() + " set baz_map_id=null where baz_map_index='a'");
		assertTrue(rows==1);
		assertTrue( s2.delete("from Bar bar")==2 );
		FooProxy[] arr = baz.getFooArray();
		assertTrue( "new array of objects", arr.length==4 && arr[1].getKey().equals( foo.getKey() ) );
		for ( int i=1; i<arr.length; i++ ) {
			if ( arr[i]!=null) s2.delete(arr[i]);
		}

		s2.load( Qux.class, new Long(666) ); //nonexistent

		assertTrue( s2.delete("from Glarch g")==1 );
		txn2.commit();

		s2.disconnect();

		Session s3 = (Session) SerializationHelper.deserialize( SerializationHelper.serialize(s2) );
		s2.close();
		//s3.reconnect();
		assertTrue( s3.load( Qux.class, new Long(666) )!=null ); //nonexistent
		//s3.disconnect();
		s3.close();
	}

	public void testSaveFlush() throws Exception {
		Session s = openSession();
		Fee fee = new Fee();
		s.save( fee, "key" );
		fee.setFi("blah");
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		fee = (Fee) s.load( Fee.class, fee.getKey() );
		assertTrue( "blah".equals( fee.getFi() ) );
		assertTrue( "key".equals( fee.getKey() ) );
		s.delete(fee);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testCreateUpdate() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		s.save(foo);
		foo.setString("dirty");
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		Foo foo2 = new Foo();
		s.load( foo2, foo.getKey() );
		// There is an interbase bug that causes null integers to return as 0, also numeric precision is <= 15
		assertTrue( "create-update", foo.equalsFoo(foo2) );
		//System.out.println( s.print(foo2) );
		s.delete(foo2);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		foo = new Foo();
		s.save(foo, "assignedid");
		foo.setString("dirty");
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		s.load(foo2, "assignedid");
		// There is an interbase bug that causes null integers to return as 0, also numeric precision is <= 15
		assertTrue( "create-update", foo.equalsFoo(foo2) );
		//System.out.println( s.print(foo2) );
		s.delete(foo2);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testUpdateCollections() throws Exception {
		Session s = openSession();
		Holder baz = new Holder();
		baz.setName("123");
		Foo f1 = new Foo();
		Foo f2 = new Foo();
		Foo f3 = new Foo();
		One o = new One();
		baz.setOnes( new ArrayList() );
		baz.getOnes().add(o);
		Foo[] foos = new Foo[] { f1, null, f2 };
		baz.setFooArray(foos);
		baz.setFoos( new HashSet() );
		baz.getFoos().add(f1);
		s.save(f1);
		s.save(f2);
		s.save(f3);
		s.save(o);
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();

		baz.getOnes().set(0, null);
		baz.getOnes().add(o);
		baz.getFoos().add(f2);
		foos[0] = f3;
		foos[1] = f1;

		s = openSession();
		s.saveOrUpdate(baz);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Holder h = (Holder) s.load(Holder.class, baz.getId());
		assertTrue( h.getOnes().get(0)==null );
		assertTrue( h.getOnes().get(1)!=null );
		assertTrue( h.getFooArray()[0]!=null);
		assertTrue( h.getFooArray()[1]!=null);
		assertTrue( h.getFooArray()[2]!=null);
		assertTrue( h.getFoos().size()==2 );
		s.connection().commit();
		s.close();

		baz.getFoos().remove(f1);
		baz.getFoos().remove(f2);
		baz.getFooArray()[0]=null;
		baz.getFooArray()[0]=null;
		baz.getFooArray()[0]=null;
		s = openSession();
		s.saveOrUpdate(baz);
		s.delete("from Foo");
		baz.getOnes().remove(o);
		s.delete("from One");
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testCreate() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		s.save(foo);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		Foo foo2 = new Foo();
		s.load( foo2, foo.getKey() );
		// There is an interbase bug that causes null integers to return as 0, also numeric precision is <= 15
		assertTrue( "create", foo.equalsFoo(foo2) );
		s.delete(foo2);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testCallback() throws Exception {
		Session s = openSession();
		Qux q = new Qux("0");
		s.save(q);
		q.setChild( new Qux("1") );
		s.save( q.getChild() );
		Qux q2 = new Qux("2");
		q2.setChild( q.getChild() );
		Qux q3 = new Qux("3");
		q.getChild().setChild(q3);
		s.save(q3);
		Qux q4 = new Qux("4");
		q4.setChild(q3);
		s.save(q4);
		s.save(q2);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		List l = s.find("from Qux");
		assertTrue( "", l.size()==5);
		s.delete( l.get(0) );
		s.delete( l.get(1) );
		s.delete( l.get(2) );
		s.delete( l.get(3) );
		s.delete( l.get(4) );
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testPolymorphism() throws Exception {
		Session s = openSession();
		Bar bar = new Bar();
		s.save(bar);
		bar.setBarString("bar bar");
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		FooProxy foo = (FooProxy) s.load( Foo.class, bar.getKey() );
		assertTrue( "polymorphic", foo instanceof BarProxy );
		assertTrue( "subclass property", ( (BarProxy) foo ).getBarString().equals( bar.getBarString() ) );
		//System.out.println( s.print(foo) );
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testRemoveContains() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		s.flush();
		assertTrue( s.contains(baz) );
		s.evict(baz);
		assertFalse( s.contains(baz) );
		Baz baz2 = (Baz) s.load( Baz.class, baz.getCode() );
		assertFalse(baz==baz2);
		s.delete(baz2);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testCollectionOfSelf() throws Exception {

		Session s = openSession();
		Bar bar = new Bar();
		s.save(bar);
		bar.setAbstracts( new HashSet() );
		bar.getAbstracts().add(bar);
		Bar bar2 = new Bar();
		bar.getAbstracts().add(bar2);
		bar.setFoo(bar);
		s.save(bar2);
		s.flush();
		s.connection().commit();
		s.close();
		bar.setAbstracts(null);
		s = openSession();
		s.load( bar, bar.getKey() );
		assertTrue( "collection contains self", bar.getAbstracts().size()==2 && bar.getAbstracts().contains(bar) );
		assertTrue( "association to self", bar.getFoo()==bar );
		Iterator iter = bar.getAbstracts().iterator();
		while ( iter.hasNext() ) {
			s.delete( iter.next() );
		}
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testFind() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();

		Bar bar = new Bar();
		s.save(bar);
		bar.setBarString("bar bar");
		bar.setString("xxx");
		Foo foo = new Foo();
		s.save(foo);
		foo.setString("foo bar");
		s.save( new Foo() );
		s.save( new Bar() );
		List list1 = s.find("select foo from Foo foo where foo.string='foo bar'");
		assertTrue( "find size", list1.size()==1 );
		assertTrue( "find ==", list1.get(0)==foo );
		List list2 = s.find("from Foo foo order by foo.string, foo.date");
		assertTrue( "find size", list2.size()==4 );

		list1 = s.find("from Foo foo where foo.class='B'");
		assertTrue( "class special property", list1.size()==2);
		list1 = s.find("from Foo foo where foo.class=Bar");
		assertTrue( "class special property", list1.size()==2);
		list1 = s.find("from Foo foo where foo.class=Bar");
		list2 = s.find("select bar from Bar bar, Foo foo where bar.string = foo.string and not bar=foo");
		assertTrue( "class special property", list1.size()==2);
		assertTrue( "select from a subclass", list2.size()==1);
		Trivial t = new Trivial();
		s.save(t);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		list1 = s.find("from Foo foo where foo.string='foo bar'");
		assertTrue( "find size", list1.size()==1 );
		// There is an interbase bug that causes null integers to return as 0, also numeric precision is <= 15
		assertTrue( "find equals", ( (Foo) list1.get(0) ).equalsFoo(foo) );
		list2 = s.find("select foo from Foo foo");
		assertTrue( "find size", list2.size()==5 );
		List list3 = s.find("from Bar bar where bar.barString='bar bar'");
		assertTrue( "find size", list3.size()==1 );
		assertTrue( "find same instance", list2.contains( list1.get(0) ) && list2.contains( list2.get(0) ) );
		assertTrue( s.find("from Trivial").size()==1 );
		s.delete("from Trivial");

		list2 = s.find("from Foo foo where foo.date = ?", new java.sql.Date(123), Hibernate.DATE);
		assertTrue ( "find by date", list2.size()==4 );
		Iterator iter = list2.iterator();
		while ( iter.hasNext() ) {
			s.delete( iter.next() );
		}
		list2 = s.find("from Foo foo");
		assertTrue( "find deleted", list2.size()==0);
		txn.commit();
		s.close();
	}

	public void testDeleteRecursive() throws Exception {
		Session s = openSession();
		Foo x = new Foo();
		Foo y = new Foo();
		x.setFoo(y);
		y.setFoo(x);
		s.save(x);
		s.save(y);
		s.flush();
		s.delete(y);
		s.delete(x);
		s.flush();
		s.connection().commit();
		s.close();
	}

	/*public void testSubcollections() throws Exception {
		Session s = sessionsopenSession();
		Baz baz = new Baz();
		s.save(baz);
		baz.setDefaults();
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		Set[] setArray = baz.getSetArray();
		baz.setSetArray(null);
		baz.setAnotherSetArray(setArray);
		baz.setAnotherSetList( baz.getSetList() );
		baz.setSetList(null);
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		assertTrue( baz.getAnotherSetArray().length==2 && baz.getAnotherSetArray()[0]!=null, "subcollection moved property");
		assertTrue( baz.getSetArray()==null, "subcollection moved property");
		assertTrue( baz.getAnotherSetList().size()==4 && baz.getAnotherSetList().get(2)!=null, "subcollection moved role");
		assertTrue( baz.getSetList()==null, "subcollection moved role");
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}*/


	public void testReachability() throws Exception {
		//first for unkeyed collections
		Session s = openSession();
		Baz baz1 = new Baz();
		s.save(baz1);
		Baz baz2 = new Baz();
		s.save(baz2);
		baz1.setIntArray( new int[] {1 ,2, 3, 4} );
		baz1.setFooSet( new HashSet() );
		Foo foo = new Foo();
		s.save(foo);
		baz1.getFooSet().add(foo);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		baz2.setFooSet( baz1.getFooSet() ); baz1.setFooSet(null);
		baz2.setIntArray( baz1.getIntArray() ); baz1.setIntArray(null);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		assertTrue( "unkeyed reachability", baz2.getIntArray().length==4 );
		assertTrue( "unkeyed reachability", baz2.getFooSet().size()==1 );
		assertTrue( "unkeyed reachability", baz1.getIntArray().length==0 );
		assertTrue( "unkeyed reachability", baz1.getFooSet().size()==0 );
		//System.out.println( s.print(baz1) + s.print(baz2) );
		FooProxy fp = (FooProxy) baz2.getFooSet().iterator().next();
		s.delete(fp);
		s.delete(baz1);
		s.delete(baz2);
		s.flush();
		s.connection().commit();
		s.close();

		//now for collections of collections
		s = openSession();
		baz1 = new Baz();
		s.save(baz1);
		baz2 = new Baz();
		s.save(baz2);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		//System.out.println( s.print(baz1) + s.print(baz2) );
		//System.out.println( s.print(baz1) + s.print(baz2) );
		s.delete(baz1);
		s.delete(baz2);
		s.flush();
		s.connection().commit();
		s.close();

		//now for keyed collections
		s = openSession();
		baz1 = new Baz();
		s.save(baz1);
		baz2 = new Baz();
		s.save(baz2);
		Foo foo1 = new Foo();
		Foo foo2 = new Foo();
		s.save(foo1); s.save(foo2);
		baz1.setFooArray( new Foo[] { foo1, null, foo2 } );
		baz1.setStringDateMap( new TreeMap() );
		baz1.getStringDateMap().put("today", new Date( System.currentTimeMillis() ) );
		baz1.getStringDateMap().put("tomorrow", new Date( System.currentTimeMillis() + 86400000 ) );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		baz2.setFooArray( baz1.getFooArray() ); baz1.setFooArray(null);
		baz2.setStringDateMap( baz1.getStringDateMap() ); baz1.setStringDateMap(null);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz2 = (Baz) s.load( Baz.class, baz2.getCode() );
		baz1 = (Baz) s.load( Baz.class, baz1.getCode() );
		assertTrue( "reachability", baz2.getStringDateMap().size()==2 );
		assertTrue( "reachability", baz2.getFooArray().length==3 );
		assertTrue( "reachability", baz1.getStringDateMap().size()==0 );
		assertTrue( "reachability", baz1.getFooArray().length==0 );
		//System.out.println( s.print(baz1) + s.print(baz2) );
		assertTrue( "null element", baz2.getFooArray()[1]==null );
		assertTrue( "non-null element", baz2.getStringDateMap().get("today")!=null );
		assertTrue( "non-null element", baz2.getStringDateMap().get("tomorrow")!=null );
		assertTrue( "null element", baz2.getStringDateMap().get("foo")==null );
		s.delete( baz2.getFooArray()[0] );
		s.delete( baz2.getFooArray()[2] );
		s.delete(baz1);
		s.delete(baz2);
		s.flush();
		assertTrue( s.find("from java.lang.Object").size()==0 );
		s.connection().commit();
		s.close();
	}

	public void testPersistentLifecycle() throws Exception {
		Session s = openSession();
		Qux q = new Qux();
		s.save(q);
		q.setStuff("foo bar baz qux");
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		q = (Qux) s.load( Qux.class, q.getKey() );
		assertTrue( "lifecycle create", q.getCreated() );
		assertTrue( "lifecycle load", q.getLoaded() );
		assertTrue( "lifecycle subobject", q.getFoo()!=null );
		s.delete(q);
		assertTrue( "lifecycle delete", q.getDeleted() );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		assertTrue( "subdeletion", s.find("from Foo foo").size()==0);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testIterators() throws Exception {
		Session s = openSession();
		for ( int i=0; i<10; i++ ) {
			Qux q = new Qux();
			Object qid = s.save(q);
			assertTrue("not null", qid!=null);
		}
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Iterator iter = s.iterate("from Qux q where q.stuff is null");
		int count=0;
		while ( iter.hasNext() ) {
			Qux q = (Qux) iter.next();
			q.setStuff("foo");
			if (count==0 || count==5) iter.remove();
			count++;
		}
		assertTrue("iterate", count==10);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		assertTrue(
			"delete by query",
			s.delete("from Qux q where q.stuff=?", "foo", Hibernate.STRING)==8
		);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		iter = s.iterate("from Qux q");
		assertTrue( "empty iterator", !iter.hasNext() );
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testVersioning() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		GlarchProxy g = new Glarch();
		s.save(g);
		GlarchProxy g2 = new Glarch();
		s.save(g2);
		Serializable gid = s.getIdentifier(g);
		Serializable g2id = s.getIdentifier(g2);
		g.setName("glarch");
		txn.commit();
		s.close();

		getSessions().evict(Glarch.class);

		s = openSession();
		txn = s.beginTransaction();
		g = (GlarchProxy) s.load( Glarch.class, gid );
		s.lock(g, LockMode.UPGRADE);
		g2 = (GlarchProxy) s.load( Glarch.class, g2id );
		assertTrue( "version", g.getVersion()==1 );
		assertTrue( "version", g.getDerivedVersion()==1 );
		assertTrue( "version", g2.getVersion()==0 );
		g.setName("foo");
		assertTrue(
			"find by version",
			s.find("from Glarch g where g.version=2").size()==1
		);
		g.setName("bar");
		txn.commit();
		s.close();

		getSessions().evict(Glarch.class);

		s = openSession();
		txn = s.beginTransaction();
		g = (GlarchProxy) s.load( Glarch.class, gid );
		g2 = (GlarchProxy) s.load( Glarch.class, g2id );
		assertTrue( "version", g.getVersion()==3 );
		assertTrue( "version", g.getDerivedVersion()==3 );
		assertTrue( "version", g2.getVersion()==0 );
		g.setNext(null);
		g2.setNext(g);
		s.delete(g2);
		s.delete(g);
		txn.commit();
		s.close();
	}

	public void testVersionedCollections() throws Exception {
		Session s = openSession();
		GlarchProxy g = new Glarch();
		s.save(g);
		g.setProxyArray( new GlarchProxy[] { g } );
		String gid = (String) s.getIdentifier(g);
		ArrayList list = new ArrayList();
		list.add("foo");
		g.setStrings(list);
		HashSet set = new HashSet();
		set.add(g);
		g.setProxySet(set);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getStrings().size()==1 );
		assertTrue( g.getProxyArray().length==1 );
		assertTrue( g.getProxySet().size()==1 );
		assertTrue( "versioned collection before", g.getVersion()==1 );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getStrings().get(0).equals("foo") );
		assertTrue( g.getProxyArray()[0]==g );
		assertTrue( g.getProxySet().iterator().next()==g );
		assertTrue( "versioned collection before", g.getVersion()==1 );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( "versioned collection before", g.getVersion()==1 );
		g.getStrings().add("bar");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( "versioned collection after", g.getVersion()==2 );
		assertTrue( "versioned collection after", g.getStrings().size()==2 );
		g.setProxyArray(null);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( "versioned collection after", g.getVersion()==3 );
		assertTrue( "versioned collection after", g.getProxyArray().length==0 );
		g.setFooComponents( new ArrayList() );
		g.setProxyArray(null);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( "versioned collection after", g.getVersion()==4 );
		s.delete(g);
		s.flush();
		assertTrue( s.find("from java.lang.Object").size()==0 );
		s.connection().commit();
		s.close();
	}

	/*public void testVersionedSubcollections() throws Exception {
		Session s = sessionsopenSession();

		assertTrue( !s.iterate("from Fee fee").hasNext() );

		GlarchProxy g = new Glarch();
		s.save(g);
		String gid = (String) s.getIdentifier(g);
		HashMap map = new HashMap();
		HashSet subSet  = new HashSet();
		map.put("xxx", subSet);
		subSet.add("foo"); subSet.add("bar");
		g.setStringSets(map);
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getVersion()==1, "versioned collection before" );
		//System.out.println( g.getStringSets().get("xxx") );
		assertTrue( ( (Set) g.getStringSets().get("xxx") ).size()==2, "versioned collection before" );
		( (Set) g.getStringSets().get("xxx") ).add("baz");
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getVersion()==2, "versioned collection after" );
		//System.out.println( g.getStringSets().get("xxx") );
		assertTrue( ( (Set) g.getStringSets().get("xxx") ).size()==3, "versioned collection after" );
		s.delete(g);
		s.flush();
		s.connection().commit();
		s.close();

		//with components! (note: this also tests some stuff testPersistCollections misses)
		s = sessionsopenSession();
		g = new Glarch(); //(GlarchProxy) s.create(Glarch.class);
		List list = new ArrayList();
		Date[] dates = new Date[] { null, null, new Date(), new Date(0) };
		list.add(null);
		list.add( new FooComponent("foo", 69, dates, new FooComponent("bar", 96, null, null, new Fee() ) ) );
		g.setFooComponents(list);
		String EIGHT_CHARS = "abcdefgh";
		s.save(g, EIGHT_CHARS + EIGHT_CHARS + EIGHT_CHARS + EIGHT_CHARS);
		gid = (String) s.getIdentifier(g);
		assertTrue( s.iterate("from Fee fee").hasNext() );
		g.getFooComponents().add( new FooComponent("bar", 96, null, null) );
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getVersion()==2, "versioned collection before" );
		( (FooComponent) g.getFooComponents().get(1) ).getImportantDates()[0] = new Date(123567890);
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getVersion()==3, "versioned collection after" );
		( (FooComponent) g.getFooComponents().get(1) ).getSubcomponent().setName("new name");
		assertTrue( ( (FooComponent) g.getFooComponents().get(1) ).getImportantDates()[0]!=null, "versioned collection after" );
		s.flush();
		s.connection().commit();
		s.close();
		s = sessionsopenSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( ( (FooComponent) g.getFooComponents().get(1) ).getSubcomponent().getName().equals("new name"), "versioned collection after" );
		assertTrue( g.getVersion()==4, "versioned collection after" );
		s.delete(g);
		assertTrue( !s.iterate("from Fee fee").hasNext() );
		s.flush();
		s.connection().commit();
		s.close();


	}*/

	public void testRecursiveLoad() throws Exception {
		//Non polymorphic class (there is an implementation optimization
		//being tested here)
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		GlarchProxy last = new Glarch();
		s.save(last);
		last.setOrder( (short) 0 );
		for (int i=0; i<5; i++) {
			GlarchProxy next = new Glarch();
			s.save(next);
			last.setNext(next);
			last = next;
			last.setOrder( (short) (i+1) );
		}
		Iterator iter = s.iterate("from Glarch g");
		while ( iter.hasNext() ) {
			iter.next();
		}
		List list = s.find("from Glarch g");
		assertTrue( "recursive find", list.size()==6 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		list = s.find("from Glarch g");
		assertTrue( "recursive iter", list.size()==6 );
		list = s.find("from Glarch g where g.next is not null");
		assertTrue( "recursive iter", list.size()==5 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		iter = s.iterate("from Glarch g order by g.order asc");
		while ( iter.hasNext() ) {
			GlarchProxy g = (GlarchProxy) iter.next();
			assertTrue( "not null", g!=null );
			iter.remove();
		}
		txn.commit();
		s.close();

		//Same thing but using polymorphic class (no optimisation possible):
		s = openSession();
		txn = s.beginTransaction();
		FooProxy flast = new Bar();
		s.save(flast);
		flast.setString( "foo0" );
		for (int i=0; i<5; i++) {
			FooProxy foo = new Bar();
			s.save(foo);
			flast.setFoo(foo);
			flast = flast.getFoo();
			flast.setString( "foo" + (i+1) );
		}
		iter = s.iterate("from Foo foo");
		while ( iter.hasNext() ) {
			iter.next();
		}
		list = s.find("from Foo foo");
		assertTrue( "recursive find", list.size()==6 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		list = s.find("from Foo foo");
		assertTrue( "recursive iter", list.size()==6 );
		iter = list.iterator();
		while ( iter.hasNext() ) {
			assertTrue( "polymorphic recursive load", iter.next() instanceof BarProxy );
		}
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		iter = s.iterate("from Foo foo order by foo.string asc");
		while ( iter.hasNext() ) {
			BarProxy bar = (BarProxy) iter.next();
			assertTrue( "not null", bar!=null );
			iter.remove();
		}
		txn.commit();
		s.close();
	}

	public void testScrollableIterator() throws Exception {
		// skip if not one of these named dialects
		boolean match = getDialect() instanceof DB2Dialect
				|| getDialect() instanceof SybaseDialect
				|| getDialect() instanceof HSQLDialect
				|| getDialect() instanceof Oracle8iDialect // 9i/10g too because of inheritence...
				;
		if ( ! match ) {
			return;
		}

		Session s = openSession();
		Transaction txn = s.beginTransaction();
		s.save( new Foo() );
		s.save( new Foo() );
		s.save( new Foo() );
		s.save( new Bar() );
		Query query = s.createQuery("select f, f.integer from Foo f");
		assertTrue( query.getReturnTypes().length==2 );
		ScrollableResults iter = query.scroll();
		assertTrue( iter.next() );
		assertTrue( iter.scroll(1) );
		FooProxy f2 = (FooProxy) iter.get()[0];
		assertTrue( f2!=null );
		assertTrue( iter.scroll(-1) );
		Object f1 = iter.get(0);
		iter.next();
		assertTrue( f1!=null && iter.get(0)==f2 );
		iter.getInteger(1);

		assertTrue( !iter.scroll(100) );
		assertTrue( iter.first() );
		assertTrue( iter.scroll(3) );
		Object f4 = iter.get(0);
		assertTrue( f4!=null );
		assertTrue( !iter.next() );
		assertTrue( iter.first() );
		assertTrue( iter.get(0)==f1 );
		assertTrue( iter.last() );
		assertTrue( iter.get(0)==f4 );
		assertTrue( iter.previous() );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		query = s.createQuery("select f, f.integer from Foo f");
		assertTrue( query.getReturnTypes().length==2 );
		iter = query.scroll();
		assertTrue( iter.next() );
		assertTrue( iter.scroll(1) );
		f2 = (FooProxy) iter.get()[0];
		assertTrue( f2!=null );
		assertTrue( f2.getString()!=null  && f2.getComponent().getImportantDates().length > 0 );
		assertTrue( iter.scroll(-1) );
		f1 = iter.get(0);
		iter.next();
		assertTrue( f1!=null && iter.get(0)==f2 );
		iter.getInteger(1);

		assertTrue( !iter.scroll(100) );
		assertTrue( iter.first() );
		assertTrue( iter.scroll(3) );
		f4 = iter.get(0);
		assertTrue( f4!=null );
		assertTrue( !iter.next() );
		assertTrue( iter.first() );
		assertTrue( iter.get(0)==f1 );
		assertTrue( iter.last() );
		assertTrue( iter.get(0)==f4 );
		assertTrue( iter.previous() );
		assertTrue( s.delete("from Foo")==4 );
		s.flush();
		assertTrue( s.find("from java.lang.Object").size()==0 );
		txn.commit();
		s.close();
	}

	public void testMultiColumnQueries() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Foo foo = new Foo();
		s.save(foo);
		Foo foo1 = new Foo();
		s.save(foo1);
		foo.setFoo(foo1);
		List l = s.find("select parent, child from Foo parent, Foo child where parent.foo = child");
		assertTrue( "multi-column find", l.size()==1 );

		Iterator rs = s.iterate("select count(distinct child.id), count(distinct parent.id) from Foo parent, Foo child where parent.foo = child");
		Object[] row = (Object[]) rs.next();
		assertTrue( "multi-column count", ( (Long) row[0] ).intValue()==1 );
		assertTrue( "multi-column count", ( (Long) row[1] ).intValue()==1 );
		assertTrue( !rs.hasNext() );

		rs = s.iterate("select child.id, parent.id, child.long from Foo parent, Foo child where parent.foo = child");
		row = (Object[]) rs.next();
		assertTrue( "multi-column id", row[0].equals( foo.getFoo().getKey() ) );
		assertTrue( "multi-column id", row[1].equals( foo.getKey() ) );
		assertTrue( "multi-column property", row[2].equals( foo.getFoo().getLong() ) );
		assertTrue( !rs.hasNext() );

		rs = s.iterate("select child.id, parent.id, child.long, child, parent.foo from Foo parent, Foo child where parent.foo = child");
		row = (Object[]) rs.next();
		assertTrue(
			foo.getFoo().getKey().equals( row[0] ) &&
			foo.getKey().equals( row[1] ) &&
			foo.getFoo().getLong().equals( row[2] ) &&
			row[3] == foo.getFoo() &&
			row[3]==row[4]
		);
		assertTrue( !rs.hasNext() );

		row = (Object[]) l.get(0);
		assertTrue( "multi-column find", row[0]==foo && row[1]==foo.getFoo() );
		txn.commit();
		s.close();
		
		s = openSession();
		txn = s.beginTransaction();
		Iterator iter = s.iterate("select parent, child from Foo parent, Foo child where parent.foo = child and parent.string='a string'");
		int deletions=0;
		while ( iter.hasNext() ) {
			Object[] pnc = (Object[]) iter.next();
			s.delete( pnc[0] );
			s.delete( pnc[1] );
			deletions++;
		}
		assertTrue("multi-column iterate", deletions==1);
		txn.commit();
		s.close();
	}

	public void testDeleteTransient() throws Exception {
		Fee fee = new Fee();
		Fee fee2 = new Fee();
		fee2.setAnotherFee(fee);
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save(fee);
		s.save(fee2);
		s.flush();
		fee.setCount(123);
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		s.delete(fee);
		s.delete(fee2);
		//foo.setAnotherFee(null);
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		assertTrue( s.find("from Fee fee").size()==0 );
		tx.commit();
		s.close();
	}

	public void testDeleteUpdatedTransient() throws Exception {
		Fee fee = new Fee();
		Fee fee2 = new Fee();
		fee2.setAnotherFee(fee);
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save(fee);
		s.save(fee2);
		s.flush();
		fee.setCount(123);
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		s.update(fee);
		//fee2.setAnotherFee(null);
		s.update(fee2);
		s.delete(fee);
		s.delete(fee2);
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		assertTrue( s.find("from Fee fee").size()==0 );
		tx.commit();
		s.close();
	}

	public void testUpdateOrder() throws Exception {
		Session s = openSession();
		Fee fee1 = new Fee();
		s.save(fee1);
		Fee fee2 = new Fee();
		fee1.setFee(fee2);
		fee2.setFee(fee1);
		fee2.setFees( new HashSet() );
		Fee fee3 = new Fee();
		fee3.setFee(fee1);
		fee3.setAnotherFee(fee2);
		fee2.setAnotherFee(fee3);
		s.save(fee3);
		s.save(fee2);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		fee1.setCount(10);
		fee2.setCount(20);
		fee3.setCount(30);
		s.update(fee1);
		s.update(fee2);
		s.update(fee3);
		s.flush();
		s.delete(fee1);
		s.delete(fee2);
		s.delete(fee3);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Transaction tx = s.beginTransaction();
		assertTrue( s.find("from Fee fee").size()==0 );
		tx.commit();
		s.close();
	}

	public void testUpdateFromTransient() throws Exception {
		Session s = openSession();
		Fee fee1 = new Fee();
		s.save(fee1);
		Fee fee2 = new Fee();
		fee1.setFee(fee2);
		fee2.setFee(fee1);
		fee2.setFees( new HashSet() );
		Fee fee3 = new Fee();
		fee3.setFee(fee1);
		fee3.setAnotherFee(fee2);
		fee2.setAnotherFee(fee3);
		s.save(fee3);
		s.save(fee2);
		s.flush();
		s.connection().commit();
		s.close();

		fee1.setFi("changed");
		s = openSession();
		s.saveOrUpdate(fee1);
		s.flush();
		s.connection().commit();
		s.close();

		Qux q = new Qux("quxxy");
		q.setTheKey(0);
		fee1.setQux(q);
		s = openSession();
		s.saveOrUpdate(fee1);
		s.flush();
		s.connection().commit();
		s.close();


		s = openSession();
		fee1 = (Fee) s.load( Fee.class, fee1.getKey() );
		assertTrue( "updated from transient", fee1.getFi().equals("changed") );
		assertTrue( "unsaved value", fee1.getQux()!=null );
		s.delete( fee1.getQux() );
		fee1.setQux(null);
		s.flush();
		s.connection().commit();
		s.close();

		fee2.setFi("CHANGED");
		fee2.getFees().add("an element");
		fee1.setFi("changed again");
		s = openSession();
		s.saveOrUpdate(fee2);
		s.update( fee1, fee1.getKey() );
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Fee fee = new Fee();
		s.load( fee, fee2.getKey() );
		fee1 = (Fee) s.load( Fee.class, fee1.getKey() );
		assertTrue( "updated from transient", fee1.getFi().equals("changed again") );
		assertTrue( "updated from transient", fee.getFi().equals("CHANGED") );
		assertTrue( "updated collection", fee.getFees().contains("an element") );
		s.flush();
		s.connection().commit();
		s.close();

		fee.getFees().clear();
		fee.getFees().add("new element");
		fee1.setFee(null);
		s = openSession();
		s.saveOrUpdate(fee);
		s.saveOrUpdate(fee1);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		s.load( fee, fee.getKey() );
		assertTrue( "update", fee.getAnotherFee()!=null );
		assertTrue( "update", fee.getFee()!=null );
		assertTrue( "update", fee.getAnotherFee().getFee()==fee.getFee() );
		assertTrue( "updated collection", fee.getFees().contains("new element") );
		assertTrue( "updated collection", !fee.getFees().contains("an element") );
		s.flush();
		s.connection().commit();
		s.close();

		fee.setQux( new Qux("quxy") );
		s = openSession();
		s.saveOrUpdate(fee);
		s.flush();
		s.connection().commit();
		s.close();

		fee.getQux().setStuff("xxx");
		s = openSession();
		s.saveOrUpdate(fee);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		s.load( fee, fee.getKey() );
		assertTrue( "cascade update", fee.getQux()!=null );
		assertTrue( "cascade update", fee.getQux().getStuff().equals("xxx") );
		assertTrue( "update", fee.getAnotherFee()!=null );
		assertTrue( "update", fee.getFee()!=null );
		assertTrue( "update", fee.getAnotherFee().getFee()==fee.getFee() );
		fee.getAnotherFee().setAnotherFee(null);
		s.delete(fee);
		s.delete("from Fee fee");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Transaction tx = s.beginTransaction();
		assertTrue( s.find("from Fee fee").size()==0 );
		tx.commit();
		s.close();
	}

	public void testArraysOfTimes() throws Exception {
		Session s = openSession();
		Baz baz = new Baz() ;
		s.save(baz);
		baz.setDefaults();
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();

		baz.getTimeArray()[2] = new Date(123);
		baz.getTimeArray()[3] = new java.sql.Time(1234);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testComponents() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Foo foo = new Foo();
		foo.setComponent( new FooComponent("foo", 69, null, new FooComponent("bar", 96, null, null) ) );
		s.save(foo);
		foo.getComponent().setName("IFA");
		txn.commit();
		s.close();

		foo.setComponent(null);

		s = openSession();
		txn = s.beginTransaction();
		s.load( foo, foo.getKey() );
		assertTrue(
			"save components",
			foo.getComponent().getName().equals("IFA") &&
			foo.getComponent().getSubcomponent().getName().equals("bar")
		);
		assertTrue( "cascade save via component", foo.getComponent().getGlarch()!=null);
		foo.getComponent().getSubcomponent().setName("baz");
		txn.commit();
		s.close();

		foo.setComponent(null);

		s = openSession();
		txn = s.beginTransaction();
		s.load( foo, foo.getKey() );
		assertTrue(
			"update components",
			foo.getComponent().getName().equals("IFA") &&
			foo.getComponent().getSubcomponent().getName().equals("baz")
		);
		s.delete(foo);
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		foo = new Foo();
		s.save(foo);
		foo.setCustom( new String[] { "one", "two" } );
		assertTrue( s.find("from Foo foo where foo.custom.s1 = 'one'").get(0)==foo );
		s.delete(foo);
		txn.commit();
		s.close();

	}

	public void testNoForeignKeyViolations() throws Exception {
		Session s = openSession();
		Glarch g1 = new Glarch();
		Glarch g2 = new Glarch();
		g1.setNext(g2);
		g2.setNext(g1);
		s.save(g1);
		s.save(g2);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		List l = s.find("from Glarch g where g.next is not null");
		s.delete( l.get(0) );
		s.delete( l.get(1) );
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testLazyCollections() throws Exception {
		Session s = openSession();
		Qux q = new Qux();
		s.save(q);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		q = (Qux) s.load( Qux.class, q.getKey() );
		s.flush();
		s.connection().commit();
		s.close();

		System.out.println("Two exceptions are supposed to occur:");
		boolean ok = false;
		try {
			q.getMoreFums().isEmpty();
		}
		catch (LazyInitializationException e) {
			ok = true;
		}
		assertTrue( "lazy collection with one-to-many", ok );

		ok = false;
		try {
			q.getFums().isEmpty();
		}
		catch (LazyInitializationException e) {
			ok = true;
		}
		assertTrue( "lazy collection with many-to-many", ok );

		s = openSession();
		q = (Qux) s.load( Qux.class, q.getKey() );
		s.delete(q);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testNewSessionLifecycle() throws Exception {
		Session s = openSession();
		Serializable fid = null;
		try {
			Foo f = new Foo();
			s.save(f);
			fid = s.getIdentifier(f);
			s.flush();
			s.connection().commit();
		}
		catch (Exception e) {
			s.connection().rollback();
			throw e;
		}
		finally {
			s.close();
		}
		s = openSession();
		try {
			Foo f = new Foo();
			s.delete(f);
			s.flush();
			s.connection().commit();
		}
		catch (Exception e) {
			s.connection().rollback();
		}
		finally {
			s.close();
		}
		s = openSession();
		try {
			Foo f = (Foo) s.load(Foo.class, fid, LockMode.UPGRADE);
			s.delete(f);
			s.flush();
			s.connection().commit();
		}
		catch (Exception e) {
			s.connection().rollback();
			throw e;
		}
		finally {
			assertTrue( s.close()==null );
		}
	}

	public void testDisconnect() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		Foo foo2 = new Foo();
		s.save(foo);
		s.save(foo2);
		foo2.setFoo(foo);
		s.flush();
		s.connection().commit();
		s.disconnect();
		s.reconnect();
		s.delete(foo);
		foo2.setFoo(null);
		s.flush();
		s.connection().commit();
		s.disconnect();
		s.reconnect();
		s.delete(foo2);
		s.flush();
		s.connection().commit();
		s.close();
	}



	public void testOrderBy() throws Exception {

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Foo foo = new Foo();
		s.save(foo);
		List list = s.find("select foo from Foo foo, Fee fee where foo.dependent = fee order by foo.string desc, foo.component.count asc, fee.id");
		assertTrue( "order by", list.size()==1 );
		Foo foo2 = new Foo();
		s.save(foo2);
		foo.setFoo(foo2);
		list = s.find("select foo.foo, foo.dependent from Foo foo order by foo.foo.string desc, foo.component.count asc, foo.dependent.id");
		assertTrue( "order by", list.size()==1 );
		list = s.find("select foo from Foo foo order by foo.dependent.id, foo.dependent.fi");
		assertTrue( "order by", list.size()==2 );
		s.delete(foo);
		s.delete(foo2);
		tx.commit();
		s.close();

		s = openSession();
		Many manyB = new Many();
		s.save(manyB);
		One oneB = new One();
		s.save(oneB);
		oneB.setValue("b");
		manyB.setOne(oneB);
		Many manyA = new Many();
		s.save(manyA);
		One oneA = new One();
		s.save(oneA);
		oneA.setValue("a");
		manyA.setOne(oneA);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		Iterator it = s.iterate(
			"SELECT one FROM " +
			One.class.getName() +
			" one ORDER BY one.value ASC"
		);
		int count = 0;
		while ( it.hasNext() ) {
			One one = (One)it.next();
			switch (count) {
				case 0:
				assertTrue("ordering failed", "a".equals(one.getValue()));
				break;
				case 1:
				assertTrue("ordering failed", "b".equals(one.getValue()));
				break;
				default:
				assertTrue("more than two elements", false);
				break;
			}
			count ++;
		}
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		it = s.iterate(
			"SELECT many.one FROM " +
			Many.class.getName() +
			" many ORDER BY many.one.value ASC, many.one.id"
		);
		count = 0;
		while ( it.hasNext() ) {
			One one = (One)it.next();
			switch (count) {
				case 0:
				assertTrue("'a' isn't first element", "a".equals(one.getValue()));
				break;
				case 1:
				assertTrue("'b' isn't second element", "b".equals(one.getValue()));
				break;
				default:
				assertTrue("more than two elements", false);
				break;
			}
			count ++;
		}
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		oneA = (One)s.load(One.class, oneA.getKey());
		manyA = (Many)s.load(Many.class, manyA.getKey());
		oneB = (One)s.load(One.class, oneB.getKey());
		manyB = (Many)s.load(Many.class, manyB.getKey());
		s.delete(manyA);
		s.delete(oneA);
		s.delete(manyB);
		s.delete(oneB);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testManyToOne() throws Exception {
		Session s = openSession();
		One one = new One();
		s.save(one);
		one.setValue("yada");
		Many many = new Many();
		many.setOne(one);
		s.save(many);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		one = (One) s.load( One.class, one.getKey() );
		one.getManies().size();
		s.connection().commit();
		s.close();


		s = openSession();
		many = (Many) s.load( Many.class, many.getKey() );
		assertTrue( "many-to-one assoc", many.getOne()!=null );
		s.delete( many.getOne() );
		s.delete(many);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testSaveDelete() throws Exception {
		Session s = openSession();
		Foo f = new Foo();
		s.save(f);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		s.delete( s.load( Foo.class, f.getKey() ) );
		s.flush();
		s.connection().commit();
		s.close();
	}

	/*public void testIdNotFound() throws Exception {
		// The following test is only enabled for MySQL which has no foreign key constraints.
		// I disabled this test cos it didn't clean up after itself so other tests failed
		if (db.equals("mysql")) {
			Session s = sessionsopenSession();
			Glarch g = (Glarch) s.create(Glarch.class);
			Glarch g2 = (Glarch) s.create(Glarch.class);
			g.setNext(g2);
			Serializable gid = s.getID(g);
			Serializable g2id = s.getID(g2);
			s.commit();

			s = sessionsopenSession();
			g2 = (Glarch) s.load( Glarch.class, g2id );
			s.delete(g2);
			s.commit();

			s = sessionsopenSession();
			boolean ok = false;
			try {
				g = (Glarch) s.load( Glarch.class, gid );
			}
			catch (HibernateException e) {
				ok = "id not found or provided object was wrong class".equals(e.getMessage());
			}
			catch (java.lang.StackOverflowError soe) {
				ok = false;
			}
			assertTrue( ok, "id not found");
			s.cancel();
		}
	}*/

	public void testProxyArray() throws Exception {
		Session s = openSession();
		GlarchProxy g = new Glarch();
		Glarch g1 = new Glarch();
		Glarch g2 = new Glarch();
		g.setProxyArray( new GlarchProxy[] { g1, g2 } );
		Glarch g3 = new Glarch();
		s.save(g3);
		g2.setProxyArray( new GlarchProxy[] {null, g3, g} );
		Set set = new HashSet();
		set.add(g1);
		set.add(g2);
		g.setProxySet(set);
		s.save(g);
		s.save(g1);
		s.save(g2);
		Serializable id = s.getIdentifier(g);
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, id);
		assertTrue( "array of proxies", g.getProxyArray().length==2 );
		assertTrue( "array of proxies", g.getProxyArray()[0]!=null );
		assertTrue("deferred load test",g.getProxyArray()[1].getProxyArray()[0]==null );
		assertTrue("deferred load test",g.getProxyArray()[1].getProxyArray()[2]==g );
		assertTrue( "set of proxies", g.getProxySet().size()==2 );
		Iterator iter = s.iterate("from Glarch g");
		while ( iter.hasNext() ) {
			iter.next();
			iter.remove();
		}

		s.flush();
		s.connection().commit();
		s.disconnect();
		SerializationHelper.deserialize( SerializationHelper.serialize(s) );
		s.close();
	}

	public void testCache() throws Exception {
		Session s = openSession();
		Immutable im = new Immutable();
		s.save(im);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		s.load( im, im.getId() );
		s.connection().commit();
		s.close();
		s = openSession();
		s.load( im, im.getId() );
		assertTrue(
			"cached object identity",
			s.find(
				"from Immutable im where im = ?",
				im,
				Hibernate.entity(Immutable.class)
			).get(0)==im &&
			im == s.load( Immutable.class, im.getId() )
		);
		s.connection().createStatement().executeUpdate("delete from immut");
		s.connection().commit();
		s.close();
	}

	public void testFindLoad() throws Exception {
		Session s = openSession();
		FooProxy foo = new Foo();
		s.save(foo);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		foo = (FooProxy) s.find("from Foo foo").get(0);
		FooProxy foo2 = (FooProxy) s.load( Foo.class, foo.getKey() );
		assertTrue("find returns same object as load", foo==foo2);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		foo2 = (FooProxy) s.load( Foo.class, foo.getKey() );
		foo = (FooProxy) s.find("from Foo foo").get(0);
		assertTrue("find returns same object as load", foo==foo2);
		s.delete("from Foo foo");
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testRefresh() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		s.save(foo);
		s.flush();
		s.connection().createStatement().executeUpdate("update "+getDialect().openQuote()+"foos"+getDialect().closeQuote()+" set long_ = -3");
		s.refresh(foo);
		assertTrue( foo.getLong().longValue()==-3l );
		assertTrue( s.getCurrentLockMode(foo)==LockMode.READ );
		s.refresh(foo, LockMode.UPGRADE);
		if ( getDialect().supportsOuterJoinForUpdate() ) {
			assertTrue( s.getCurrentLockMode(foo)==LockMode.UPGRADE );
		}
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testAutoFlush() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		FooProxy foo = new Foo();
		s.save(foo);
		assertTrue( "autoflush create", s.find("from Foo foo").size()==1 );
		foo.setChar( new Character('X') );
		assertTrue( "autoflush update", s.find("from Foo foo where foo.char='X'").size()==1 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		foo = (FooProxy) s.load( Foo.class, foo.getKey() );
		//s.update( new Foo(), foo.getKey() );
		//assertTrue( s.find("from Foo foo where not foo.char='X'").size()==1, "autoflush update" );
		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) && !(getDialect() instanceof PointbaseDialect) )  {
			foo.setBytes( "osama".getBytes() );
			assertTrue( "autoflush collection update", s.find("from Foo foo where 111 in elements(foo.bytes)").size()==1 );
			foo.getBytes()[0] = 69;
			assertTrue( "autoflush collection update", s.find("from Foo foo where 69 in elements(foo.bytes)").size()==1 );
		}
		s.delete(foo);
		assertTrue( "autoflush delete", s.find("from Foo foo").size()==0 );
		txn.commit();
		s.close();
	}

	public void testVeto() throws Exception {
		Session s = openSession();
		Vetoer v = new Vetoer();
		s.save(v); Serializable id = s.save(v);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		s.update(v, id); s.update(v, id);
		s.delete(v); s.delete(v);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testSerializableType() throws Exception {
		Session s = openSession();
		Vetoer v = new Vetoer();
		v.setStrings( new String[] { "foo", "bar", "baz" } );
		s.save(v); Serializable id = s.save(v);
		v.getStrings()[1] = "osama";
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		v = (Vetoer) s.load(Vetoer.class, id);
		assertTrue( "serializable type", v.getStrings()[1].equals("osama") );
		s.delete(v); s.delete(v);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testAutoFlushCollections() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		baz = (Baz) s.load(Baz.class, baz.getCode());
		baz.getStringArray()[0] = "bark";
		Iterator i = s.iterate("select elements(baz.stringArray) from Baz baz");
		boolean found = false;
		while ( i.hasNext() ) {
			if ( "bark".equals( i.next() ) ) found = true;
		}
		assertTrue(found);
		baz.setStringArray(null);
		i = s.iterate("select distinct elements(baz.stringArray) from Baz baz");
		assertTrue( !i.hasNext() );
		baz.setStringArray( new String[] { "foo", "bar" } );
		i = s.iterate("select elements(baz.stringArray) from Baz baz");
		assertTrue( i.hasNext() );

		Foo foo = new Foo();
		s.save(foo);
		s.flush();
		baz.setFooArray( new Foo[] {foo} );

		i = s.iterate("select foo from Baz baz join baz.fooArray foo");
		found = false;
		while ( i.hasNext() ) {
			if ( foo==i.next() ) found = true;
		}
		assertTrue(found);

		baz.getFooArray()[0] = null;
		i = s.iterate("select foo from Baz baz join baz.fooArray foo");
		assertTrue( !i.hasNext() );
		baz.getFooArray()[0] = foo;
		i = s.iterate("select elements(baz.fooArray) from Baz baz");
		assertTrue( i.hasNext() );

		if ( !(getDialect() instanceof MySQLDialect) && !(getDialect() instanceof HSQLDialect) && !(getDialect() instanceof InterbaseDialect) && !(getDialect() instanceof PointbaseDialect) && !(getDialect() instanceof SAPDBDialect) )  {
			baz.getFooArray()[0] = null;
			i = s.iterate(
				"from Baz baz where ? in elements(baz.fooArray)",
				foo, Hibernate.entity(Foo.class)
			);
			assertTrue( !i.hasNext() );
			baz.getFooArray()[0] = foo;
			i = s.iterate(
				"select foo from Foo foo where foo in "
				+ "(select elt from Baz baz join baz.fooArray elt)"
			);
			assertTrue( i.hasNext() );
		}
		s.delete(foo);
		s.delete(baz);
		tx.commit();
		s.close();

	}

	public void testUserProvidedConnection() throws Exception {
		ConnectionProvider dcp = new DriverManagerConnectionProvider();
		dcp.configure( Environment.getProperties() );
		Session s = getSessions().openSession( dcp.getConnection() );
		Transaction tx = s.beginTransaction();
		s.find("from Fo");
		tx.commit();
		Connection c = s.disconnect();
		assertTrue( c!=null );
		s.reconnect(c);
		tx = s.beginTransaction();
		s.find("from Fo");
		tx.commit();
		assertTrue( s.close()==c );
		c.close();
	}

	public void testCachedCollection() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		baz.setDefaults();
		s.save(baz);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		( (FooComponent) baz.getTopComponents().get(0) ).setCount(99);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		assertTrue( ( (FooComponent) baz.getTopComponents().get(0) ).getCount()==99 );
		s.delete(baz);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testComplicatedQuery() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		Foo foo = new Foo();
		Serializable id = s.save(foo);
		assertTrue(id!=null);
		Qux q = new Qux("q");
		foo.getDependent().setQux(q);
		s.save(q);
		q.getFoo().setString("foo2");
		//s.flush();
		//s.connection().commit();
		assertTrue(
			s.iterate("from Foo foo where foo.dependent.qux.foo.string = 'foo2'").hasNext()
		);
		s.delete(foo);
		txn.commit();
		s.close();
	}

	public void testLoadAfterDelete() throws Exception {
		Session s = openSession();
		Foo foo = new Foo();
		Serializable id = s.save(foo);
		s.flush();
		s.delete(foo);
		boolean err=false;
		try {
			s.load(Foo.class, id);
		}
		catch (ObjectNotFoundException ode) {
			err=true;
		}
		assertTrue(err);
		s.flush();
		err=false;
		try {
			( (FooProxy) s.load(Foo.class, id) ).getBool();
		}
		catch (ObjectNotFoundException onfe) {
			err=true;
		}
		assertTrue(err);
		Fo fo = Fo.newFo();
		id = new FumTest("").fumKey("abc"); //yuck!!
		s.save(fo, id);
		s.flush();
		s.delete(fo);
		err=false;
		try {
			s.load(Fo.class, id);
		}
		catch (ObjectNotFoundException ode) {
			err=true;
		}
		assertTrue(err);
		s.flush();
		err=false;
		try {
			s.load(Fo.class, id);
		}
		catch (ObjectNotFoundException onfe) {
			err=true;
		}
		assertTrue(err);
		s.connection().commit();
		s.close();
	}

	public void testObjectType() throws Exception {
		Session s = openSession();
		GlarchProxy g = new Glarch();
		Foo foo = new Foo();
		g.setAny(foo);
		Serializable gid = s.save(g);
		s.save(foo);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		g = (GlarchProxy) s.load(Glarch.class, gid);
		assertTrue( g.getAny()!=null && g.getAny() instanceof FooProxy );
		s.delete( g.getAny() );
		s.delete(g);
		//s.delete( g.getAny() );
		s.flush();
		s.connection().commit();
		s.close();
	}


	public void testAny() throws Exception {
		Session s = openSession();
		One one = new One();
		BarProxy foo = new Bar();
		foo.setObject(one);
		//Serializable oid = s.save(one);
		Serializable fid = s.save(foo);
		Serializable oid = one.getKey();
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		assertTrue( s.find(
			"from Bar bar where bar.object.id = ? and bar.object.class = ?",
			new Object[] { oid, new Character('O') },
			new Type[] { Hibernate.LONG, Hibernate.CHARACTER }
		).size()==1 );
		assertTrue( s.find(
			"select one from One one, Bar bar where bar.object.id = one.id and bar.object.class = 'O'"
		).size()==1 );
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		foo = (BarProxy) s.load(Foo.class, fid);
		assertTrue( foo.getObject()!=null && foo.getObject() instanceof One && s.getIdentifier( foo.getObject() ).equals(oid) );
		//s.delete( foo.getObject() );
		s.delete(foo);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testEmbeddedCompositeID() throws Exception {
		Session s = openSession();
		Location l = new Location();
		l.setCountryCode("AU");
		l.setDescription("foo bar");
		l.setLocale( Locale.getDefault() );
		l.setStreetName("Brunswick Rd");
		l.setStreetNumber(300);
		l.setCity("Melbourne");
		s.save(l);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		s.setFlushMode(FlushMode.MANUAL);
		l = (Location) s.find("from Location l where l.countryCode = 'AU' and l.description='foo bar'").get(0);
		assertTrue( l.getCountryCode().equals("AU") );
		assertTrue( l.getCity().equals("Melbourne") );
		assertTrue( l.getLocale().equals( Locale.getDefault() ) );
		assertTrue( s.createCriteria(Location.class).add( Restrictions.eq( "streetNumber", new Integer(300) ) ).list().size()==1 );
		s.connection().commit();
		s.close();

		s = openSession();
		l.setDescription("sick're");
		s.update(l);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		l = new Location();
		l.setCountryCode("AU");
		l.setDescription("foo bar");
		l.setLocale(Locale.ENGLISH);
		l.setStreetName("Brunswick Rd");
		l.setStreetNumber(300);
		l.setCity("Melbourne");
		assertTrue( l==s.load(Location.class, l) );
		assertTrue( l.getLocale().equals( Locale.getDefault() ) );
		s.delete(l);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testAutosaveChildren() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Baz baz = new Baz();
		Set bars = new HashSet();
		baz.setCascadingBars(bars);
		s.save(baz);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		baz.getCascadingBars().add( new Bar() );
		baz.getCascadingBars().add( new Bar() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		assertTrue( baz.getCascadingBars().size()==2 );
		assertTrue( baz.getCascadingBars().iterator().next()!=null );
		baz.getCascadingBars().clear(); //test all-delete-orphan;
		s.flush();
		assertTrue( s.find("from Bar bar").size()==0 );
		s.delete(baz);
		t.commit();
		s.close();
	}

	public void testOrphanDelete() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Baz baz = new Baz();
		Set bars = new HashSet();
		baz.setCascadingBars(bars);
		bars.add( new Bar() );
		bars.add( new Bar() );
		bars.add( new Bar() );
		bars.add( new Bar() );
		s.save(baz);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		bars = baz.getCascadingBars();
		assertEquals( 4, bars.size() );
		bars.remove( bars.iterator().next() );
		assertEquals( 3, s.find("From Bar bar").size() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.load( Baz.class, baz.getCode() );
		bars = baz.getCascadingBars();
		assertEquals( 3, bars.size() );
		bars.remove( bars.iterator().next() );
		s.delete(baz);
		bars.remove( bars.iterator().next() );
		assertEquals( 0, s.find("From Bar bar").size() );
		t.commit();
		s.close();

	}

	public void testTransientOrphanDelete() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Baz baz = new Baz();
		Set bars = new HashSet();
		baz.setCascadingBars(bars);
		bars.add( new Bar() );
		bars.add( new Bar() );
		bars.add( new Bar() );
		List foos = new ArrayList();
		foos.add( new Foo() );
		foos.add( new Foo() );
		baz.setFooBag(foos);
		s.save(baz);
		Iterator i = new JoinedIterator( new Iterator[] {foos.iterator(), bars.iterator()} );
		while ( i.hasNext() ) {
			FooComponent cmp = ( (Foo) i.next() ).getComponent();
			s.delete( cmp.getGlarch() );
			cmp.setGlarch(null);
		}
		t.commit();
		s.close();

		bars.remove( bars.iterator().next() );
		foos.remove(1);
		s = openSession();
		t = s.beginTransaction();
		s.update(baz);
		assertEquals( 2, s.find("From Bar bar").size() );
		assertEquals( 3, s.find("From Foo foo").size() );
		t.commit();
		s.close();

		foos.remove(0);
		s = openSession();
		t = s.beginTransaction();
		s.update(baz);
		bars.remove( bars.iterator().next() );
		assertEquals( 1, s.find("From Foo foo").size() );
		s.delete(baz);
		//s.flush();
		assertEquals( 0, s.find("From Foo foo").size() );
		t.commit();
		s.close();

	}

	public void testProxiesInCollections() throws Exception {
		Session s = openSession();
		Baz baz = new Baz();
		Bar bar = new Bar();
		Bar bar2 = new Bar();
		s.save(bar);
		Serializable bar2id = s.save(bar2);
		baz.setFooArray( new Foo[] { bar, bar2 } );
		HashSet set = new HashSet();
		bar = new Bar();
		s.save(bar);
		set.add(bar);
		baz.setFooSet(set);
		set = new HashSet();
		set.add( new Bar() );
		set.add( new Bar() );
		baz.setCascadingBars(set);
		ArrayList list = new ArrayList();
		list.add( new Foo() );
		baz.setFooBag(list);
		Serializable id = s.save(baz);
		Serializable bid = ( (Bar) baz.getCascadingBars().iterator().next() ).getKey();
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		BarProxy barprox = (BarProxy) s.load(Bar.class, bid);
		BarProxy bar2prox = (BarProxy) s.load(Bar.class, bar2id);
		assertTrue(bar2prox instanceof HibernateProxy);
		assertTrue(barprox instanceof HibernateProxy);
		baz = (Baz) s.load(Baz.class, id);
		Iterator i = baz.getCascadingBars().iterator();
		BarProxy b1 = (BarProxy) i.next();
		BarProxy b2 = (BarProxy) i.next();
		assertTrue( ( b1==barprox && !(b2 instanceof HibernateProxy) ) || ( b2==barprox && !(b1 instanceof HibernateProxy) ) ); //one-to-many
		assertTrue( baz.getFooArray()[0] instanceof HibernateProxy ); //many-to-many
		assertTrue( baz.getFooArray()[1]==bar2prox );
		if ( !isOuterJoinFetchingDisabled() ) assertTrue( !(baz.getFooBag().iterator().next() instanceof HibernateProxy) ); //many-to-many outer-join="true"
		assertTrue( !(baz.getFooSet().iterator().next() instanceof HibernateProxy) ); //one-to-many
		s.delete("from Baz");
		s.delete("from Foo");
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testPSCache() throws Exception {
		Session s = openSession();
		Transaction txn = s.beginTransaction();
		for ( int i=0; i<10; i++ ) s.save( new Foo() );
		Query q = s.createQuery("from Foo");
		q.setMaxResults(2);
		q.setFirstResult(5);
		assertTrue( q.list().size()==2 );
		q = s.createQuery("from Foo");
		assertTrue( q.list().size()==10 );
		assertTrue( q.list().size()==10 );
		q.setMaxResults(3);
		q.setFirstResult(3);
		assertTrue( q.list().size()==3 );
		q = s.createQuery("from Foo");
		assertTrue( q.list().size()==10 );
		txn.commit();
		s.close();

		s = openSession();
		txn = s.beginTransaction();
		q = s.createQuery("from Foo");
		assertTrue( q.list().size()==10 );
		q.setMaxResults(5);
		assertTrue( q.list().size()==5 );
		s.delete("from Foo");
		txn.commit();
		s.close();

	}

	public void testForCertain() throws Exception {
		Glarch g = new Glarch();
		Glarch g2 = new Glarch();
		List set = new ArrayList();
		set.add("foo");
		g2.setStrings(set);
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Serializable gid = (Serializable) s.save(g);
		Serializable g2id = (Serializable) s.save(g2);
		t.commit();
		assertTrue( g.getVersion()==0 );
		assertTrue( g2.getVersion()==0 );
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Glarch) s.get(Glarch.class, gid);
		g2 = (Glarch) s.get(Glarch.class, g2id);
		assertTrue( g2.getStrings().size()==1 );
		s.delete(g);
		s.delete(g2);
		t.commit();
		s.close();

	}

	public void testBagMultipleElements() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Baz baz = new Baz();
		baz.setBag( new ArrayList() );
		baz.setByteBag( new ArrayList() );
		s.save(baz);
		baz.getBag().add("foo");
		baz.getBag().add("bar");
		baz.getByteBag().add( "foo".getBytes() );
		baz.getByteBag().add( "bar".getBytes() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		//put in cache
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertTrue( baz.getBag().size()==2 );
		assertTrue( baz.getByteBag().size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		baz = (Baz) s.get( Baz.class, baz.getCode() );
		assertTrue( baz.getBag().size()==2 );
		assertTrue( baz.getByteBag().size()==2 );
		baz.getBag().remove("bar");
 		baz.getBag().add("foo");
 		baz.getByteBag().add( "bar".getBytes() );
		t.commit();
		s.close();

 		s = openSession();
 		t = s.beginTransaction();
 		baz = (Baz) s.get( Baz.class, baz.getCode() );
 		assertTrue( baz.getBag().size()==2 );
 		assertTrue( baz.getByteBag().size()==3 );
 		s.delete(baz);
 		t.commit();
 		s.close();
 	}

	public void testWierdSession() throws Exception {
 		Session s = openSession();
 		Transaction t = s.beginTransaction();
 		Serializable id =  s.save( new Foo() );
 		t.commit();
 		s.close();

 		s = openSession();
 		s.setFlushMode(FlushMode.MANUAL);
		t = s.beginTransaction();
		Foo foo = (Foo) s.get(Foo.class, id);
		t.commit();
		s.disconnect();

		s.reconnect();
		t = s.beginTransaction();
		s.flush();
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		foo = (Foo) s.get(Foo.class, id);
		s.delete(foo);
		t.commit();
		s.close();
	}

}
