//$Id: $
package org.hibernate.ejb.test.emops;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class RefreshTest extends TestCase {

	public void testRefreshNonManaged() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Race race = new Race();
		em.persist( race );
		em.flush();
		em.clear();

		try {
			em.refresh( race );
			fail("Refresh should fail on a non managed entity");
		}
		catch( IllegalArgumentException e) {
			//success
		}

		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Race.class,
				Competitor.class
		};
	}
}
