/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.ops;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class MergeNewTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testMergeNew() throws Exception {
		Workload load = new Workload();
		load.name = "Cleaning";
		load.load = 10;
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		load = em.merge( load );
		assertNotNull( load.id );
		em.flush();
		assertNotNull( load.id );
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	public void testMergeAfterRemove() throws Exception {
		Workload load = new Workload();
		load.name = "Cleaning";
		load.load = 10;
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		load = em.merge( load );
		em.flush();
		em.getTransaction().commit();
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		load = em.find( Workload.class, load.id );
		em.remove( load );
		em.flush();
		em.getTransaction().commit();
		em.close();
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge( load );
		em.flush();
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Workload.class
		};
	}
}
