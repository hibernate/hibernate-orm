//$Id$
package org.hibernate.test.annotations.entitynonentity;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class EntityNonEntityTest extends TestCase {
	public void testMix() throws Exception {
		GSM gsm = new GSM();
		gsm.brand = "Sony";
		gsm.frequency = 900;
		gsm.isNumeric = true;
		gsm.number = 2;
		gsm.species = "human";
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( gsm );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		gsm = (GSM) s.get( GSM.class, gsm.id );
		assertEquals( "top mapped superclass", 2, gsm.number );
		assertNull( "non entity between mapped superclass and entity", gsm.species );
		assertTrue( "mapped superclass under entity", gsm.isNumeric );
		assertNull( "non entity under entity", gsm.brand );
		assertEquals( "leaf entity", 900, gsm.frequency );
		s.delete( gsm );
		tx.commit();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Phone.class,
				Voice.class,
				GSM.class
		};
	}
}
