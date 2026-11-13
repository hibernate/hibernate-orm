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
import org.hibernate.UnmanagedObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.dialect.lock.internal.OptimisticEntityLockException;
import org.hibernate.dialect.lock.internal.PessimisticEntityLockException;
import org.hibernate.dialect.lock.spi.LockingStrategyException;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.exception.TransactionSerializationException;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SyntaxException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

	@Nonnull
	private final SharedSessionContractImplementor session;
	private final boolean isJpaBootstrap;

	public ExceptionConverterImpl(@Nonnull SharedSessionContractImplementor session) {
		this.session = session;
		isJpaBootstrap = session.getFactory().getSessionFactoryOptions().isJpaBootstrap();
	}

	@Override
	@Nonnull
	public RuntimeException convertCommitException(@Nonnull RuntimeException exception) {
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
	@Nonnull
	public RuntimeException convert(@Nonnull HibernateException exception, @Nullable LockOptions lockOptions) {
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
		else if ( exception instanceof org.hibernate.QueryTimeoutException queryTimeoutException ) {
			final var converted =
					causesRollback( queryTimeoutException )
							// per spec, we must throw this exception if the tx was aborted
							? new PersistenceException( exception.getMessage(), exception )
							// per spec, we only throw this exception if the tx is not aborted
							: new QueryTimeoutException( exception.getMessage(), exception );
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
		else if ( isJpaBootstrap && exception instanceof QueryTypeMismatchException ) {
			return new IllegalArgumentException( exception );
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
		else if ( exception instanceof UnmanagedObjectException ) {
			try {
				session.markForRollbackOnly();
			}
			catch (Exception ne) {
				//we do not want the subsequent exception to swallow the original one
				CORE_LOGGER.unableToMarkForRollbackOnDetachedObjectException( ne );
			}
			throw new IllegalArgumentException( exception );
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
	@Nonnull
	public RuntimeException convert(@Nonnull HibernateException exception) {
		return convert( exception, null );
	}

	@Override
	@Nonnull
	public RuntimeException convert(@Nonnull RuntimeException exception) {
		if ( exception instanceof HibernateException hibernateException ) {
			return convert( hibernateException );
		}
		else {
			session.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	@Nonnull
	public RuntimeException convert(@Nonnull RuntimeException exception, @Nullable LockOptions lockOptions) {
		if ( exception instanceof HibernateException hibernateException ) {
			return convert( hibernateException, lockOptions );
		}
		else {
			session.markForRollbackOnly();
			return exception;
		}
	}

	@Override
	@Nonnull
	public JDBCException convert(@Nonnull SQLException e, @Nullable String message) {
		return session.getJdbcServices().getSqlExceptionHelper().convert( e, message );
	}

	@Nonnull
	protected PersistenceException wrapStaleStateException(@Nonnull StaleStateException exception) {
		final String message = exception.getMessage();
		if ( exception instanceof StaleObjectStateException stale && stale.getIdentifier() != null ) {
			final var persister =
					session.getFactory().getMappingMetamodel()
							.findEntityDescriptor( stale.getEntityName() );
			if ( persister != null ) {
				final var key = session.generateEntityKey( stale.getIdentifier(), persister );
				// Only attach an instance we already have. A load here can mask the original failure.
				final var entity = session.getPersistenceContextInternal().getEntity( key );
				if ( entity instanceof Serializable ) {
					return new OptimisticLockException( message, exception, entity );
				}
			}
		}
		return new OptimisticLockException( message, exception );
	}

	@Nonnull
	protected PersistenceException wrapLockException(
			@Nonnull LockingStrategyException exception,
			@Nullable LockOptions lockOptions) {
		final String message = exception.getMessage();
		final Object entity = exception.getEntity();
		if ( exception instanceof OptimisticEntityLockException lockException ) {
			return new OptimisticLockException( message, lockException, entity );
		}
		else if ( exception instanceof PessimisticEntityLockException lockException ) {
			// JPA distinguishes transaction rollback from statement rollback, regardless of the requested timeout.
			return causesRollback( lockException.getCause() )
					? new PessimisticLockException( message, lockException, entity )
					: new LockTimeoutException( message, lockException, entity );
		}
		else {
			throw new AssertionFailure( "Unrecognized exception type" );
		}
	}

	@Nonnull
	protected PersistenceException wrapLockException(
			@Nonnull org.hibernate.PessimisticLockException exception,
			@Nullable LockOptions lockOptions) {
		final String message = exception.getMessage();
		return causesRollback( exception )
				? new PessimisticLockException( message, exception )
				: new LockTimeoutException( message, exception );
	}

	private boolean causesRollback(@Nonnull JDBCException exception) {
		return session.getJdbcServices().getDialect().causesRollback( exception.getSQLException() );
	}

	private void rollbackIfNecessary(@Nonnull PersistenceException persistenceException) {
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
	private static boolean isNonRollbackException(@Nonnull PersistenceException persistenceException) {
		return persistenceException instanceof NoResultException
			|| persistenceException instanceof NonUniqueResultException
			|| persistenceException instanceof LockTimeoutException
			|| persistenceException instanceof QueryTimeoutException;
	}

}
