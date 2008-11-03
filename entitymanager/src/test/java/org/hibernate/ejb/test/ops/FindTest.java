//$Id$
package org.hibernate.ejb.test.ops;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class FindTest extends TestCase {

	public void testSubclassWrongId() throws Exception {
		Mammal mammal = new Mammal();
		mammal.setMamalNbr( 2 );
		mammal.setName( "Human" );
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( mammal );
		em.flush();
		assertNull( em.find(Reptile.class, 1l) );
		em.getTransaction().rollback();
		em.close();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Mammal.class,
				Reptile.class,
				Animal.class
		};
	}
}
