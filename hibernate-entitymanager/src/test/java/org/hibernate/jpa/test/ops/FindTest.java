/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ops;

import javax.persistence.EntityManager;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class FindTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testSubclassWrongId() throws Exception {
		Mammal mammal = new Mammal();
		mammal.setMamalNbr( 2 );
		mammal.setName( "Human" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( mammal );
		em.flush();
		Assert.assertNull( em.find( Reptile.class, 1l ) );
		em.getTransaction().rollback();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Mammal.class,
				Reptile.class,
				Animal.class
		};
	}
}
