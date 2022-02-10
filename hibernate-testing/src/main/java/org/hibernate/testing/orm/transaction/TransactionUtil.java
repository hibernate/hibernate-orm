/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.transaction;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;

import org.jboss.logging.Logger;

import javax.persistence.EntityManager;

public abstract class TransactionUtil {
	private static final Logger log = Logger.getLogger( TransactionUtil.class );

	public static void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		wrapInTransaction( session, session, action );
	}

	public static void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
		wrapInTransaction( (SharedSessionContract) entityManager, entityManager, action );
	}

	public static void inTransaction(StatelessSession session, Consumer<StatelessSession> action) {
		wrapInTransaction( session, session, action );
	}

	public static <R> R fromTransaction(SessionImplementor session, Function<SessionImplementor, R> action) {
		return wrapInTransaction( session, session, action );
	}

	public static <R> R fromTransaction(EntityManager entityManager, Function<EntityManager, R> action) {
		return wrapInTransaction( (SharedSessionContract) entityManager, entityManager, action );
	}

	private static <T> void wrapInTransaction(SharedSessionContract session, T actionInput, Consumer<T> action) {
		final Transaction txn = session.beginTransaction();
		log.trace( "Started transaction" );

		try {
			log.trace( "Calling action in txn" );
			action.accept( actionInput );
			log.trace( "Called action - in txn" );

			if ( !txn.getRollbackOnly() ) {
				log.trace( "Committing transaction" );
				txn.commit();
				log.trace( "Committed transaction" );
			}
			else {
				try {
					log.trace( "Rollback transaction marked for rollback only" );
					txn.rollback();
				}
				catch (Exception e) {
					log.error( "Rollback failure", e );
				}
			}
		}
		catch (Exception e) {
			log.tracef(
					"Error calling action: %s (%s) - rolling back",
					e.getClass().getName(),
					e.getMessage()
			);
			try {
				txn.rollback();
			}
			catch (Exception ignore) {
				log.trace( "Was unable to roll back transaction" );
				// really nothing else we can do here - the attempt to
				//		rollback already failed and there is nothing else
				// 		to clean up.
			}

			throw e;
		}
		catch (AssertionError t) {
			try {
				txn.rollback();
			}
			catch (Exception ignore) {
				log.trace( "Was unable to roll back transaction" );
				// really nothing else we can do here - the attempt to
				//		rollback already failed and there is nothing else
				// 		to clean up.
			}
			throw t;
		}
	}


	private static <T, R> R wrapInTransaction(SharedSessionContract session, T actionInput, Function<T, R> action) {
		log.trace( "Started transaction" );
		Transaction txn = session.beginTransaction();
		try {
			log.trace( "Calling action in txn" );
			final R result = action.apply( actionInput );
			log.trace( "Called action - in txn" );

			log.trace( "Committing transaction" );
			txn.commit();
			log.trace( "Committed transaction" );

			return result;
		}
		catch (Exception e) {
			log.tracef(
					"Error calling action: %s (%s) - rolling back",
					e.getClass().getName(),
					e.getMessage()
			);
			try {
				txn.rollback();
			}
			catch (Exception ignore) {
				log.trace( "Was unable to roll back transaction" );
				// really nothing else we can do here - the attempt to
				//		rollback already failed and there is nothing else
				// 		to clean up.
			}

			throw e;
		}
		catch (AssertionError t) {
			try {
				txn.rollback();
			}
			catch (Exception ignore) {
				log.trace( "Was unable to roll back transaction" );
				// really nothing else we can do here - the attempt to
				//		rollback already failed and there is nothing else
				// 		to clean up.
			}
			throw t;
		}
	}

}
