//$Id: MultiTableTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;


public class MultiTableTest extends LegacyTestCase {

	public MultiTableTest(String arg0) {
		super(arg0);
	}

	public String[] getMappings() {
		return new String[] { "legacy/Multi.hbm.xml", "legacy/MultiExtends.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MultiTableTest.class );
	}

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

	public void testFetchOneToMany() throws Exception {
		Session s = openSession();
		s.createCriteria(Po.class).setFetchMode("set", FetchMode.EAGER).list();
		s.createCriteria(Po.class).setFetchMode("list", FetchMode.EAGER).list();
		s.connection().commit();
		s.close();
	}

	public void testNarrow() throws Exception {
		Session s = openSession();
		s.find("from Po po, Lower low where low.mypo = po");
		s.find("from Po po join po.set as sm where sm.amount > 0");
		s.find("from Po po join po.top as low where low.foo = 'po'");
		s.connection().commit();
		s.close();
	}

	public void testJoins() throws Exception {
		Session s = openSession();
		s.find("from Lower l join l.yetanother l2 where lower(l2.name) > 'a'");
		s.find("from Lower l where lower(l.yetanother.top.name) > 'a'");
		s.find("from SubMulti sm join sm.children smc where smc.name > 'a'");
		s.find("select s, ya from Lower s join s.yetanother ya");
		s.find("from Lower s1 join s1.bag s2");
		s.find("from Lower s1 left join s1.bag s2");
		s.find("select s, a from Lower s join s.another a");
		s.find("select s, a from Lower s left join s.another a");
		s.find("from Top s, Lower ls");
		s.find("from Lower ls join ls.set s where s.name > 'a'");
		s.find("from Po po join po.list sm where sm.name > 'a'");
		s.find("from Lower ls inner join ls.another s where s.name is not null");
		s.find("from Lower ls where ls.other.another.name is not null");
		s.find("from Multi m where m.derived like 'F%'");
		s.find("from SubMulti m where m.derived like 'F%'");
		s.connection().commit();
		s.close();
	}

	public void testSubclassCollection() throws Exception {
		//if ( getDialect() instanceof HSQLDialect ) return; //TODO: figure out why!?
		Session s = openSession();
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
		s.flush();
		s.connection().commit();
		s.close();

		getSessions().evict(SubMulti.class);

		s = openSession();
		s.connection().createStatement().executeQuery(
			"select * from leafsubsubclass sm, nonleafsubclass m, rootclass s where sm.sid=m.sid and sm.sid=s.id1_ and sm.sid=1"
		).next();
		assertTrue( s.find("select s from SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null").size()==2 );
		s.find("select c from SubMulti sm join sm.children c");
		assertTrue( s.find("select elements(sm.children) from SubMulti as sm").size()==2 );
		assertTrue( s.find("select distinct sm from SubMulti as sm join sm.children as s where s.amount>-1 and s.name is null").size()==1 );
		sm = (SubMulti) s.load(SubMulti.class, id);
		assertTrue( sm.getChildren().size()==2 );
		assertEquals(
			s.filter( sm.getMoreChildren(), "select count(*) where this.amount>-1 and this.name is null" ).iterator().next(),
			new Long(2)
		);
		assertEquals( "FOO", sm.getDerived() );
		assertSame(
			s.iterate("select distinct s from SubMulti s where s.moreChildren[1].amount < 1.0").next(),
			sm
		);
		assertTrue( sm.getMoreChildren().size()==2 );
		s.delete(sm);
		Iterator iter = sm.getChildren().iterator();
		while ( iter.hasNext() ) s.delete( iter.next() );
		s.flush();
		s.connection().commit();
		s.close();

	}

	public void testCollectionOnly() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Mono m = new Mono();
		Long id = (Long) s.save(m);
		t.commit();
		s.close();
		s = openSession();
		t = s.beginTransaction();
		s.update(m, id);
		s.flush();
		m.setAddress("foo bar");
		s.flush();
		s.delete(m);
		t.commit();
		s.close();
	}

	public void testQueries() throws Exception {
		Session s = openSession();
		Long id = ( Long ) s.save( new TrivialClass() );

		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		TrivialClass tc = (TrivialClass) s.load(TrivialClass.class, id);
		s.find("from TrivialClass s where s.id = 2");
		s.find("select t.count from Top t");
		s.find("from Lower s where s.another.name='name'");
		s.find("from Lower s where s.yetanother.name='name'");
		s.find("from Lower s where s.yetanother.name='name' and s.yetanother.foo is null");
		s.find("from Top s where s.count=1");
		s.find("select s.count from Top s, Lower ls where ls.another=s");
		s.find("select elements(ls.bag), elements(ls.set) from Lower ls");
		s.iterate("from Lower");
		s.iterate("from Top");
		s.delete(tc);
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testConstraints() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		SubMulti sm = new SubMulti();
		sm.setAmount(66.5f);
		s.save( sm );
		t.commit();
		s.close();
		s = openSession();
		s.delete( "from SubMulti" );
		t = s.beginTransaction();
		t.commit();
		s.close();
	}

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
		s.update(multi, mid);
		simp.setName("new name");
		s.update(simp, sid);
		sm.setAmount(456.7f);
		s.update(sm, smid);
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
		Iterator iter = s.iterate("select\n\nt from Top t where t.count>0");
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
		s.find("from Multi m where m.count>0 and m.extraProp is not null");
		s.find("from Top m where m.count>0 and m.name is not null");
		s.find("from Lower m where m.other is not null");
		s.find("from Multi m where m.other.id = 1");
		s.find("from SubMulti m where m.amount > 0.0");

		assertTrue(
			s.find("from Multi").size()==2
		);
		assertTrue(
			s.find("from Multi m where m.class = SubMulti").size()==1
		);
		assertTrue(
			s.find("from Top m where m.class = Multi").size()==1
		);
		assertTrue(
			s.find("from Top").size()==3
		);
		assertTrue(
			s.find("from Lower").size()==0
		);
		assertTrue(
			s.find("from SubMulti").size()==1
		);

		s.find("from Lower ls join ls.bag s where s.id is not null");
		s.find("from Lower ls join ls.set s where s.id is not null");
		if ( !(getDialect() instanceof MySQLDialect) ) s.find("from SubMulti sm where exists elements(sm.children)");

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

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load(Top.class, mid, LockMode.UPGRADE);
		simp = (Top) s.load(Top.class, sid);
		s.lock(simp, LockMode.UPGRADE_NOWAIT);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.update(multi, mid);
		s.delete(multi);
		assertTrue( s.delete("from Top")==2);
		t.commit();
		s.close();

	}

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
		s.update( multi, multiId );
		simp.setName("new name");
		s.update( simp, simpId );
		sm.setAmount(456.7f);
		s.update( sm, smId );
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
		Iterator iter = s.iterate("select\n\nt from Top t where t.count>0");
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
		s.find("from Multi m where m.count>0 and m.extraProp is not null");
		s.find("from Top m where m.count>0 and m.name is not null");
		s.find("from Lower m where m.other is not null");
		s.find("from Multi m where m.other.id = 1");
		s.find("from SubMulti m where m.amount > 0.0");

		assertTrue(
			s.find("from Multi").size()==2
		);
		/*assertTrue(
			s.find("from m in class Multi where m.class = Multi").size()==1
		);*/
		assertTrue(
			s.find("from Top").size()==3
		);
		assertTrue(
			s.find("from Lower").size()==0
		);
		assertTrue(
			s.find("from SubMulti").size()==1
		);

		s.find("from Lower ls join ls.bag s where s.id is not null");
		if ( !(getDialect() instanceof MySQLDialect) ) s.find("from SubMulti sm where exists elements(sm.children)");

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		multi = (Multi) s.load( Top.class, multiId, LockMode.UPGRADE );
		simp = (Top) s.load( Top.class, simpId );
		s.lock(simp, LockMode.UPGRADE_NOWAIT);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.update( multi, multiId );
		s.delete(multi);
		assertTrue( s.delete("from Top")==2);
		t.commit();
		s.close();

	}

	public void testMultiTableCollections() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertTrue( s.find("from Top").size()==0 );
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
		assertTrue( foundSimple==2 && foundMulti==1 );
		assertTrue( s.delete("from Top")==3 );
		t.commit();
		s.close();
	}

	public void testMultiTableManyToOne() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertTrue( s.find("from Top").size()==0 );
		Multi multi = new Multi();
		multi.setExtraProp("extra");
		multi.setName("name");
		Top simp = new Top();
		simp.setDate( new Date() );
		simp.setName("simp");
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
		ls = (Lower) s.load(Lower.class, id);
		assertTrue( ls.getOther()==ls && ls.getYetanother()==ls );
		assertTrue( ls.getAnother().getName().equals("name") && ls.getAnother() instanceof Multi );
		s.delete(ls);
		s.delete( ls.getAnother() );
		t.commit();
		s.close();
	}

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
		assertTrue( s.find("from Top").size()==0 );
		t.commit();
		s.close();
	}

	public void testOneToOne() throws Exception {
		Session s = openSession();
		Lower ls = new Lower();
		Serializable id = s.save(ls);
		s.flush();
		s.connection().commit();
		s.close();
		s = openSession();
		s.load(Lower.class, id);
		s.connection().commit();
		s.close();
		s = openSession();
		s.delete( s.load(Lower.class, id) );
		s.flush();
		s.connection().commit();
		s.close();
	}

	public void testCollectionPointer() throws Exception {
		Session sess = openSession();
		Lower ls = new Lower();
		List list = new ArrayList();
		ls.setBag(list);
		Top s = new Top();
		Serializable id = sess.save(ls);
		sess.save(s);
		sess.flush();
		list.add(s);
		sess.flush();
		sess.connection().commit();
		sess.close();

		sess = openSession();
		ls = (Lower) sess.load(Lower.class, id);
		assertTrue( ls.getBag().size()==1 );
		sess.delete("from java.lang.Object");
		sess.flush();
		sess.connection().commit();
		sess.close();
	}

}
