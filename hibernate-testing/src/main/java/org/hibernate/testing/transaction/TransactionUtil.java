/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.transaction;

import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TransactionUtil {
	private static final Logger log = Logger.getLogger( TransactionUtil.class );
	public static final String ACTION_COMPLETED_TXN = "Execution of action caused managed transaction to be completed";

	public static void inSession(SessionFactory sessionFactory, Consumer<Session> action) {
		log.trace( "#inSession(SF,action)" );

		final Session session = sessionFactory.openSession();
		try {
			log.trace( "Session opened, calling action" );
			action.accept( session );
			log.trace( "called action" );
		}
		finally {
			session.close();
			log.trace( "Session closed" );
		}
	}


	public static void inTransaction(SessionFactory factory, final Consumer<Session> action) {
		log.trace( "#inTransaction(factory, action)");

		inSession(
				factory,
				new Consumer<Session>() {
					@Override
					public void accept(Session session) {
						inTransaction( session, action );
					}
				}
		);
	}

	public static void inTransaction(Session session, Consumer<Session> action) {
		log.trace( "inTransaction(session,action)" );

		final Transaction txn = session.beginTransaction();
		log.trace( "Started transaction" );

		try {
			log.trace( "Calling action in txn" );
			action.accept( session );
			log.trace( "Called action - in txn" );

			if ( !txn.isActive() ) {
				throw new TransactionManagementException( ACTION_COMPLETED_TXN );
			}
		}
		catch (Exception e) {
			// an error happened in the action
			if ( ! txn.isActive() ) {
				log.warn( ACTION_COMPLETED_TXN, e );
			}
			else {
				log.trace( "Rolling back transaction due to action error" );
				try {
					txn.rollback();
					log.trace( "Rolled back transaction due to action error" );
				}
				catch (Exception inner) {
					log.trace( "Rolling back transaction due to action error failed; throwing original error" );
				}
			}

			if ( e instanceof RuntimeException ) {
				throw (RuntimeException) e;
			}
			else {
				throw new RuntimeException( e );
			}
		}

		// action completed with no errors - attempt to commit the transaction allowing
		// 		any RollbackException to propagate.  Note that when we get here we know the
		//		txn is active

		log.trace( "Committing transaction after successful action execution" );
		try {
			txn.commit();
			log.trace( "Committing transaction after successful action execution - success" );
		}
		catch (Exception e) {
			log.trace( "Committing transaction after successful action execution - failure" );

			if ( e instanceof RuntimeException ) {
				throw (RuntimeException) e;
			}
			else {
				throw new RuntimeException( e );
			}
		}
	}

	private static class TransactionManagementException extends RuntimeException {
		public TransactionManagementException(String message) {
			super( message );
		}
	}
}
