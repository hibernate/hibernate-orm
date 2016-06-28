/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.sql.SQLException;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.QueryTimeoutException;
import javax.persistence.RollbackException;

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
import org.hibernate.loader.MultipleBagFetchException;

/**
 * @author Andrea Boriero
 */
public class ExceptionConverterImpl implements ExceptionConverter {
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( ExceptionConverterImpl.class );

	private final AbstractSharedSessionContract sharedSessionContract;

	public ExceptionConverterImpl(AbstractSharedSessionContract sharedSessionContract) {
		this.sharedSessionContract = sharedSessionContract;
	}

	@Override
	public RuntimeException convertCommitException(RuntimeException e) {
		if ( sharedSessionContract.getFactory().getSessionFactoryOptions().isJpaBootstrap() ) {
			Throwable wrappedException;
			if ( e instanceof PersistenceException ) {
				Throwable cause = e.getCause() == null ? e : e.getCause();
				if ( cause instanceof HibernateException ) {
					wrappedException = convert( (HibernateException) cause );
				}
				else {
					wrappedException = cause;
				}
			}
			else if ( e instanceof HibernateException ) {
				wrappedException = convert( (HibernateException) e );
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
	public RuntimeException convert(HibernateException e, LockOptions lockOptions) {
		Throwable cause = e;
		if ( cause instanceof StaleStateException ) {
			final PersistenceException converted = wrapStaleStateException( (StaleStateException) cause );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof LockingStrategyException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) cause, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof org.hibernate.exception.LockTimeoutException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) cause, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof org.hibernate.PessimisticLockException ) {
			final PersistenceException converted = wrapLockException( (HibernateException) cause, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof org.hibernate.QueryTimeoutException ) {
			final QueryTimeoutException converted = new QueryTimeoutException( cause.getMessage(), cause );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof ObjectNotFoundException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( cause.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof org.hibernate.NonUniqueObjectException ) {
			final EntityExistsException converted = new EntityExistsException( cause.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof org.hibernate.NonUniqueResultException ) {
			final NonUniqueResultException converted = new NonUniqueResultException( cause.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof UnresolvableObjectException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( cause.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( cause instanceof QueryException ) {
			return new IllegalArgumentException( cause );
		}
		else if ( cause instanceof MultipleBagFetchException ) {
			return new IllegalArgumentException( cause );
		}
		else if ( cause instanceof TransientObjectException ) {
			try {
				sharedSessionContract.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				log.unableToMarkForRollbackOnTransientObjectException( ne );
			}
			return new IllegalStateException( e ); //Spec 3.2.3 Synchronization rules
		}
		else {
			final PersistenceException converted = new PersistenceException( cause );
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
			final Serializable identifier = sose.getIdentifier();
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
