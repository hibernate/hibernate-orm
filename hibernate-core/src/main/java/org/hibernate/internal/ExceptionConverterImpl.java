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
import org.hibernate.QueryException;
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
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.ParsingException;

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
	public RuntimeException convertCommitException(RuntimeException e) {
		if ( isJpaBootstrap ) {
			Throwable wrappedException;
			if ( e instanceof HibernateException ) {
				wrappedException = convert( (HibernateException) e );
			}
			else if ( e instanceof PersistenceException ) {
				Throwable cause = e.getCause() == null ? e : e.getCause();
				if ( cause instanceof HibernateException ) {
					wrappedException = convert( (HibernateException) cause );
				}
				else {
					wrappedException = cause;
				}
			}
			else {
				wrappedException = e;
			}
			try {
				//as per the spec we should rollback if commit fails
				sharedSessionContract.getTransaction().rollback();
			}
			catch (Exception re) {
				//swallow
			}
			return new RollbackException( "Error while committing the transaction", wrappedException );
		}
		else {
			return e;
		}
	}

	@Override
	public RuntimeException convert(HibernateException exception, LockOptions lockOptions) {
		if ( exception instanceof StaleStateException ) {
			final PersistenceException converted = wrapStaleStateException( (StaleStateException) exception );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof LockAcquisitionException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) exception, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof LockingStrategyException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) exception, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.PessimisticLockException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) exception, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.QueryTimeoutException ) {
			final QueryTimeoutException converted = new QueryTimeoutException( exception.getMessage(), exception );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof ObjectNotFoundException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( exception.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueObjectException ) {
			final EntityExistsException converted = new EntityExistsException( exception.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof org.hibernate.NonUniqueResultException ) {
			final NonUniqueResultException converted = new NonUniqueResultException( exception.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof UnresolvableObjectException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( exception.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( exception instanceof SemanticException ) {
			return new IllegalArgumentException( exception );
		}
		else if ( exception instanceof QueryException ) {
			return new IllegalArgumentException( exception );
		}
		else if ( exception instanceof InterpretationException ) {
			return new IllegalArgumentException( exception );
		}
		else if ( exception instanceof ParsingException ) {
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
			final PersistenceException converted = new PersistenceException(
					"Converting `" + exception.getClass().getName() + "` to JPA `PersistenceException` : " + exception.getMessage(),
					exception
			);
			handlePersistenceException( converted );
			return converted;
		}
	}

	@Override
	public RuntimeException convert(HibernateException e) {
		return convert( e, null );
	}

	@Override
	public RuntimeException convert(RuntimeException e) {
		RuntimeException result = e;
		if ( e instanceof HibernateException ) {
			result = convert( (HibernateException) e );
		}
		else {
			sharedSessionContract.markForRollbackOnly();
		}
		return result;
	}

	@Override
	public RuntimeException convert(RuntimeException e, LockOptions lockOptions) {
		RuntimeException result = e;
		if ( e instanceof HibernateException ) {
			result = convert( (HibernateException) e, lockOptions );
		}
		else {
			sharedSessionContract.markForRollbackOnly();
		}
		return result;
	}

	@Override
	public JDBCException convert(SQLException e, String message) {
		return sharedSessionContract.getJdbcServices().getSqlExceptionHelper().convert( e, message );
	}

	protected PersistenceException wrapStaleStateException(StaleStateException e) {
		PersistenceException pe;
		if ( e instanceof StaleObjectStateException ) {
			final StaleObjectStateException sose = (StaleObjectStateException) e;
			final Object identifier = sose.getIdentifier();
			if ( identifier != null ) {
				try {
					final Object entity = sharedSessionContract.internalLoad( sose.getEntityName(), identifier, false, true);
					if ( entity instanceof Serializable ) {
						//avoid some user errors regarding boundary crossing
						pe = new OptimisticLockException( e.getMessage(), e, entity );
					}
					else {
						pe = new OptimisticLockException( e.getMessage(), e );
					}
				}
				catch (EntityNotFoundException enfe) {
					pe = new OptimisticLockException( e.getMessage(), e );
				}
			}
			else {
				pe = new OptimisticLockException( e.getMessage(), e );
			}
		}
		else {
			pe = new OptimisticLockException( e.getMessage(), e );
		}
		return pe;
	}

	protected PersistenceException wrapLockException(HibernateException e, LockOptions lockOptions) {
		final PersistenceException pe;
		if ( e instanceof OptimisticEntityLockException ) {
			final OptimisticEntityLockException lockException = (OptimisticEntityLockException) e;
			pe = new OptimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
		}
		else if ( e instanceof org.hibernate.exception.LockTimeoutException ) {
			pe = new LockTimeoutException( e.getMessage(), e, null );
		}
		else if ( e instanceof PessimisticEntityLockException ) {
			final PessimisticEntityLockException lockException = (PessimisticEntityLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				pe = new LockTimeoutException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
			else {
				pe = new PessimisticLockException(
						lockException.getMessage(),
						lockException,
						lockException.getEntity()
				);
			}
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			final org.hibernate.PessimisticLockException jdbcLockException = (org.hibernate.PessimisticLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				pe = new LockTimeoutException( jdbcLockException.getMessage(), jdbcLockException, null );
			}
			else {
				pe = new PessimisticLockException( jdbcLockException.getMessage(), jdbcLockException, null );
			}
		}
		else {
			pe = new OptimisticLockException( e );
		}
		return pe;
	}

	private void handlePersistenceException(PersistenceException e) {
		if ( e instanceof NoResultException ) {
			return;
		}
		if ( e instanceof NonUniqueResultException ) {
			return;
		}
		if ( e instanceof LockTimeoutException ) {
			return;
		}
		if ( e instanceof QueryTimeoutException ) {
			return;
		}

		try {
			sharedSessionContract.markForRollbackOnly();
		}
		catch (Exception ne) {
			//we do not want the subsequent exception to swallow the original one
			log.unableToMarkForRollbackOnPersistenceException( ne );
		}
	}

}
