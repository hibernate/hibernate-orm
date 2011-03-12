//$Id$
package org.hibernate.ejb.test.ops;

import javax.persistence.EntityManager;

import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class MergeNewTest extends TestCase {

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

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Workload.class
		};
	}
}
