/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.legacy;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metamodel.spi.relational.Index;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ABCTest extends LegacyTestCase {
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
		boolean found = false;
		for ( Schema schema : metadata().getDatabase().getSchemas() ) {
			for ( Table table : schema.getTables() ) {
				for ( Index index : table.getIndexes() ) {
					if ( index.getName().toString().equals( "indx_a_name" ) ) {
						found = true;
						break;
					}
				}
			}
		}
		assertTrue("Unable to locate indx_a_name index creation", found);
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

