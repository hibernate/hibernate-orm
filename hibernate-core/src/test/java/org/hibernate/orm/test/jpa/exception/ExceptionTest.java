/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.exception;

import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {Music.class, Musician.class, Instrument.class})
public class ExceptionTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOptimisticLockingException(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		EntityManager em2 = scope.getEntityManagerFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			Music music = new Music();
			music.setName( "Old Country" );
			em.persist( music );
			em.getTransaction().commit();

			em2.getTransaction().begin();
			Music music2 = em2.find( Music.class, music.getId() );
			music2.setName( "HouseMusic" );
			em2.getTransaction().commit();

			em.getTransaction().begin();
			music.setName( "Rock" );
			try {
				em.flush();
				fail( "Should raise an optimistic lock exception" );
			}
			catch (OptimisticLockException e) {
				//success
				assertThat( e.getEntity() ).isEqualTo( music );
			}
			catch (Exception e) {
				fail( "Should raise an optimistic lock exception" );
			}
			finally {
				em.getTransaction().rollback();
				em.close();
			}
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			if ( em2.getTransaction().isActive() ) {
				em2.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
			em2.close();
		}
	}

	@Test
	public void testEntityNotFoundException(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		Music music = em.getReference( Music.class, -1 );
		try {
			music.getName();
			fail( "Non existent entity should raise an exception when state is accessed" );
		}
		catch ( EntityNotFoundException e ) {
			//"success"
		}
		finally {
			em.close();
		}
	}

	@Test
	@SkipForDialect(dialectClass = TiDBDialect.class, reason = "TiDB do not support FK violation checking")
	public void testConstraintViolationException(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Music music = new Music();
						music.setName( "Jazz" );
						entityManager.persist( music );
						Musician lui = new Musician();
						lui.setName( "Lui Armstrong" );
						lui.setFavouriteMusic( music );
						entityManager.persist( lui );
						entityManager.getTransaction().commit();
						try {
							entityManager.getTransaction().begin();
							String hqlDelete = "delete from Music where name = :name";
							entityManager.createQuery( hqlDelete ).setParameter( "name", "Jazz" ).executeUpdate();
							entityManager.getTransaction().commit();
							fail();
						}
						catch ( PersistenceException e ) {
							assertTrue( e instanceof ConstraintViolationException, "Should be a constraint violation" );
							entityManager.getTransaction().rollback();
						}
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
	@JiraKey( value = "HHH-4676" )
	public void testInterceptor(EntityManagerFactoryScope scope) {
		EntityManager em = scope.getEntityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Instrument instrument = new Instrument();
		instrument.setName( "Guitar" );
		try {
			em.persist( instrument );
			fail( "Commit should have failed." );
		}
		catch ( RuntimeException e ) {
			assertTrue( em.getTransaction().getRollbackOnly() );
			em.getTransaction().rollback();
		}
		finally {
			em.close();
		}
	}
}
