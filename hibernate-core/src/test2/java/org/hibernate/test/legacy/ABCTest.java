/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ABCTest extends BaseNonConfigCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "legacy/ABC.hbm.xml", "legacy/ABCExtends.hbm.xml" };
	}

	@Test
	public void testFormulaAssociation() throws Throwable {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Long did = Long.valueOf(12);
		D d = new D( did );
		s.save(d);
		A a = new A();
		a.setName("a");
		s.save( a );
		d.setReverse( a );
		d.inverse = a;
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		d = (D) s.get(D.class, did);
		assertNotNull( d.getReverse() );
		s.clear();
		sessionFactory().getCache().evictEntityRegion( D.class );
		sessionFactory().getCache().evictEntityRegion(A.class);
		d = (D) s.get(D.class, did);
		assertNotNull( d.inverse );
		assertTrue(d.inverse.getName().equals("a"));
		s.clear();
		sessionFactory().getCache().evictEntityRegion( D.class );
		sessionFactory().getCache().evictEntityRegion( A.class );
		assertTrue( s.createQuery( "from D d join d.reverse r join d.inverse i where i = r" ).list().size()==1 );
		t.commit();
		s.close();
	}

	@Test
	public void testHigherLevelIndexDefinition() throws Throwable {
		Table table = metadata().getDatabase().getDefaultNamespace().locateTable( Identifier.toIdentifier( "TA" ) );
		Iterator<Index> indexItr = table.getIndexIterator();
		boolean found = false;
		while ( indexItr.hasNext() ) {
			final Index index = indexItr.next();
			if ( "indx_a_name".equals( index.getName() ) ) {
				found = true;
				break;
			}
		}
		assertTrue("Unable to locate indx_a_name index", found);
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
		c1.setBName("a funny name");
		c1.setD(d);
		s.save(c1);
		d.setId( c1.getId() );
		s.save(d);

		assertTrue( s.createQuery( "from C2 c where 1=1 or 1=1" ).list().size()==0 );

		t.commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( A.class );
		
		s = openSession();
		t = s.beginTransaction();
		c1 = (C1) s.get( A.class, c1.getId() );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		assertEquals( "a funny name", c1.getBName() );
		t.commit();
		s.close();
		
		sessionFactory().getCache().evictEntityRegion( A.class );

		s = openSession();
		t = s.beginTransaction();
		c1 = (C1) s.get( B.class, c1.getId() );
		assertTrue(
			c1.getAddress().equals("foo bar") &&
			(c1.getCount()==23432) &&
			c1.getName().equals("c1") &&
			c1.getD().getAmount()>213.3f
		);
		assertEquals( "a funny name", c1.getBName() );
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
		List bs = s.createQuery("from B").list();
		for (int i=0; i<bs.size(); i++) {
			C1 b = (C1) bs.get(i);
			s.delete(b);
			s.delete( b.getD() );
		}
		t.commit();
		s.close();
	}
	
	@Test
	public void testGetSave() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertNull( s.get( D.class, Long.valueOf(1) ) );
		D d = new D();
		d.setId( Long.valueOf(1) );
		s.save(d);
		s.flush();
		assertNotNull( s.get( D.class, Long.valueOf(1) ) );
		s.delete(d);
		s.flush();
		t.commit();
		s.close();
	}

}

