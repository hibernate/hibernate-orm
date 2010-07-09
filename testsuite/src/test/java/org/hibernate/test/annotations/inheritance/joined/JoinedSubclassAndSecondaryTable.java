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
		sp.setAddress( "Park Avenue" );
		s.persist( sp );
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
