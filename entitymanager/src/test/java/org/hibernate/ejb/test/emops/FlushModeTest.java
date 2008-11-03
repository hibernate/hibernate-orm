//$Id$
package org.hibernate.ejb.test.emops;

import java.util.Map;
import java.util.HashMap;
import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class FlushModeTest extends TestCase {

	public void testCreateEMFlushMode() throws Exception {
		Map properties = new HashMap();
		properties.put( "org.hibernate.flushMode", "manual" );
		EntityManager em = createEntityManager( properties );
		em.getTransaction().begin();
		Dress dress = new Dress();
		dress.name  = "long dress";
		em.persist( dress );
		em.getTransaction().commit();

		em.clear();

		assertNull( em.find( Dress.class, dress.name ) );

		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Race.class,
				Competitor.class,
				Dress.class
		};
	}
}
