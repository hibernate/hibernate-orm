/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.locking;

import java.util.concurrent.TimeoutException;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.LockAcquisitionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Make sure that directly specifying lock modes, even though deprecated, continues to work until removed.
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-5275")
public class LockModeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return  new Class[] { A.class };
	}

	@Before
	public void createData() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.save( new A( "it" ) );
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void cleanupData() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.createQuery( "delete A" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

//	@Test( timeout = 6000 )
	@Test
	@SuppressWarnings( {"deprecation"})
	public void testLoading() {
		// open a session, begin a transaction and lock row
		Session s1 = sessionFactory().openSession();
		s1.beginTransaction();
		try {
			A it = (A) s1.get( A.class, 1, LockMode.PESSIMISTIC_WRITE );
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		}
		finally {
			s1.getTransaction().commit();
			s1.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-5275" )
	public void testLegacyCriteria() {
		// open a session, begin a transaction and lock row
		Session s1 = sessionFactory().openSession();
		s1.beginTransaction();
		try {
			A it = (A) s1.createCriteria( A.class )
					.setLockMode( LockMode.PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		}
		finally {
			s1.getTransaction().commit();
			s1.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-5275" )
	public void testLegacyCriteriaAliasSpecific() {
		// open a session, begin a transaction and lock row
		Session s1 = sessionFactory().openSession();
		s1.beginTransaction();
		try {
			A it = (A) s1.createCriteria( A.class )
					.setLockMode( "this", LockMode.PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		}
		finally {
			s1.getTransaction().commit();
			s1.close();
		}
	}

	@Test
	@FailureExpected( jiraKey = "HHH-5275" )
	public void testQuery() {
		// open a session, begin a transaction and lock row
		Session s1 = sessionFactory().openSession();
		s1.beginTransaction();
		try {
			A it = (A) s1.createQuery( "from A a" )
					.setLockMode( "a", LockMode.PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		}
		finally {
			s1.getTransaction().commit();
			s1.close();
		}
	}

	@Test
	public void testQueryUsingLockOptions() {
		// todo : need an association here to make sure the alias-specific lock modes are applied correctly
		Session s1 = sessionFactory().openSession();
		s1.beginTransaction();
		s1.createQuery( "from A a" )
				.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
				.uniqueResult();
		s1.createQuery( "from A a" )
				.setLockOptions( new LockOptions().setAliasSpecificLockMode( "a", LockMode.PESSIMISTIC_WRITE ) )
				.uniqueResult();
		s1.getTransaction().commit();
		s1.close();
	}

	private void nowAttemptToUpdateRow() {
		// here we just need to open a new connection (database session and transaction) and make sure that
		// we are not allowed to acquire exclusive locks to that row and/or write to that row.  That may take
		// one of two forms:
		//		1) either the get-with-lock or the update fails immediately with a sql error
		//		2) either the get-with-lock or the update blocks indef (in real world, it would block
		//			until the txn in the calling method completed.
		// To be able to cater to the second type, we run this block in a separate thread to be able to "time it out"

		try {
			new TimedExecutor( 10*1000, 1*1000 ).execute(
					new Executable() {
						Session s;

						@Override
						public void execute() {
							s = sessionFactory().openSession();
							s.beginTransaction();
							try {
								// load with write lock to deal with databases that block (wait indefinitely) direct attempts
								// to write a locked row
								A it = (A) s.get(
										A.class,
										1,
										new LockOptions( LockMode.PESSIMISTIC_WRITE ).setTimeOut( LockOptions.NO_WAIT )
								);
								it.setValue( "changed" );
								s.flush();
								fail( "Pessimistic lock not obtained/held" );
							}
							catch ( Exception e ) {
								// grr, exception can be any number of types based on database
								// 		see HHH-6887
								if ( LockAcquisitionException.class.isInstance( e )
										|| GenericJDBCException.class.isInstance( e )
										|| PessimisticLockException.class.isInstance( e ) ) {
									// "ok"
								}
								else {
									fail( "Unexpected error type testing pessimistic locking : " + e.getClass().getName() );
								}
							}
							finally {
								shutDown();
							}
						}

						private void shutDown() {
							try {
								s.getTransaction().rollback();
								s.close();
							}
							catch (Exception ignore) {
							}
							s = null;
						}

						@Override
						public void forceStop() {
							s.cancelQuery();
							shutDown();
						}
					}
			);
		}
		catch (TimeoutException e) {
			// timeout is ok, see rule #2 above
		}
	}

	interface Executable {
		public void execute();
		public void forceStop();
	}

	class TimedExecutor {
		private final long timeOut;
		private final int checkMilliSeconds;

		TimedExecutor(long timeOut) {
			this( timeOut, 1000 );
		}

		TimedExecutor(long timeOut, int checkMilliSeconds) {
			this.timeOut = timeOut;
			this.checkMilliSeconds = checkMilliSeconds;
		}

		public void execute(Executable executable) throws TimeoutException {
			final ExecutableAdapter adapter = new ExecutableAdapter( executable );
			final Thread separateThread = new Thread( adapter );
			separateThread.start();

			int runningTime = 0;
			do {
				if ( runningTime > timeOut ) {
					try {
						executable.forceStop();
					}
					catch (Exception ignore) {
					}
					throw new TimeoutException();
				}
				try {
					Thread.sleep( checkMilliSeconds );
					runningTime += checkMilliSeconds;
				}
				catch (InterruptedException ignore) {
				}
			} while ( !adapter.isDone() );
		}
	}

	class ExecutableAdapter implements Runnable {
		private final Executable executable;
		private boolean isDone;

		ExecutableAdapter(Executable executable) {
			this.executable = executable;
		}

		public boolean isDone() {
			return isDone;
		}

		@Override
		public void run() {
			isDone = false;
			try {
				executable.execute();
			}
			finally {
				isDone = true;
			}
		}
	}
}
