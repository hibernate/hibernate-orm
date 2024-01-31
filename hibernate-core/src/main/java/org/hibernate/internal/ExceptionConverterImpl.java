/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.SQLException;

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
import org.hibernate.exception.LockAcquisitionException;
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

/**
 * @author Andrea Boriero
 */
public class ExceptionConverterImpl implements ExceptionConverter {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( ExceptionConverterImpl.class );

	private final SharedSessionContractImplementor sharedSessionContract;
	private final boolean isJpaBootstrap;

	public ExceptionConverterImpl(SharedSessionContractImplementor sharedSessionContract) {
		this.sharedSessionContract = sharedSessionContract;
		isJpaBootstrap = sharedSessionContract.getFactory().getSessionFactoryOptions().isJpaBootstrap();
	}

	@Override
	public RuntimeException convertCommitException(RuntimeException exception) {
		if ( isJpaBootstrap ) {
			try {
				//as per the spec we should roll back if commit fails
				sharedSessionContract.getTransaction().rollback();
			}
			catch (Exception re) {
				//swallow
			}
			return new RollbackException( "Error while committing the transaction", wrapCommitException( exception ) );
		}
		else {
			return exception;
		}
	}

	private Throwable wrapCommitException(RuntimeException exception) {
		if ( exception instanceof HibernateException ) {
			return convert( (HibernateException) exception);
		}
		else if ( exception instanceof PersistenceException ) {
			Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			if ( cause instanceof HibernateException ) {
				return convert( (HibernateException) cause );
			}
			else {
				return cause;
			}
		}
		else {
			return exception;
		}
	}

	@Override
	public RuntimeException convert(HibernateException exception, LockOptions lockOptions) {
		if ( exception instanceof StaleStateException ) {
			final PersistenceException converted = wrapStaleStateException( (StaleStateException) exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof LockAcquisitionException ) {
			final PersistenceException converted = wrapLockException( exception, lockOptions );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof LockingStrategyException ) {
			final PersistenceException converted = wrapLockException( exception, lockOptions );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.PessimisticLockException ) {
			final PersistenceException converted = wrapLockException( exception, lockOptions );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.QueryTimeoutException ) {
			final QueryTimeoutException converted = new QueryTimeoutException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof ObjectNotFoundException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueObjectException
					|| exception instanceof PersistentObjectException) {
			final EntityExistsException converted = new EntityExistsException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueResultException ) {
			final NonUniqueResultException converted = new NonUniqueResultException( exception.getMessage(), exception );
			rollbackIfNecessary( converted );
			return converted;
		}
		else if ( exception instanceof UnresolvableObjectException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( exception.getMessage(), exception );
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
				sharedSessionContract.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				log.unableToMarkForRollbackOnTransientObjectException( ne );
			}
			//Spec 3.2.3 Synchronization rules
			return new IllegalStateException( exception );
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
		if ( exception instanceof HibernateException ) {
			return convert( (HibernateException) exception );
		}
		else {
			sharedSessionContract.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	public RuntimeException convert(RuntimeException exception, LockOptions lockOptions) {
		if ( exception instanceof HibernateException ) {
			return convert( (HibernateException) exception, lockOptions );
		}
		else {
			sharedSessionContract.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	public JDBCException convert(SQLException e, String message) {
		return sharedSessionContract.getJdbcServices().getSqlExceptionHelper().convert( e, message );
	}

	protected PersistenceException wrapStaleStateException(StaleStateException exception) {
		if ( exception instanceof StaleObjectStateException ) {
			final StaleObjectStateException sose = (StaleObjectStateException) exception;
			final Object identifier = sose.getIdentifier();
			if ( identifier != null ) {
				try {
					final Object entity = sharedSessionContract.internalLoad( sose.getEntityName(), identifier, false, true);
					if ( entity instanceof Serializable ) {
						//avoid some user errors regarding boundary crossing
						return new OptimisticLockException( exception.getMessage(), exception, entity );
					}
					else {
						return new OptimisticLockException( exception.getMessage(), exception );
					}
				}
				catch (EntityNotFoundException enfe) {
					return new OptimisticLockException( exception.getMessage(), exception );
				}
			}
			else {
				return new OptimisticLockException( exception.getMessage(), exception );
			}
		}
		else {
			return new OptimisticLockException( exception.getMessage(), exception );
		}
	}

	protected PersistenceException wrapLockException(HibernateException exception, LockOptions lockOptions) {
		if ( exception instanceof OptimisticEntityLockException ) {
			final OptimisticEntityLockException lockException = (OptimisticEntityLockException) exception;
			return new OptimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
		}
		else if ( exception instanceof org.hibernate.exception.LockTimeoutException ) {
			return new LockTimeoutException( exception.getMessage(), exception, null );
		}
		else if ( exception instanceof PessimisticEntityLockException ) {
			final PessimisticEntityLockException lockException = (PessimisticEntityLockException) exception;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				return new LockTimeoutException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
			else {
				return new PessimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
		}
		else if ( exception instanceof org.hibernate.PessimisticLockException ) {
			final org.hibernate.PessimisticLockException lockException = (org.hibernate.PessimisticLockException) exception;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				return new LockTimeoutException( lockException.getMessage(), lockException, null );
			}
			else {
				return new PessimisticLockException( lockException.getMessage(), lockException, null );
			}
		}
		else {
			return new OptimisticLockException( exception );
		}
	}

	private void rollbackIfNecessary(PersistenceException e) {
		if ( !( e instanceof NoResultException
				|| e instanceof NonUniqueResultException
				|| e instanceof LockTimeoutException
				|| e instanceof QueryTimeoutException ) ) {
			try {
				sharedSessionContract.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				log.unableToMarkForRollbackOnPersistenceException( ne );
			}
		}
	}

}
