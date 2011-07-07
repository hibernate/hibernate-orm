//$Id$
package org.hibernate.test.annotations.inheritance.joined;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassAndSecondaryTable extends TestCase {

	public void testSecondaryTableAndJoined() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		SwimmingPool sp = new SwimmingPool();
		//sp.setAddress( "Park Avenue" );
		s.persist( sp );
		s.flush();
		s.clear();
		
		SwimmingPool sp2 = (SwimmingPool)s.get(SwimmingPool.class, sp.getId());
		assertEquals( sp.getAddress(), null);
		
		PoolAddress addr = new PoolAddress();
		addr.setAddress("Park Avenue");
		sp2.setAddress(addr);
		
		s.flush();
		s.clear();
		
		sp2 = (SwimmingPool)s.get(SwimmingPool.class, sp.getId());
		assertFalse( sp2.getAddress() == null );
		assertEquals( sp2.getAddress().getAddress(), "Park Avenue");
		
		tx.rollback();
		s.close();
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Pool.class,
				SwimmingPool.class
		};
	}

}
