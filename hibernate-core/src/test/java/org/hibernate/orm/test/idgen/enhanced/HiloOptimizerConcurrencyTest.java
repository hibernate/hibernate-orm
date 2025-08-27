/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.enhanced;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Demonstrates HHH-3628 issue with rolling over buckets in HiLoOptimizer. There are
 * two variants of the test which do pretty much the same thing - one in sessions in
 * parallel threads and one simply performing actions in sequence in two sessions.
 * Possibly the threaded version is somewhat redundant given that the simpler test
 * also exhibits the problem.
 *
 * @author Richard Barnes 4 May 2016
 */
@JiraKey(value = "HHH-3628")
public class HiloOptimizerConcurrencyTest extends BaseNonConfigCoreFunctionalTestCase {

	private boolean createSchema = true;

	private ExecutorService executor = Executors.newFixedThreadPool( 2 );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				HibPerson.class
		};
	}

	@Test
	public void testTwoSessionsParallelGeneration() {
		createSchema = true;

		StandardServiceRegistry serviceRegistry = serviceRegistry();
		SessionFactoryImplementor sessionFactory = sessionFactory();

		try {
			final Session session1 = openSession();

			try {
				session1.beginTransaction();
				HibPerson p = new HibPerson();
				session1.persist( p );
			}
			finally {
				session1.getTransaction().commit();
			}

			createSchema = false;
			buildResources();

			final Session session2 = openSession();
			try {
				session2.beginTransaction();
				HibPerson p = new HibPerson();
				session2.persist( p );
			}
			finally {
				session2.getTransaction().commit();
			}

			final List<Throwable> errs = new CopyOnWriteArrayList<>();

			CountDownLatch firstLatch = new CountDownLatch( 1 );
			CountDownLatch secondLatch = new CountDownLatch( 1 );

			Callable<Void> callable1 = () -> {
				try {
					for ( int i = 2; i < 6; i++ ) {
						try {
							session1.beginTransaction();
							HibPerson p = new HibPerson();
							session1.persist( p );
						}
						finally {
							session1.getTransaction().commit();
						}
					}
					firstLatch.countDown();
					secondLatch.await();
					try {
						session1.beginTransaction();
						HibPerson p = new HibPerson();
						session1.persist( p );
					}
					finally {
						session1.getTransaction().commit();
					}
				}
				catch (Throwable t) {
					errs.add( t );
				}

				return null;
			};

			Callable<Void> callable2 = () -> {
				try {
					firstLatch.await();
					secondLatch.countDown();
					try {
						session2.beginTransaction();
						HibPerson p = new HibPerson();
						session2.persist( p );
					}
					finally {
						session2.getTransaction().commit();
					}
				}
				catch (Throwable t) {
					errs.add( t );
				}

				return null;
			};

			executor.invokeAll( Arrays.asList(
				callable1, callable2
			) , 30, TimeUnit.SECONDS).forEach( c -> {
				try {
					c.get();
				}
				catch (InterruptedException|ExecutionException e) {
					Thread.interrupted();
					fail(e.getMessage());
				}
			});

			for(Throwable ex : errs) {
				fail(ex.getMessage());
			}
		}
		catch (InterruptedException e) {
			fail(e.getMessage());
		}
		finally {
			releaseResources( serviceRegistry, sessionFactory );
		}
	}

	@Test
	public void testTwoSessionsSerialGeneration() {
		createSchema = true;
		rebuildSessionFactory();
		StandardServiceRegistry serviceRegistry = serviceRegistry();
		SessionFactoryImplementor sessionFactory = sessionFactory();

		try {
			final Session session1 = openSession();

			try {
				session1.beginTransaction();
				HibPerson p = new HibPerson();
				session1.persist( p );
			}
			finally {
				session1.getTransaction().commit();
			}

			createSchema = false;
			buildResources();

			final Session session2 = openSession();
			session2.beginTransaction();
			try {
				HibPerson p = new HibPerson();
				session2.persist( p );
			}
			finally {
				session2.getTransaction().commit();
			}

			for ( int i = 2; i < 6; i++ ) {
				session1.beginTransaction();
				try {
					HibPerson p = new HibPerson();
					session1.persist( p );
				}
				finally {
					session1.getTransaction().commit();
				}
			}
			session2.beginTransaction();
			try {
				HibPerson p = new HibPerson();
				session2.persist( p );
			}
			finally {
				session2.getTransaction().commit();
			}
			session1.beginTransaction();
			try {
				HibPerson p = new HibPerson();
				session1.persist( p );
			}
			finally {
				session1.getTransaction().commit();
			}
		}
		catch (ConstraintViolationException cve) {
			fail( "ConstraintViolationException: " + cve.getMessage() );
		}
		finally {
			releaseResources( serviceRegistry, sessionFactory );
		}
	}

	private void releaseResources(StandardServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
		if ( sessionFactory != null ) {
			try {
				sessionFactory.close();
			}
			catch (Exception e) {
				fail( "Unable to release SessionFactory : " + e.getMessage() );
			}
		}

		if ( serviceRegistry != null ) {
			try {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
			catch (Exception e) {
				fail( "Unable to release StandardServiceRegistry : " + e.getMessage() );
			}
		}
	}

	@Override
	protected boolean createSchema() {
		return createSchema;
	}

	@Entity(name = "HibPerson")
	public static class HibPerson {

		@Id
		@GeneratedValue(generator = "HIB_TGEN")
		@GenericGenerator(name = "HIB_TGEN", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = {
				@Parameter(name = "table_name", value = "HIB_TGEN"),
				@Parameter(name = "prefer_entity_table_as_segment_value", value = "true"),
				@Parameter(name = "optimizer", value = "hilo"),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "5")
		})
		private long id = -1;

		public HibPerson() {
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

	}
}
