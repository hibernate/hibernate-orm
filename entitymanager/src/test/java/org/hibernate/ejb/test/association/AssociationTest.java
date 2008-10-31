//$Id: $
package org.hibernate.ejb.test.association;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class AssociationTest extends TestCase {
	public void testBidirOneToOne() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		String id = "10";
		Incident i = em.find( Incident.class, id );
		if ( i == null ) {
			i = new Incident( id );
			IncidentStatus ist = new IncidentStatus( id );
			i.setIncidentStatus( ist );
			ist.setIncident( i );
			em.persist( i );
		}
		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();
		em.remove( em.find(Incident.class, id) );
		em.getTransaction().commit();
		em.close();
	}

	public void testMergeAndBidirOneToOne() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Oven oven = new Oven();
		Kitchen kitchen = new Kitchen();
		em.persist( oven );
		em.persist( kitchen );
		kitchen.setOven( oven );
		oven.setKitchen( kitchen );
		em.flush();
		em.clear();
		//oven = em.find(Oven.class, oven.getId() );
		oven = em.merge( oven );
		em.flush();

		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Incident.class,
				IncidentStatus.class,
				Kitchen.class,
				Oven.class
		};
	}
}
