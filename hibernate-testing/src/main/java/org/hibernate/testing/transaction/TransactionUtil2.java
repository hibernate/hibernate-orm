/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.transaction;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class TransactionUtil2 {
	private static final Logger log = Logger.getLogger( TransactionUtil2.class );
	public static final String ACTION_COMPLETED_TXN = "Execution of action caused managed transaction to be completed";


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in/from Session

	public static void inSession(SessionFactoryImplementor sfi, Consumer<SessionImplementor> action) {
		log.trace( "#inSession(SF,action)" );

		try (SessionImplementor session = sfi.openSession()) {
			log.trace( "Session opened, calling action" );
			action.accept( session );
			log.trace( "called action" );
		}
		finally {
			log.trace( "Session closed (AutoCloseable)" );
		}
	}

	public static <R> R fromSession(SessionFactoryImplementor sfi, Function<SessionImplementor,R> action) {
		log.trace( "#fromSession(SF,action)" );

		try (SessionImplementor session = sfi.openSession()) {
			log.trace( "Session opened, calling action" );
			return action.apply( session );
		}
		finally {
			log.trace( "Session closed (AutoCloseable)" );
		}
	}

	public static <R> R inSessionReturn(SessionFactoryImplementor sfi, Function<SessionImplementor,R> action) {
		log.trace( "#inSession(SF,action)" );

		R result = null;
		try (SessionImplementor session = sfi.openSession()) {
			log.trace( "Session opened, calling action" );
			result = action.apply( session );
			log.trace( "called action" );
		}
		finally {
			log.trace( "Session closed (AutoCloseable)" );
		}
		return result;
	}


	public static void inTransaction(SessionFactoryImplementor factory, Consumer<SessionImplementor> action) {
		log.trace( "#inTransaction(factory, action)");

		inSession(
				factory,
				session -> inTransaction( session, action )
		);
	}
	public static <R> R fromTransaction(SessionFactoryImplementor factory, Function<SessionImplementor,R> action) {
		log.trace( "#fromTransaction(factory, action)");

		return fromSession(
				factory,
				session -> fromTransaction( session, action )
		);
	}

	public static void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
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

			throw e;
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
			throw e;
		}
	}

	public static <R> R fromTransaction(SessionImplementor session, Function<SessionImplementor,R> action) {
		log.trace( "fromTransaction(session,action)" );

		final Transaction txn = session.beginTransaction();
		log.trace( "Started transaction" );

		final R result;
		try {
			log.trace( "Calling action in txn" );
			result = action.apply( session );
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

			throw e;
		}

		assert result != null;

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
			throw e;
		}

		return result;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// in/from StatelessSession

	public static void inStatelessSession(SessionFactoryImplementor sfi, Consumer<StatelessSession> action) {
		log.trace( "#inSession(SF,action)" );

		try (StatelessSession session = sfi.openStatelessSession()) {
			log.trace( "StatelessSession opened, calling action" );
			action.accept( session );
			log.trace( "called action" );
		}
		finally {
			log.trace( "Session closed (AutoCloseable)" );
		}
	}


	public static void inStatelessTransaction(SessionFactoryImplementor factory, Consumer<StatelessSession> action) {
		log.trace( "#inTransaction(factory, action)");

		inStatelessSession(
				factory,
				session -> inStatelessTransaction( session, action )
		);
	}

	public static void inStatelessTransaction(StatelessSession session, Consumer<StatelessSession> action) {
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

			throw e;
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
			throw e;
		}
	}



	private static class TransactionManagementException extends RuntimeException {
		public TransactionManagementException(String message) {
			super( message );
		}
	}
}
