/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {
				Race.class,
				Competitor.class,
				Music.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "0") }
)
public class RemoveTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRemove(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Race race = new Race();
						race.competitors.add( new Competitor() );
						race.competitors.add( new Competitor() );
						race.competitors.add( new Competitor() );
						entityManager.getTransaction().begin();
						entityManager.persist( race );
						entityManager.flush();
						entityManager.remove( race );
						entityManager.flush();
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testRemoveAndFind(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						Race race = new Race();
						entityManager.getTransaction().begin();
						entityManager.persist( race );
						entityManager.remove( race );
						Assertions.assertNull( entityManager.find( Race.class, race.id ) );
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testUpdatedAndRemove(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityManager em2 = scope.getEntityManagerFactory().createEntityManager();
		try {
			Music music = new Music();
			music.setName( "Classical" );
			em.getTransaction().begin();
			em.persist( music );
			em.getTransaction().commit();
			em.clear();

			try {
				em2.getTransaction().begin();
				//read music from 2nd EM
				music = em2.find( Music.class, music.getId() );
			}
			catch (Exception e) {
				em2.getTransaction().rollback();
				em2.close();
				throw e;
			}

			//change music
			em = scope.getEntityManagerFactory().createEntityManager();
			em.getTransaction().begin();
			em.find( Music.class, music.getId() ).setName( "Rap" );
			em.getTransaction().commit();

			try {
				em2.remove( music ); //remove changed music
				em2.flush();
				Assertions.fail( "should have an optimistic lock exception" );
			}
			catch( OptimisticLockException e ) {
				//"success"
			}
			finally {
				em2.getTransaction().rollback();
				em2.close();
			}

			//clean
			em.getTransaction().begin();
			em.remove( em.find( Music.class, music.getId() ) );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
		finally {
			em.close();
			em2.close();
		}
	}
}
