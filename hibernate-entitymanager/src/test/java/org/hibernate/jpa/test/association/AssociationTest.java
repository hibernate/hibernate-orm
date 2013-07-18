package org.hibernate.jpa.test.association;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class AssociationTest extends BaseEntityManagerFunctionalTestCase {
	@Test
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
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find(Incident.class, id) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMergeAndBidirOneToOne() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Oven oven = new Oven();
		Kitchen kitchen = new Kitchen();
		em.persist( oven );
		em.persist( kitchen );
		kitchen.setOven( oven );
		oven.setKitchen( kitchen );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		oven = em.merge( oven );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.find( Oven.class, oven.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Incident.class,
				IncidentStatus.class,
				Kitchen.class,
				Oven.class
		};
	}
}
