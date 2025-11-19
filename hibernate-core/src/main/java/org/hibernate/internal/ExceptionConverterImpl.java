/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PersistentObjectException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.exception.TransactionSerializationException;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SyntaxException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import jakarta.persistence.RollbackException;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Andrea Boriero
 */
// Extended by Hibernate Reactive
public class ExceptionConverterImpl implements ExceptionConverter {

	private final SharedSessionContractImplementor session;
	private final boolean isJpaBootstrap;

	public ExceptionConverterImpl(SharedSessionContractImplementor session) {
		this.session = session;
		isJpaBootstrap = session.getFactory().getSessionFactoryOptions().isJpaBootstrap();
	}

	@Override
	public RuntimeException convertCommitException(RuntimeException exception) {
		if ( isJpaBootstrap ) {
			try {
				//as per the spec we should roll back if commit fails
				session.getTransaction().rollback();
			}
			catch (Exception re) {
				//swallow
			}
			return new RollbackException( "Error while committing the transaction [" + exception.getMessage() + "]",
					exception instanceof HibernateException hibernateException
							? convert( hibernateException )
							: exception );
		}
		else {
			return exception;
		}
	}

	@Override
	public RuntimeException convert(HibernateException exception, LockOptions lockOptions) {
		if ( exception instanceof StaleStateException staleStateException ) {
			final var converted = wrapStaleStateException( staleStateException );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.PessimisticLockException pessimisticLockException ) {
			final var converted = wrapLockException( pessimisticLockException, lockOptions );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof LockingStrategyException lockingStrategyException ) {
			final var converted = wrapLockException( lockingStrategyException, lockOptions );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof SnapshotIsolationException ) {
			return new OptimisticLockException( exception.getMessage(), exception );
		}
		else if ( exception instanceof org.hibernate.QueryTimeoutException ) {
			final var converted = new QueryTimeoutException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof ObjectNotFoundException ) {
			final var converted = new EntityNotFoundException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueObjectException
					|| exception instanceof PersistentObjectException) {
			final var converted = new EntityExistsException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueResultException ) {
			final var converted = new NonUniqueResultException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof UnresolvableObjectException ) {
			final var converted = new EntityNotFoundException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof SyntaxException
				|| exception instanceof SemanticException
				|| exception instanceof IllegalQueryOperationException) {
			return new IllegalArgumentException( exception );
		}
		else if ( exception instanceof MultipleBagFetchException ) {
			return new IllegalArgumentException( exception );
		}
		else if ( exception instanceof TransientObjectException ) {
			try {
				session.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				CORE_LOGGER.unableToMarkForRollbackOnTransientObjectException( ne );
			}
			//Spec 3.2.3 Synchronization rules
			return new IllegalStateException( exception );
		}
		else if ( exception instanceof TransactionSerializationException ) {
			final var converted = new RollbackException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else {
			rollbackIfNecessary( exception );
			return exception;
		}
	}

	@Override
	public RuntimeException convert(HibernateException exception) {
		return convert( exception, null );
	}

	@Override
	public RuntimeException convert(RuntimeException exception) {
		if ( exception instanceof HibernateException hibernateException ) {
			return convert( hibernateException );
		}
		else {
			session.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	public RuntimeException convert(RuntimeException exception, LockOptions lockOptions) {
		if ( exception instanceof HibernateException hibernateException ) {
			return convert( hibernateException, lockOptions );
		}
		else {
			session.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	public JDBCException convert(SQLException e, String message) {
		return session.getJdbcServices().getSqlExceptionHelper().convert( e, message );
	}

	protected PersistenceException wrapStaleStateException(StaleStateException exception) {
		final String message = exception.getMessage();
		if ( exception instanceof StaleObjectStateException staleStateException ) {
			final Object identifier = staleStateException.getIdentifier();
			final String entityName = staleStateException.getEntityName();
			if ( identifier != null ) {
				try {
					final Object entity = session.internalLoad( entityName, identifier, false, true );
					if ( entity instanceof Serializable ) { // avoid some user errors regarding boundary crossing
						return new OptimisticLockException( message, exception, entity );
					}
				}
				catch (EntityNotFoundException entityNotFoundException) {
					// swallow it;
				}
			}
		}
		return new OptimisticLockException( message, exception );
	}

	protected PersistenceException wrapLockException(LockingStrategyException exception, LockOptions lockOptions) {
		final String message = exception.getMessage();
		final Object entity = exception.getEntity();
		if ( exception instanceof OptimisticEntityLockException lockException ) {
			return new OptimisticLockException( message, lockException, entity );
		}
		else if ( exception instanceof PessimisticEntityLockException lockException ) {
			// assume lock timeout occurred if a timeout or NO WAIT was specified
			return lockOptions != null && lockOptions.getTimeout().milliseconds() > -1
					? new LockTimeoutException( message, lockException, entity )
					: new PessimisticLockException( message, lockException, entity );
		}
		else {
			throw new AssertionFailure( "Unrecognized exception type" );
		}
	}

	protected PersistenceException wrapLockException(org.hibernate.PessimisticLockException exception, LockOptions lockOptions) {
		final String message = exception.getMessage();
		if ( exception instanceof org.hibernate.exception.LockTimeoutException ) {
			return new LockTimeoutException( message, exception );
		}
		else {
			// assume lock timeout occurred if a timeout or NO WAIT was specified
			return lockOptions != null && lockOptions.getTimeout().milliseconds() > -1
					? new LockTimeoutException( message, exception )
					: new PessimisticLockException( message, exception );
		}
	}

	private void rollbackIfNecessary(PersistenceException persistenceException) {
		if ( !isNonRollbackException( persistenceException ) ) {
			try {
				session.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				CORE_LOGGER.unableToMarkForRollbackOnPersistenceException( ne );
			}
		}
	}

	/**
	 * Is this a special exception type explicitly exempted from
	 * forced rollbacks by the JPA specification itself?
	 */
	private static boolean isNonRollbackException(PersistenceException persistenceException) {
		return persistenceException instanceof NoResultException
			|| persistenceException instanceof NonUniqueResultException
			|| persistenceException instanceof LockTimeoutException
			|| persistenceException instanceof QueryTimeoutException;
	}

}
