/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.HSQLDialect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ABCProxyTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {  "legacy/ABCProxy.hbm.xml" };
	}

	@Test
	public void testDiscriminatorFiltering() throws Exception {
		if ( ( getDialect() instanceof HSQLDialect ) ) return;
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from C1 c1 left join c1.c2s c2").list();
		s.createCriteria(C1.class).createCriteria("c2s").list();
		t.commit();
		s.close();
	}

	@Test
	public void testNarrow() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from E e join e.reverse as b where b.count=1").list();
		s.createQuery("from E e join e.as as b where b.count=1").list();
		t.commit();
		s.close();
	}

	@Test
	public void testSharedColumn() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		C1 c1 = new C1();
		C2 c2 = new C2();
		c1.setC2(c2);
		c2.setC1(c1);
		s.save(c1); s.save(c2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery( "from B" ).list();
		assertTrue( list.size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1 = (C1) s.createQuery("from C1").uniqueResult();
		c2 = (C2) s.createQuery("from C2").uniqueResult();
		assertTrue( c1.getC2()==c2 );
		assertTrue( c2.getC1()==c1 );
		assertTrue( c1.getC2s().contains(c2) );
		assertTrue( c2.getC1s().contains(c1) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1 = (C1) s.get( A.class, c1.getId() );
		c2 = (C2) s.get( A.class, c2.getId() );
		assertTrue( c1.getC2()==c2 );
		assertTrue( c2.getC1()==c1 );
		assertTrue( c1.getC2s().contains(c2) );
		assertTrue( c2.getC1s().contains(c1) );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete(c1); s.delete(c2);
		t.commit();
		s.close();

	}

	@Test
	public void testSubclassing() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		C1 c1 = new C1();
		D d = new D();
		d.setAmount(213.34f);
		c1.setAddress("foo bar");
		c1.setCount(23432);
		c1.setName("c1");
		c1.setD(d);
		s.save(c1);
		d.setId( c1.getId() );
		s.save(d);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		A c1a = (A) s.load( A.class, c1.getId() );
		assertFalse( Hibernate.isInitialized(c1a) );
		assertTrue( c1a.getName().equals("c1") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		B c1b = (B) s.load( B.class, c1.getId() );
		assertTrue(
			(c1b.getCount()==23432) &&
			c1b.getName().equals("c1")
		);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1 = (C1) s.load( C1.class, c1.getId() );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1a = (A) s.load( A.class, c1.getId() );
		assertTrue( c1a.getName().equals("c1") );
		c1 = (C1) s.load( C1.class, c1.getId() );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		c1b = (B) s.load( B.class, c1.getId() );
		assertTrue(
			(c1b.getCount()==23432) &&
			c1b.getName().equals("c1")
		);
		assertTrue( c1a.getName().equals("c1") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1a = (A) s.load( A.class, c1.getId() );
		assertTrue( c1a.getName().equals("c1") );
		c1 = (C1) s.load( C1.class, c1.getId(), LockMode.UPGRADE );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		c1b = (B) s.load( B.class, c1.getId(), LockMode.UPGRADE );
		assertTrue(
			(c1b.getCount()==23432) &&
			c1b.getName().equals("c1")
		);
		assertTrue( c1a.getName().equals("c1") );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c1a = (A) s.load( A.class, c1.getId() );
		c1 = (C1) s.load( C1.class, c1.getId() );
		c1b = (B) s.load( B.class, c1.getId() );
		assertTrue( c1a.getName().equals("c1") );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		assertTrue(
			(c1b.getCount()==23432) &&
			c1b.getName().equals("c1")
		);
		for ( Object a : s.createQuery( "from A" ).list() ) {
			s.delete( a );
		}
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.save( new B() );
		s.save( new A() );
		assertTrue( s.createQuery( "from B" ).list().size()==1 );
		assertTrue( s.createQuery( "from A" ).list().size()==2 );
		for ( Object a : s.createQuery( "from A" ).list() ) {
			s.delete( a );
		}
		t.commit();
		s.close();
	}

	@Test
	public void testSubclassMap() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		B b = new B();
		s.save(b);
		Map map = new HashMap();
		map.put("3", new Integer(1) );
		b.setMap(map);
		s.flush();
		s.delete(b);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		map = new HashMap();
		map.put("3", new Integer(1) );
		b = new B();
		b.setMap(map);
		s.save(b);
		s.flush();
		s.delete(b);
		t.commit();
		s.close();
	}

	@Test
	public void testOneToOne() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		A a = new A();
		E d1 = new E();
		C1 c = new C1();
		E d2 = new E();
		a.setForward(d1);
		d1.setReverse(a);
		c.setForward(d2);
		d2.setReverse(c);
		Serializable aid = s.save(a);
		Serializable d2id = s.save(d2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List l = s.createQuery( "from E e, A a where e.reverse = a.forward and a = ?" )
				.setEntity( 0, a )
				.list();
		assertTrue( l.size()==1 );
		l = s.createQuery( "from E e join fetch e.reverse" ).list();
		assertTrue( l.size()==2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		l = s.createQuery( "from E e" ).list();
		assertTrue( l.size()==2 );
		E e = (E) l.get(0);
		assertTrue( e==e.getReverse().getForward() );
		e = (E) l.get(1);
		assertTrue( e==e.getReverse().getForward() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		a = (A) s.load(A.class, aid);
		d2 = (E) s.load(E.class, d2id);
		assertTrue( a==a.getForward().getReverse() );
		assertTrue( d2==d2.getReverse().getForward() );
		s.delete(a);
		s.delete( a.getForward() );
		s.delete(d2);
		s.delete( d2.getReverse() );
		t.commit();

		s = openSession();
		t = s.beginTransaction();
		l = s.createQuery( "from E e" ).list();
		assertTrue( l.size()==0 );
		t.commit();
		s.close();
	}


}
