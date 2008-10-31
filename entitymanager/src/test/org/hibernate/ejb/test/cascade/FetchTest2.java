package org.hibernate.ejb.test.cascade;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

public class FetchTest2 extends TestCase {

	public void testProxyTransientStuff() throws Exception {

		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();

		Troop2 disney = new Troop2();
		disney.setName( "Disney" );

		Soldier2 mickey = new Soldier2();
		mickey.setName( "Mickey" );
		mickey.setTroop( disney );

		em.persist( disney );
		em.persist( mickey );

		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();

		Soldier2 soldier = em.find( Soldier2.class, mickey.getId() );
		soldier.getTroop().getId();
		try {
			em.flush();
		}
		catch (IllegalStateException e) {
			fail( "Should not raise an exception" );
		}

		em.getTransaction().commit();
		em.close();

		em = factory.createEntityManager();
		em.getTransaction().begin();

		//load troop wo a proxy
		disney = em.find( Troop2.class, disney.getId() );
		soldier = em.find( Soldier2.class, mickey.getId() );

		try {
			em.flush();
		}
		catch (IllegalStateException e) {
			fail( "Should not raise an exception" );
		}
		em.remove( soldier );
		em.remove( disney );
		em.getTransaction().commit();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Troop2.class,
				Soldier2.class
		};
	}
}
