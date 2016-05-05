/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.table.concurrent;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.Semaphore;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Demonstrates HHH-3628 issue with rolling over buckets in HiLoOptimizer. There are
 * two variants of the test which do pretty much the same thing - one in sessions in 
 * parallel threads and one simply performing actions in sequence in two sessions.
 * Possibly the threaded version is somewhat redundant given that the simpler test
 * also exhibits the problem.  
 * 
 * @author Richard Barnes 4 May 2016
 */
public class HHH3628ConcurrentTest extends BaseNonConfigCoreFunctionalTestCase {

	private boolean createSchema = true;

	@Test
	public void testTwoSessionsParallelGeneration() {
		createSchema = true;
		StandardServiceRegistry serviceRegistry = serviceRegistry();
		SessionFactoryImplementor sessionFactory = sessionFactory();
		final Session session1 = openSession();
		Transaction tx = session1.beginTransaction();
		HibPerson p = new HibPerson();
		session1.save( p );
		tx.commit();

		createSchema = false;
		buildResources();

		final Session session2 = openSession();
		tx = session2.beginTransaction();
		p = new HibPerson();
		session2.save( p );
		tx.commit();

		final Throwable[] errs1 = new Throwable[1];
		final Throwable[] errs2 = new Throwable[1];
		final Semaphore baton = new Semaphore( 0, true );

		Runnable r1 = new Runnable() {

			public void run() {
				try {
					Transaction tx1 = null;
					for ( int i = 2; i < 6; i++ ) {
						tx1 = session1.beginTransaction();
						System.out.print( i );
						HibPerson p = new HibPerson();
						session1.save( p );
						tx1.commit();
					}
					baton.release();
					baton.acquire();
					tx1 = session1.beginTransaction();
					HibPerson p = new HibPerson();
					session1.save( p );
					tx1.commit();
				}
				catch (Throwable t) {
					errs1[0] = t;
					return;
				}
			}
		};

		Runnable r2 = new Runnable() {

			public void run() {
				try {
					baton.acquire();
					Transaction tx2 = session2.beginTransaction();
					HibPerson p = new HibPerson();
					session2.save( p );
					tx2.commit();
				}
				catch (Throwable t) {
					errs2[0] = t;
					return;
				}
				finally {
					baton.release();
				}
			}
		};

		Thread t1 = new Thread( r1 );
		Thread t2 = new Thread( r2 );

		t1.start();
		t2.start();

		try {
			t1.join();
			t2.join();
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if ( errs1[0] != null ) {
			errs1[0].printStackTrace();
		}
		if ( errs2[0] != null ) {
			errs2[0].printStackTrace();
		}
		assertNull( errs1[0] );
		assertNull( errs2[0] );

		releaseResources( serviceRegistry, sessionFactory );
	}

	@Test
	public void testTwoSessionsSerialGeneration() {
		createSchema = true;
		rebuildSessionFactory();
		StandardServiceRegistry serviceRegistry = serviceRegistry();
		SessionFactoryImplementor sessionFactory = sessionFactory();
		final Session session1 = openSession();
		Transaction tx = session1.beginTransaction();
		HibPerson p = new HibPerson();
		session1.save( p );
		tx.commit();

		createSchema = false;
		buildResources();

		final Session session2 = openSession();
		tx = session2.beginTransaction();
		p = new HibPerson();
		session2.save( p );
		tx.commit();

		try {
			Transaction tx1 = null;
			for ( int i = 2; i < 6; i++ ) {
				tx1 = session1.beginTransaction();
				System.out.print( i );
				p = new HibPerson();
				session1.save( p );
				tx1.commit();
			}
			Transaction tx2 = session2.beginTransaction();
			p = new HibPerson();
			session2.save( p );
			tx2.commit();
			tx1 = session1.beginTransaction();
			p = new HibPerson();
			session1.save( p );
			tx1.commit();
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
				System.err.println( "Unable to release SessionFactory : " + e.getMessage() );
				e.printStackTrace();
			}
		}
		sessionFactory = null;

		if ( serviceRegistry != null ) {
			try {
				StandardServiceRegistryBuilder.destroy( serviceRegistry );
			}
			catch (Exception e) {
				System.err.println( "Unable to release StandardServiceRegistry : " + e.getMessage() );
				e.printStackTrace();
			}
		}
		serviceRegistry = null;
	}

	@Override
	protected boolean createSchema() {
		return createSchema;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		Class[] clazz = { HibPerson.class };
		return clazz;
	}
}
