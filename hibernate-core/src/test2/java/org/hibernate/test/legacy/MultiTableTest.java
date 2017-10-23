/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: MultiTableTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.junit.Test;


public class MultiTableTest extends LegacyTestCase {
	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	@Override
	public String[] getMappings() {
		return new String[] { "legacy/Multi.hbm.xml", "legacy/MultiExtends.hbm.xml" };
	}

	@Test
	public void testCriteria() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Lower l = new Lower();
		s.save(l);
		assertTrue( l==s.createCriteria(Top.class).uniqueResult() );
		s.delete(l);
		s.flush();
		Criteria c = s.createCriteria(Lower.class);
		c.createCriteria("yetanother")
			.add( Restrictions.isNotNull("id") )
			.createCriteria("another");
		c.createCriteria("another").add( Restrictions.isNotNull("id") );
		c.list();
		t.commit();
		s.close();
	}

	@Test
	public void testFetchOneToMany() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createCriteria(Po.class).setFetchMode("set", FetchMode.JOIN).list();
		s.createCriteria(Po.class).setFetchMode("list", FetchMode.JOIN).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNarrow() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery("from Po po, Lower low where low.mypo = po").list();
		s.createQuery("from Po po join po.set as sm where sm.amount > 0").list();
		s.createQuery("from Po po join po.top as low where low.foo = 'po'").list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testJoins() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "from Lower l join l.yetanother l2 where lower(l2.name) > 'a'" ).list();
		s.createQuery( "from Lower l where lower(l.yetanother.top.name) > 'a'" ).list();
		s.createQuery( "from SubMulti sm join sm.children smc where smc.name > 'a'" ).list();
		s.createQuery( "select s, ya from Lower s join s.yetanother ya" ).list();
		s.createQuery( "from Lower s1 join s1.bag s2" ).list();
		s.createQuery( "from Lower s1 left join s1.bag s2" ).list();
		s.createQuery( "select s, a from Lower s join s.another a" ).list();
		s.createQuery( "select s, a from Lower s left join s.another a" ).list();
		s.createQuery( "from Top s, Lower ls" ).list();
		s.createQuery( "from Lower ls join ls.set s where s.name > 'a'" ).list();
		s.createQuery( "from Po po join po.list sm where sm.name > 'a'" ).list();
		s.createQuery( "from Lower ls inner join ls.another s where s.name is not null" ).list();
		s.createQuery( "from Lower ls where ls.other.another.name is not null" ).list();
		s.createQuery( "from Multi m where m.derived like 'F%'" ).list();
		s.createQuery( "from SubMulti m where m.derived like 'F%'" ).list();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSubclassCollection() throws Exception {
		//if ( getDialect() instanceof HSQLDialect ) return; //TODO: figure out why!?
		Session s = openSession();
		s.beginTransaction();
		SubMulti sm = new SubMulti();
		SubMulti sm1 = new SubMulti();
		SubMulti sm2 = new SubMulti();
		ArrayList list = new ArrayList();
		ArrayList anotherList = new ArrayList();
		sm.setChildren(list);
		sm.setMoreChildren(anotherList);
		sm.setExtraProp("foo");
		list.add(sm1);
		list.add(sm2);
		anotherList.add(sm1);
		anotherList.add(sm2);
		sm1.setParent(sm);
		sm2.setParent(sm);
		Serializable id = s.save(sm);
		s.save(sm1);
		s.save(sm2);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion(SubMulti.class);

		final Session s2 = openSession();
		s2.beginTransaction();
		s2.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						final String sql = "select * from leafsubsubclass sm, nonleafsubclass m, rootclass s " +
								"where sm.sid=m.sid and sm.sid=s.id1_ and sm.sid=1";
						Statement st = ((SessionImplementor)s2).getJdbcCoordinator().getStatementPreparer().createStatement();
						((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().extract( st, sql ).next();
					}
				}
		);
		assertTrue(
				s2.createQuery(
						"select s from SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null"
				).list().size()==2 );
		s2.createQuery( "select c from SubMulti sm join sm.children c" ).list();
		assertTrue( s2.createQuery( "select elements(sm.children) from SubMulti as sm" ).list().size()==2 );
		assertTrue(
				s2.createQuery(
						"select distinct sm from SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null"
				).list().size()==1 );
		sm = (SubMulti) s2.load(SubMulti.class, id);
		assertTrue( sm.getChildren().size()==2 );
		assertEquals(
			s2.createFilter( sm.getMoreChildren(), "select count(*) where this.amount>-1 and this.name is null" ).list().get(0),
			new Long(2)
		);
		assertEquals( "FOO", sm.getDerived() );
		assertSame(
				s2.createQuery( "select distinct s from SubMulti s where s.moreChildren[1].amount < 1.0" ).iterate().next(),
			sm
		);
		assertTrue( sm.getMoreChildren().size()==2 );
		s2.delete(sm);
		Iterator iter = sm.getChildren().iterator();
		while ( iter.hasNext() ) {
			s2.delete( iter.next() );
		}
		s2.flush();
		s2.getTransaction().commit();
		s2.close();

	}

	@Test
	public void testCollectionOnly() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Mono m = new Mono();
		Long id = (Long) s.save(m);
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.update( m );
		s.flush();
		m.setAddress("foo bar");
		s.flush();
		s.delete(m);
		t.commit();
		s.close();
	}

	@Test
	public void testQueries() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Long id = ( Long ) s.save( new TrivialClass() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		TrivialClass tc = (TrivialClass) s.load(TrivialClass.class, id);
		s.createQuery( "from TrivialClass s where s.id = 2" ).list();
		s.createQuery( "select t.count from Top t" ).list();
		s.createQuery( "from Lower s where s.another.name='name'" ).list();
		s.createQuery( "from Lower s where s.yetanother.name='name'" ).list();
		s.createQuery( "from Lower s where s.yetanother.name='name' and s.yetanother.foo is null" ).list();
		s.createQuery( "from Top s where s.count=1" ).list();
		s.createQuery( "select s.count from Top s, Lower ls where ls.another=s" ).list();
		s.createQuery( "select elements(ls.bag), elements(ls.set) from Lower ls" ).list();
		s.createQuery( "from Lower" ).iterate();
		s.createQuery( "from Top" ).iterate();
		s.delete(tc);
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testConstraints() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		SubMulti sm = new SubMulti();
		sm.setAmount(66.5f);
		s.save( sm );
		t.commit();
		s.close();

		s = openSession();
//		doDelete( s, "from SubMulti" );
//		t = s.beginTransaction();
		t = s.beginTransaction();
		doDelete( s, "from SubMulti" );
		t.commit();
		s.close();
	}

	@Test
	public void testMultiTable() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Multi multi = new Multi();
		multi.setExtraProp("extra");
		multi.setName("name");
		Top simp = new Top();
		simp.setDate( new Date() );
		simp.setName("simp");

		Serializable mid = s.save(multi);
		Serializable sid = s.save(simp);

		SubMulti sm = new SubMulti();
		sm.setAmount(66.5f);
		Serializable smid = s.save(sm);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi.setExtraProp( multi.getExtraProp() + "2" );
		//multi.setCount( multi.getCount() + 1 );
		multi.setName("new name");
		s.update( multi );
		simp.setName("new name");
		s.update( simp );
		sm.setAmount(456.7f);
		s.update( sm );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load(Multi.class, mid);
		assertTrue( multi.getExtraProp().equals("extra2") );
		multi.setExtraProp( multi.getExtraProp() + "3" );
		//multi.setCount( multi.getCount() + 1 );
		assertTrue( multi.getName().equals("new name") );
		multi.setName("newer name");
		sm = (SubMulti) s.load(SubMulti.class, smid);
		assertTrue( sm.getAmount()==456.7f );
		sm.setAmount(23423f);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load(Top.class, mid);
		simp = (Top) s.load(Top.class, sid);
		assertTrue( ! (simp instanceof Multi) );
		assertTrue( multi.getExtraProp().equals("extra23") );
		//multi.setCount( multi.getCount() + 1 );
		assertTrue( multi.getName().equals("newer name") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator iter = s.createQuery( "select\n\nt from Top t where t.count>0" ).iterate();
		boolean foundSimp = false;
		boolean foundMulti = false;
		boolean foundSubMulti = false;
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if ( ( o instanceof Top ) && !( o instanceof Multi) ) foundSimp = true;
			if ( o instanceof Multi && !(o instanceof SubMulti) ) foundMulti = true;
			if ( o instanceof SubMulti ) foundSubMulti = true;
		}
		assertTrue( foundSimp&&foundMulti&&foundSubMulti );
		s.createQuery( "from Multi m where m.count>0 and m.extraProp is not null" ).list();
		s.createQuery( "from Top m where m.count>0 and m.name is not null" ).list();
		s.createQuery( "from Lower m where m.other is not null" ).list();
		s.createQuery( "from Multi m where m.other.id = 1" ).list();
		s.createQuery( "from SubMulti m where m.amount > 0.0" ).list();

		assertTrue(
				s.createQuery( "from Multi" ).list().size()==2
		);
		assertTrue(
				s.createQuery( "from Multi m where m.class = SubMulti" ).list().size()==1
		);
		assertTrue(
				s.createQuery( "from Top m where m.class = Multi" ).list().size()==1
		);
		assertTrue(
				s.createQuery( "from Top" ).list().size()==3
		);
		assertTrue(
				s.createQuery( "from Lower" ).list().size()==0
		);
		assertTrue(
				s.createQuery( "from SubMulti" ).list().size()==1
		);

		s.createQuery( "from Lower ls join ls.bag s where s.id is not null" ).list();
		s.createQuery( "from Lower ls join ls.set s where s.id is not null" ).list();
		if ( !(getDialect() instanceof MySQLDialect) )
			s.createQuery( "from SubMulti sm where exists elements(sm.children)" ).list();

		List l = s.createCriteria(Top.class).list();
		assertTrue( l.size()==3 );
		assertTrue( s.createCriteria(SubMulti.class).list().size()==1 );
		assertTrue(
			s.createCriteria(SubMulti.class)
				.add( Restrictions.lt("amount", new Float(0)) )
				.list()
				.size()==0
		);
		assertTrue(
			s.createCriteria(SubMulti.class)
				.add( Restrictions.ge("amount", new Float(0)) )
				.list()
				.size()==1
		);

		t.commit();
		s.close();

		// HANA currently requires specifying table name by 'FOR UPDATE of t1.c1'
		// if there are more than one tables/views/subqueries in the FROM clause
		if ( !( getDialect() instanceof AbstractHANADialect ) ) {
			s = openSession();
			t = s.beginTransaction();
			multi = (Multi) s.load( Top.class, mid, LockMode.UPGRADE );
			simp = (Top) s.load( Top.class, sid );
			s.lock( simp, LockMode.UPGRADE_NOWAIT );
			t.commit();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		s.update(multi);
		s.delete(multi);
		assertEquals( 2, doDelete( s, "from Top" ) );
		t.commit();
		s.close();

	}

	@Test
	public void testMultiTableGeneratedId() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Multi multi = new Multi();
		multi.setExtraProp("extra");
		//multi.setCount(666);
		multi.setName("name");
		Top simp = new Top();
		simp.setDate( new Date() );
		simp.setName("simp");
		//simp.setCount(132);
		Serializable multiId = s.save( multi );
		Serializable simpId = s.save( simp );
		SubMulti sm = new SubMulti();
		sm.setAmount(66.5f);
		Serializable smId = s.save( sm );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi.setExtraProp( multi.getExtraProp() + "2" );
		//multi.setCount( multi.getCount() + 1 );
		multi.setName("new name");
		s.update( multi );
		simp.setName("new name");
		s.update( simp );
		sm.setAmount(456.7f);
		s.update( sm );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load( Multi.class, multiId );
		assertTrue( multi.getExtraProp().equals("extra2") );
		multi.setExtraProp( multi.getExtraProp() + "3" );
		//multi.setCount( multi.getCount() + 1 );
		assertTrue( multi.getName().equals("new name") );
		multi.setName("newer name");
		sm = (SubMulti) s.load( SubMulti.class, smId );
		assertTrue( sm.getAmount()==456.7f );
		sm.setAmount(23423f);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load( Top.class, multiId );
		simp = (Top) s.load( Top.class, simpId );
		assertTrue( ! (simp instanceof Multi) );
		assertTrue( multi.getExtraProp().equals("extra23") );
		//multi.setCount( multi.getCount() + 1 );
		assertTrue( multi.getName().equals("newer name") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Iterator iter = s.createQuery( "select\n\nt from Top t where t.count>0" ).iterate();
		boolean foundSimp = false;
		boolean foundMulti = false;
		boolean foundSubMulti = false;
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if ( ( o instanceof Top ) && !( o instanceof Multi) ) foundSimp = true;
			if ( o instanceof Multi && !(o instanceof SubMulti) ) foundMulti = true;
			if ( o instanceof SubMulti ) foundSubMulti = true;
		}
		assertTrue( foundSimp&&foundMulti&&foundSubMulti );
		s.createQuery( "from Multi m where m.count>0 and m.extraProp is not null" ).list();
		s.createQuery( "from Top m where m.count>0 and m.name is not null" ).list();
		s.createQuery( "from Lower m where m.other is not null" ).list();
		s.createQuery( "from Multi m where m.other.id = 1" ).list();
		s.createQuery( "from SubMulti m where m.amount > 0.0" ).list();

		assertTrue(
				s.createQuery( "from Multi" ).list().size()==2
		);
		/*assertTrue(
			s.find("from m in class Multi where m.class = Multi").size()==1
		);*/
		assertTrue(
				s.createQuery( "from Top" ).list().size()==3
		);
		assertTrue(
				s.createQuery( "from Lower" ).list().size()==0
		);
		assertTrue(
				s.createQuery( "from SubMulti" ).list().size()==1
		);

		s.createQuery( "from Lower ls join ls.bag s where s.id is not null" ).list();
		if ( !(getDialect() instanceof MySQLDialect) )
			s.createQuery( "from SubMulti sm where exists elements(sm.children)" ).list();

		t.commit();
		s.close();

		// HANA currently requires specifying table name by 'FOR UPDATE of t1.c1'
		// if there are more than one tables/views/subqueries in the FROM clause
		if ( !( getDialect() instanceof AbstractHANADialect ) ) {
			s = openSession();
			t = s.beginTransaction();
			multi = (Multi) s.load( Top.class, multiId, LockMode.UPGRADE );
			simp = (Top) s.load( Top.class, simpId );
			s.lock( simp, LockMode.UPGRADE_NOWAIT );
			t.commit();
			s.close();
		}

		s = openSession();
		t = s.beginTransaction();
		s.update( multi );
		s.delete(multi);
		assertEquals( 2, doDelete( s, "from Top" ) );
		t.commit();
		s.close();

	}

	@Test
	public void testMultiTableCollections() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertTrue( s.createQuery( "from Top" ).list().size()==0 );
		Multi multi = new Multi();
		multi.setExtraProp("extra");
		multi.setName("name");
		Top simp = new Top();
		simp.setDate( new Date() );
		simp.setName("simp");

		s.save(multi);
		s.save(simp);

		Lower ls = new Lower();
		ls.setOther(ls);
		ls.setAnother(ls);
		ls.setYetanother(ls);
		ls.setName("Less Simple");
		Set set = new HashSet();
		ls.setSet(set);
		set.add(multi);
		set.add(simp);
		Serializable id = s.save(ls);
		t.commit();
		s.close();
		assertTrue( ls.getOther()==ls && ls.getAnother()==ls && ls.getYetanother()==ls );

		s = openSession();
		t = s.beginTransaction();
		ls = (Lower) s.load(Lower.class, id);
		assertTrue( ls.getOther()==ls && ls.getAnother()==ls && ls.getYetanother()==ls );
		assertTrue( ls.getSet().size()==2 );
		Iterator iter = ls.getSet().iterator();
		int foundMulti = 0;
		int foundSimple = 0;
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if ( o instanceof Top ) foundSimple++;
			if ( o instanceof Multi ) foundMulti++;
		}
		assertTrue( foundSimple == 2 && foundMulti == 1 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		try {
			// MySQL does not like deleting rows that refer to itself without first
			// null'ing out the FK.  Ugh...
			ls = s.load( Lower.class, id );
			ls.setOther( null );
			ls.setAnother( null );
			ls.setYetanother( null );
			for ( Object o : ls.getSet() ) {
				s.delete( o );
			}
			ls.getSet().clear();
			s.flush();
			s.delete( ls );
			t.commit();
		}
		catch (Exception e) {
			t.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testMultiTableManyToOne() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertTrue( s.createQuery( "from Top" ).list().size() == 0 );
		Multi multi = new Multi();
		multi.setExtraProp( "extra" );
		multi.setName("name");
		s.save(multi);
		Lower ls = new Lower();
		ls.setOther(ls);
		ls.setAnother(multi);
		ls.setYetanother(ls);
		ls.setName("Less Simple");
		Serializable id = s.save(ls);
		t.commit();
		s.close();
		assertTrue( ls.getOther()==ls && ls.getAnother()==multi && ls.getYetanother()==ls );

		s = openSession();
		t = s.beginTransaction();
		try {
			// MySQL does not like deleting rows that refer to itself without first
			// null'ing out the FK.  Ugh...
			ls = s.load( Lower.class, id );
			assertTrue( ls.getOther() == ls && ls.getYetanother() == ls );
			assertTrue( ls.getAnother().getName().equals( "name" ) && ls.getAnother() instanceof Multi );
			s.delete( ls.getAnother() );
			ls.setOther( null );
			ls.setAnother( null );
			ls.setYetanother( null );
			ls.getSet().clear();
			s.flush();
			s.delete( ls );
			t.commit();
		}
		catch (Exception e) {
			t.rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testMultiTableNativeId() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Multi multi = new Multi();
		multi.setExtraProp("extra");
		Long id = (Long) s.save(multi);
		assertTrue( id!=null );
		s.delete(multi);
		t.commit();
		s.close();
	}

	@Test
	public void testCollection() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Multi multi1 = new Multi();
		multi1.setExtraProp("extra1");
		Multi multi2 = new Multi();
		multi2.setExtraProp("extra2");
		Po po = new Po();
		multi1.setPo(po); multi2.setPo(po);
		po.setSet( new HashSet() );
		po.getSet().add(multi1);
		po.getSet().add(multi2);
		po.setList( new ArrayList() );
		//po.getList().add(null);
		po.getList().add( new SubMulti() );
		Serializable id = s.save(po);
		assertTrue( id!=null );
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		po = (Po) s.load(Po.class, id);
		assertTrue( po.getSet().size()==2 );
		assertTrue( po.getList().size()==1 );
		s.delete(po);
		assertTrue( s.createQuery( "from Top" ).list().size()==0 );
		t.commit();
		s.close();
	}

	@Test
	public void testOneToOne() throws Exception {
		Session s = openSession();
		s.beginTransaction();
		Lower ls = new Lower();
		Serializable id = s.save(ls);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.load(Lower.class, id);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( s.load(Lower.class, id) );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testCollectionPointer() throws Exception {
		Session sess = openSession();
		sess.beginTransaction();
		Lower ls = new Lower();
		List list = new ArrayList();
		ls.setBag(list);
		Top s = new Top();
		Serializable id = sess.save(ls);
		sess.save(s);
		sess.flush();
		list.add(s);
		sess.getTransaction().commit();
		sess.close();

		sess = openSession();
		sess.beginTransaction();
		ls = (Lower) sess.load(Lower.class, id);
		assertTrue( ls.getBag().size()==1 );
		doDelete( sess, "from java.lang.Object" );
		sess.getTransaction().commit();
		sess.close();
	}

}
