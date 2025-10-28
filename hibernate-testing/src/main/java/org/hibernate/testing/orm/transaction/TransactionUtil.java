/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.transaction;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.EntityManager;

import jakarta.persistence.QueryTimeoutException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SharedSessionContract;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.AsyncExecutor;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class TransactionUtil {
	private static final Logger log = Logger.getLogger( TransactionUtil.class );

	public static void inTransaction(SessionFactoryImplementor sessionFactory, Consumer<SessionImplementor> action) {
		try (var session = sessionFactory.openSession()) {
			inTransaction( session, action );
		}
	}

	public static void inTransaction(SessionImplementor session, Consumer<SessionImplementor> action) {
		wrapInTransaction( session, session, action );
	}

	public static void inTransaction(EntityManager entityManager, Consumer<EntityManager> action) {
		wrapInTransaction( (SharedSessionContract) entityManager, entityManager, action );
	}

	public static void inTransaction(StatelessSessionImplementor session, Consumer<StatelessSessionImplementor> action) {
		wrapInTransaction( session, session, action );
	}

	public static <R> R fromTransaction(SessionFactoryImplementor sessionFactory, Function<SessionImplementor, R> action) {
		try (var session = sessionFactory.openSession()) {
			return fromTransaction( session, action );
		}
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

	public static void deleteRow(SessionFactoryScope factoryScope, String tableName, boolean expectingToBlock) {
		try {
			AsyncExecutor.executeAsync( 2, TimeUnit.SECONDS, () -> {
				factoryScope.inTransaction( (session) -> {
					final String sql = String.format( "delete from %s", tableName );
					//noinspection deprecation
					session.createNativeQuery( sql ).executeUpdate();
					if ( expectingToBlock ) {
						fail( "Expecting `delete from " + tableName + "` to block due to locks" );
					}
				} );
			} );
		}
		catch (AsyncExecutor.TimeoutException expected) {
			if ( !expectingToBlock ) {
				fail( "Expecting update to " + tableName + " to succeed, but failed due to async timeout (presumably due to locks)", expected );
			}
		}
		catch (RuntimeException re) {
			if ( re.getCause() instanceof jakarta.persistence.LockTimeoutException
				|| re.getCause() instanceof org.hibernate.exception.LockTimeoutException
				|| re.getCause() instanceof QueryTimeoutException ) {
				if ( !expectingToBlock ) {
					fail( "Expecting update to " + tableName + " to succeed, but failed due to async timeout (presumably due to locks)", re.getCause() );
				}
			}
			else if ( re.getCause() instanceof ConstraintViolationException cve ) {
				throw cve;
			}
			else {
				throw re;
			}
		}
	}

	public static void assertRowLock(SessionFactoryScope factoryScope, String tableName, String columnName, String idColumn, Number id, boolean expectingToBlock) {
		final Dialect dialect = factoryScope.getSessionFactory().getJdbcServices().getDialect();
		final boolean skipLocked = dialect.getLockingSupport().getMetadata().supportsSkipLocked();
		// SQL Server readpast hint doesn't really work unfortunately
		if ( skipLocked && !( dialect instanceof SQLServerDialect ) ) {
			factoryScope.inTransaction( (session) -> {
				final String baseSql = String.format( "select %s from %s t where %s=%s", columnName, tableName, idColumn, id );
				final String sql = dialect.applyLocksToSql(
						baseSql,
						new LockOptions( LockMode.UPGRADE_SKIPLOCKED ),
						Map.of( "t", new String[0] )
				);
				final int resultSize = session.createNativeQuery( sql ).getResultList().size();
				if ( expectingToBlock && resultSize > 0 ) {
					fail( "Expecting update to " + tableName + " to block dues to locks" );
				}
				else if ( !expectingToBlock && resultSize == 0 ) {
					fail( "Unexpected lock found on " + tableName );
				}
			} );
		}
		else {
			try {
				AsyncExecutor.executeAsync( 2, TimeUnit.SECONDS, () -> {
					factoryScope.inTransaction( (session) -> {
						final String sql = String.format( "update %s set %s = null", tableName, columnName );
						//noinspection deprecation
						session.createNativeQuery( sql ).executeUpdate();
						if ( expectingToBlock ) {
							fail( "Expecting update to " + tableName + " to block dues to locks" );
						}
					} );
				} );
			}
			catch (AsyncExecutor.TimeoutException expected) {
				if ( !expectingToBlock ) {
					fail( "Expecting update to " + tableName + " to succeed, but failed due to async timeout (presumably due to locks)", expected );
				}
			}
			catch (RuntimeException re) {
				if ( re.getCause() instanceof jakarta.persistence.LockTimeoutException
					|| re.getCause() instanceof org.hibernate.exception.LockTimeoutException
					|| re.getCause() instanceof QueryTimeoutException ) {
					if ( !expectingToBlock ) {
						fail( "Expecting update to " + tableName + " to succeed, but failed due to async timeout (presumably due to locks)", re.getCause() );
					}
				}
				else if ( re.getCause() instanceof ConstraintViolationException cve ) {
					throw cve;
				}
				else {
					throw re;
				}
			}
		}
	}
}
