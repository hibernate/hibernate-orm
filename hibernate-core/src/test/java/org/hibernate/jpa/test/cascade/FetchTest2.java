/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.fail;

public class FetchTest2 extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testProxyTransientStuff() throws Exception {
		EntityManager em = entityManagerFactory().createEntityManager();
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

		em = entityManagerFactory().createEntityManager();
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

		em = entityManagerFactory().createEntityManager();
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

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Troop2.class,
				Soldier2.class
		};
	}
}
