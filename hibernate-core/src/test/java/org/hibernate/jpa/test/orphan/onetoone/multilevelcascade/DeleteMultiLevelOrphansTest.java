/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.orphan.onetoone.multilevelcascade;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class DeleteMultiLevelOrphansTest extends BaseEntityManagerFunctionalTestCase {

	private void createData() {
		Preisregelung preisregelung = new Preisregelung();

		Tranchenmodell tranchenmodell = new Tranchenmodell();

		X x = new X();

		Tranche tranche1 = new Tranche();

		Y y = new Y();

		Tranche tranche2 = new Tranche();

		preisregelung.setTranchenmodell( tranchenmodell );
		tranchenmodell.setPreisregelung( preisregelung );

		tranchenmodell.setX( x );
		x.setTranchenmodell( tranchenmodell );

		tranchenmodell.getTranchen().add( tranche1 );
		tranche1.setTranchenmodell( tranchenmodell );
		tranchenmodell.getTranchen().add( tranche2 );
		tranche2.setTranchenmodell( tranchenmodell );

		tranche1.setY( y );
		y.setTranche( tranche1 );

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( preisregelung );
		em.getTransaction().commit();
		em.close();
	}

	private void cleanupData() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete Tranche" ).executeUpdate();
		em.createQuery( "delete Tranchenmodell" ).executeUpdate();
		em.createQuery( "delete Preisregelung" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testDirectAssociationOrphanedWhileManaged() {
		createData();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = (Preisregelung) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
		preisregelung.setTranchenmodell( null );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		preisregelung = (Preisregelung) em.find( Preisregelung.class, preisregelung.getId() );
		assertNull( preisregelung.getTranchenmodell() );
		results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from Tranche" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from X" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from Y" ).getResultList();
		assertEquals( 0, results.size() );

		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );

		em.getTransaction().commit();
		em.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testReplacedDirectAssociationWhileManaged() {
		createData();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = (Preisregelung) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );

		// Create a new Tranchenmodell with new direct and nested associations
		Tranchenmodell tranchenmodellNew = new Tranchenmodell();
		X xNew = new X();
		tranchenmodellNew.setX( xNew );
		xNew.setTranchenmodell( tranchenmodellNew );
		Tranche trancheNew = new Tranche();
		tranchenmodellNew.getTranchen().add( trancheNew );
		trancheNew.setTranchenmodell( tranchenmodellNew );
		Y yNew = new Y();
		trancheNew.setY( yNew );
		yNew.setTranche( trancheNew );

		// Replace with a new Tranchenmodell instance containing new direct and nested associations
		preisregelung.setTranchenmodell(tranchenmodellNew );
		tranchenmodellNew.setPreisregelung( preisregelung );

		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		results = em.createQuery( "from Tranche" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from X" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Y" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );
		preisregelung = (Preisregelung) results.get( 0 );
		tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertEquals( tranchenmodellNew.getId(), tranchenmodell.getId() );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( xNew.getId(), tranchenmodell.getX().getId() );
		assertEquals( 1, tranchenmodell.getTranchen().size() );
		assertEquals( trancheNew.getId(), tranchenmodell.getTranchen().get( 0 ).getId() );
		assertEquals( yNew.getId(), tranchenmodell.getTranchen().get( 0 ).getY().getId() );

		// Replace with a new Tranchenmodell instance with no associations
		tranchenmodellNew = new Tranchenmodell();
		preisregelung.setTranchenmodell(tranchenmodellNew );
		tranchenmodellNew.setPreisregelung( preisregelung );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 1, results.size() );
		tranchenmodell = (Tranchenmodell) results.get( 0 );
		assertEquals( tranchenmodellNew.getId(), tranchenmodell.getId() );
		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );
		preisregelung =  (Preisregelung) results.get( 0 );
		assertEquals( tranchenmodell, preisregelung.getTranchenmodell() );
		results = em.createQuery( "from Tranche" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from X" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from Y" ).getResultList();
		assertEquals( 0, results.size() );
		em.getTransaction().commit();
		em.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testDirectAndNestedAssociationsOrphanedWhileManaged() {
		createData();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		List results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 1, results.size() );
		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = (Preisregelung) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
		preisregelung.setTranchenmodell( null );
		tranchenmodell.setX( null );
		tranchenmodell.getTranchen().get( 0 ).setY( null );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		preisregelung = (Preisregelung) em.find( Preisregelung.class, preisregelung.getId() );
		assertNull( preisregelung.getTranchenmodell() );
		results = em.createQuery( "from Tranchenmodell" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from Tranche" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from X" ).getResultList();
		assertEquals( 0, results.size() );
		results = em.createQuery( "from Y" ).getResultList();
		assertEquals( 0, results.size() );

		results = em.createQuery( "from Preisregelung" ).getResultList();
		assertEquals( 1, results.size() );

		em.getTransaction().commit();
		em.close();

		cleanupData();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Preisregelung.class,
				Tranche.class,
				Tranchenmodell.class,
				X.class,
				Y.class
		};
	}

}
