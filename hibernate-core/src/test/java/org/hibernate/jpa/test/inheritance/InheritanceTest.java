/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.inheritance;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testFind() throws Exception {
		EntityManager firstSession = getOrCreateEntityManager( );
        Strawberry u = new Strawberry();
		u.setSize( 12l );
		firstSession.getTransaction().begin();
        firstSession.persist(u);
        firstSession.getTransaction().commit();
        Long newId = u.getId();
        firstSession.clear();

		firstSession.getTransaction().begin();
        // 1.
        Strawberry result1 = firstSession.find(Strawberry.class, newId);
        assertNotNull( result1 );

        // 2.
        Strawberry result2 = (Strawberry) firstSession.find(Fruit.class, newId);
        System.out.println("2. result is:" + result2);

        firstSession.getTransaction().commit();
        firstSession.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Fruit.class,
				Strawberry.class
		};
	}
}
