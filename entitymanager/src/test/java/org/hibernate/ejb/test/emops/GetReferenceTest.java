//$Id: $
package org.hibernate.ejb.test.emops;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;


/**
 * @author Emmanuel Bernard
 */
public class GetReferenceTest extends TestCase {

	public void testWrongIdType() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		try {
			Competitor c = em.getReference( Competitor.class, new String("30") );
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			//success
		}
		catch ( Exception e ) {
			fail("Wrong exception: " + e );
		}

		try {
			Mail c = em.getReference( Mail.class, 1 );
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			//success
		}
		catch ( Exception e ) {
			fail("Wrong exception: " + e );
		}
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Competitor.class,
				Race.class,
				Mail.class
		};
	}
}
