//$Id: ABCTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.util.List;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;


public class ABCTest extends LegacyTestCase {

	public ABCTest(String arg0) {
		super(arg0);
	}
	
	public void testFormulaAssociation() throws Throwable {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		D d = new D();
		Long did = new Long(12);
		s.save(d, did);
		A a = new A();
		a.setName("a");
		s.save(a, did);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		d = (D) s.get(D.class, did);
		assertTrue(d.getReverse().getId().equals(did));
		s.clear();
		getSessions().evict(D.class);
		getSessions().evict(A.class);
		d = (D) s.get(D.class, did);
		assertTrue(d.inverse.getId().equals(did));
		assertTrue(d.inverse.getName().equals("a"));
		s.clear();
		getSessions().evict(D.class);
		getSessions().evict(A.class);
		assertTrue( s.createQuery( "from D d join d.reverse r join d.inverse i where i = r" ).list().size()==1 );
		t.commit();
		s.close();
	}

	public void testHigherLevelIndexDefinition() throws Throwable {
		String[] commands = getCfg().generateSchemaCreationScript( getDialect() );
		int max = commands.length;
		boolean found = false;
		for (int indx = 0; indx < max; indx++) {
			System.out.println("Checking command : " + commands[indx]);
			found = commands[indx].indexOf("create index indx_a_name") >= 0;
			if (found)
				break;
		}
		assertTrue("Unable to locate indx_a_name index creation", found);
	}

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

		getSessions().evict(A.class);
		
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
		
		getSessions().evict(A.class);

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
	
	public void testGetSave() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		assertNull( s.get( D.class, new Long(1) ) );
		D d = new D();
		d.setId( new Long(1) );
		s.save(d);
		s.flush();
		assertNotNull( s.get( D.class, new Long(1) ) );
		s.delete(d);
		s.flush();
		t.commit();
		s.close();
	}

	public String[] getMappings() {
		return new String[] { "legacy/ABC.hbm.xml", "legacy/ABCExtends.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ABCTest.class );
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

}

