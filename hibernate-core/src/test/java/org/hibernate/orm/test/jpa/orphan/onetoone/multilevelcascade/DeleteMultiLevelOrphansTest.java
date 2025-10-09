/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone.multilevelcascade;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@Jpa(
		annotatedClasses = {
				Preisregelung.class,
				Tranche.class,
				Tranchenmodell.class,
				X.class,
				Y.class
		}
)
public class DeleteMultiLevelOrphansTest {

	@BeforeEach
	public void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

			entityManager.persist( preisregelung );
		} );
	}

	@AfterEach
	public void cleanupData(EntityManagerFactoryScope scope) {
		scope.inTransaction(  entityManager -> scope.getEntityManagerFactory().getSchemaManager().truncate() );
	}

	@Test
	@JiraKey( value = "HHH-9091")
	public void testDirectAssociationOrphanedWhileManaged(EntityManagerFactoryScope scope) {
		Long id = scope.fromTransaction(
				em -> {
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

					return preisregelung.getId();
				}
		);

		scope.inTransaction(
				em -> {
					Preisregelung preisregelung = em.find( Preisregelung.class, id );
					assertNull( preisregelung.getTranchenmodell() );
					List results = em.createQuery( "from Tranchenmodell" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from Tranche" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from X" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from Y" ).getResultList();
					assertEquals( 0, results.size() );

					results = em.createQuery( "from Preisregelung" ).getResultList();
					assertEquals( 1, results.size() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9091")
	public void testReplacedDirectAssociationWhileManaged(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
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

		em = scope.getEntityManagerFactory().createEntityManager();
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

		em = scope.getEntityManagerFactory().createEntityManager();
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
	}

	@Test
	@JiraKey( value = "HHH-9091")
	public void testDirectAndNestedAssociationsOrphanedWhileManaged(EntityManagerFactoryScope scope) {
		Long id = scope.fromTransaction(
				em -> {
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

					return preisregelung.getId();
				}
		);
		scope.inTransaction(
				em -> {
					Preisregelung preisregelung = em.find( Preisregelung.class, id );
					assertNull( preisregelung.getTranchenmodell() );
					List results = em.createQuery( "from Tranchenmodell" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from Tranche" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from X" ).getResultList();
					assertEquals( 0, results.size() );
					results = em.createQuery( "from Y" ).getResultList();
					assertEquals( 0, results.size() );

					results = em.createQuery( "from Preisregelung" ).getResultList();
					assertEquals( 1, results.size() );
				}
		);
	}

}
