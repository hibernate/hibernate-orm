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
	private static final CoreMessageLogger log = CoreLogging.messageLogger( ExceptionConverterImpl.class );

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
			return new RollbackException( "Error while committing the transaction",
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
				session.markForRollbackOnly();
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
		if ( exception instanceof StaleObjectStateException staleStateException ) {
			final Object identifier = staleStateException.getIdentifier();
			final String entityName = staleStateException.getEntityName();
			if ( identifier != null ) {
				try {
					final Object entity = session.internalLoad( entityName, identifier, false, true );
					if ( entity instanceof Serializable ) { // avoid some user errors regarding boundary crossing
						return new OptimisticLockException( exception.getMessage(), exception, entity );
					}
				}
				catch (EntityNotFoundException entityNotFoundException) {
					// swallow it;
				}
			}
		}
		return new OptimisticLockException( exception.getMessage(), exception );
	}

	protected PersistenceException wrapLockException(HibernateException exception, LockOptions lockOptions) {
		if ( exception instanceof OptimisticEntityLockException lockException ) {
			return new OptimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
		}
		else if ( exception instanceof org.hibernate.exception.LockTimeoutException ) {
			return new LockTimeoutException( exception.getMessage(), exception, null );
		}
		else if ( exception instanceof PessimisticEntityLockException lockException ) {
			// assume lock timeout occurred if a timeout or NO WAIT was specified
			return lockOptions != null && lockOptions.getTimeOut() > -1
					? new LockTimeoutException( lockException.getMessage(), lockException, lockException.getEntity() )
					: new PessimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
		}
		else if ( exception instanceof org.hibernate.PessimisticLockException lockException ) {
			// assume lock timeout occurred if a timeout or NO WAIT was specified
			return lockOptions != null && lockOptions.getTimeOut() > -1
					? new LockTimeoutException( lockException.getMessage(), lockException, null )
					: new PessimisticLockException( lockException.getMessage(), lockException, null );
		}
		else {
			return new OptimisticLockException( exception );
		}
	}

	private void rollbackIfNecessary(PersistenceException persistenceException) {
		if ( !isNonRollbackException( persistenceException ) ) {
			try {
				session.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				log.unableToMarkForRollbackOnPersistenceException( ne );
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
