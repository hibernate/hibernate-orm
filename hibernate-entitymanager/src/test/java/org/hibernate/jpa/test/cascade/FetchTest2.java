/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
