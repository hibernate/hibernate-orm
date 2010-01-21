//$Id$
package org.hibernate.test.annotations.id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.id.entities.Location;
import org.hibernate.test.annotations.id.entities.Tower;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class IdClassTest extends TestCase {
	
	public void testIdClassInSuperclass() throws Exception {
		Tower tower = new Tower();
		tower.latitude = 10.3;
		tower.longitude = 45.4;
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		s.flush();
		s.clear();
		Location loc = new Location();
		loc.latitude = tower.latitude;
		loc.longitude = tower.longitude;
		assertNotNull( s.get( Tower.class, loc ) );
		tx.rollback();
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Tower.class
		};
	}
}
