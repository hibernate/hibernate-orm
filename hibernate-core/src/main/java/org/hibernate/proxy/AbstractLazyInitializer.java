/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;

import java.io.Serializable;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionException;
import org.hibernate.TransientObjectException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Convenience base class for lazy initialization handlers.  Centralizes the basic plumbing of doing lazy
 * initialization freeing subclasses to acts as essentially adapters to their intended entity mode and/or
 * proxy generation strategy.
 *
 * @author Gavin King
 */
public abstract class AbstractLazyInitializer implements LazyInitializer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractLazyInitializer.class );

	private String entityName;
	private Serializable id;
	private Object target;
	private boolean initialized;
	private boolean readOnly;
	private boolean unwrap;
	private transient SharedSessionContractImplementor session;
	private Boolean readOnlyBeforeAttachedToSession;

	private String sessionFactoryUuid;
	private boolean allowLoadOutsideTransaction;

	/**
	 * @deprecated This constructor was initially intended for serialization only, and is not useful anymore.
	 * In any case it should not be relied on by user code.
	 * Subclasses should rather implement Serializable with an {@code Object writeReplace()} method returning
	 * a subclass of {@link AbstractSerializableProxy},
	 * which in turn implements Serializable and an {@code Object readResolve()} method
	 * instantiating the {@link AbstractLazyInitializer} subclass
	 * and calling {@link AbstractSerializableProxy#afterDeserialization(AbstractLazyInitializer)} on it.
	 * See {@link org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor} and
	 * {@link org.hibernate.proxy.pojo.bytebuddy.SerializableProxy} for examples.
	 */
	@Deprecated
	protected AbstractLazyInitializer() {
	}

	/**
	 * Main constructor.
	 *
	 * @param entityName The name of the entity being proxied.
	 * @param id The identifier of the entity being proxied.
	 * @param session The session owning the proxy.
	 */
	protected AbstractLazyInitializer(String entityName, Serializable id, SharedSessionContractImplementor session) {
		this.entityName = entityName;
		this.id = id;
		// initialize other fields depending on session state
		if ( session == null ) {
			unsetSession();
		}
		else {
			setSession( session );
		}
	}

	@Override
	public final String getEntityName() {
		return entityName;
	}

	@Override
	public final Serializable getIdentifier() {
		if ( isUninitialized() && isInitializeProxyWhenAccessingIdentifier() ) {
			initialize();
		}
		return id;
	}

	private boolean isInitializeProxyWhenAccessingIdentifier() {
		return session != null && session.getFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance().isJpaProxyComplianceEnabled();
	}

	@Override
	public final void setIdentifier(Serializable id) {
		this.id = id;
	}

	@Override
	public final boolean isUninitialized() {
		return !initialized;
	}

	@Override
	public final SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public final void setSession(SharedSessionContractImplementor s) throws HibernateException {
		if ( s != session ) {
			// check for s == null first, since it is least expensive
			if ( s == null ) {
				unsetSession();
			}
			else if ( isConnectedToSession() ) {
				//TODO: perhaps this should be some other RuntimeException...
				LOG.attemptToAssociateProxyWithTwoOpenSessions(
					entityName,
					id
				);
				throw new HibernateException( "illegally attempted to associate proxy [" + entityName + "#" + id + "] with two open Sessions" );
			}
			else {
				// s != null
				session = s;
				if ( readOnlyBeforeAttachedToSession == null ) {
					// use the default read-only/modifiable setting
					final EntityPersister persister = s.getFactory().getEntityPersister( entityName );
					setReadOnly( s.getPersistenceContext().isDefaultReadOnly() || !persister.isMutable() );
				}
				else {
					// use the read-only/modifiable setting indicated during deserialization
					setReadOnly( readOnlyBeforeAttachedToSession );
					readOnlyBeforeAttachedToSession = null;
				}
			}
		}
	}

	private static EntityKey generateEntityKeyOrNull(Serializable id, SharedSessionContractImplementor s, String entityName) {
		if ( id == null || s == null || entityName == null ) {
			return null;
		}
		return s.generateEntityKey( id, s.getFactory().getEntityPersister( entityName ) );
	}

	@Override
	public final void unsetSession() {
		prepareForPossibleLoadingOutsideTransaction();
		session = null;
		readOnly = false;
		readOnlyBeforeAttachedToSession = null;
	}

	@Override
	public final void initialize() throws HibernateException {
		if ( !initialized ) {
			if ( allowLoadOutsideTransaction ) {
				permissiveInitialization();
			}
			else if ( session == null ) {
				throw new LazyInitializationException( "could not initialize proxy [" + entityName + "#" + id + "] - no Session" );
			}
			else if ( !session.isOpenOrWaitingForAutoClose() ) {
				throw new LazyInitializationException( "could not initialize proxy [" + entityName + "#" + id + "] - the owning Session was closed" );
			}
			else if ( !session.isConnected() ) {
				throw new LazyInitializationException( "could not initialize proxy [" + entityName + "#" + id + "] - the owning Session is disconnected" );
			}
			else {
				target = session.immediateLoad( entityName, id );
				initialized = true;
				checkTargetState(session);
			}
		}
		else {
			checkTargetState(session);
		}
	}

	protected void permissiveInitialization() {
		if ( session == null ) {
			//we have a detached collection thats set to null, reattach
			if ( sessionFactoryUuid == null ) {
				throw new LazyInitializationException( "could not initialize proxy [" + entityName + "#" + id + "] - no Session" );
			}
			try {
				SessionFactoryImplementor sf = (SessionFactoryImplementor)
						SessionFactoryRegistry.INSTANCE.getSessionFactory( sessionFactoryUuid );
				SharedSessionContractImplementor session = (SharedSessionContractImplementor) sf.openSession();
				session.getPersistenceContext().setDefaultReadOnly( true );
				session.setFlushMode( FlushMode.MANUAL );

				boolean isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

				if ( !isJTA ) {
					// Explicitly handle the transactions only if we're not in
					// a JTA environment.  A lazy loading temporary session can
					// be created even if a current session and transaction are
					// open (ex: session.clear() was used).  We must prevent
					// multiple transactions.
					session.beginTransaction();
				}

				try {
					target = session.immediateLoad( entityName, id );
					initialized = true;
					checkTargetState(session);
				}
				finally {
					// make sure the just opened temp session gets closed!
					try {
						if ( !isJTA ) {
							session.getTransaction().commit();
						}
						session.close();
					}
					catch (Exception e) {
						LOG.warn( "Unable to close temporary session used to load lazy proxy associated to no session" );
					}
				}
			}
			catch (Exception e) {
				LOG.error( "Initialization failure [" + entityName + "#" + id + "]", e );
				throw new LazyInitializationException( e.getMessage() );
			}
		}
		else if ( session.isOpenOrWaitingForAutoClose() && session.isConnected() ) {
			target = session.immediateLoad( entityName, id );
			initialized = true;
			checkTargetState(session);
		}
		else {
			throw new LazyInitializationException( "could not initialize proxy [" + entityName + "#" + id + "] - Session was closed or disced" );
		}
	}

	/**
	 * Attempt to initialize the proxy without loading anything from the database.
	 *
	 * This will only have any effect if the proxy is still attached to a session,
	 * and the entity being proxied has been loaded and added to the persistence context
	 * of that session since the proxy was created.
	 */
	public final void initializeWithoutLoadIfPossible() {
		if ( !initialized && session != null && session.isOpenOrWaitingForAutoClose() ) {
			final EntityKey key = session.generateEntityKey(
					getIdentifier(),
					session.getFactory().getMetamodel().entityPersister( getEntityName() )
			);
			final Object entity = session.getPersistenceContextInternal().getEntity( key );
			if ( entity != null ) {
				setImplementation( entity );
			}
		}
	}

	/**
	 * Initialize internal state based on the currently attached session,
	 * in order to be ready to load data even after the proxy is detached from the session.
	 *
	 * This method only has any effect if
	 * {@link SessionFactoryOptions#isInitializeLazyStateOutsideTransactionsEnabled()} is {@code true}.
	 */
	protected void prepareForPossibleLoadingOutsideTransaction() {
		if ( session != null ) {
			allowLoadOutsideTransaction = session.getFactory().getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();

			if ( allowLoadOutsideTransaction && sessionFactoryUuid == null ) {
				sessionFactoryUuid = session.getFactory().getUuid();
			}
		}
	}

	private void checkTargetState(SharedSessionContractImplementor session) {
		if ( !unwrap ) {
			if ( target == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
			}
		}
	}

	/**
	 * Getter for property 'connectedToSession'.
	 *
	 * @return Value for property 'connectedToSession'.
	 */
	protected final boolean isConnectedToSession() {
		return getProxyOrNull() != null;
	}

	private Object getProxyOrNull() {
		final EntityKey entityKey = generateEntityKeyOrNull( getIdentifier(), session, getEntityName() );
		if ( entityKey != null && session != null && session.isOpenOrWaitingForAutoClose() ) {
			return session.getPersistenceContextInternal().getProxy( entityKey );
		}
		return null;
	}

	@Override
	public final Object getImplementation() {
		initialize();
		return target;
	}

	@Override
	public final void setImplementation(Object target) {
		this.target = target;
		initialized = true;
	}

	@Override
	public final Object getImplementation(SharedSessionContractImplementor s) throws HibernateException {
		final EntityKey entityKey = generateEntityKeyOrNull( getIdentifier(), s, getEntityName() );
		return ( entityKey == null ? null : s.getPersistenceContext().getEntity( entityKey ) );
	}

	/**
	 * Getter for property 'target'.
	 * <p/>
	 * Same as {@link #getImplementation()} except that this method will not force initialization.
	 *
	 * @return Value for property 'target'.
	 */
	protected final Object getTarget() {
		return target;
	}

	@Override
	public final boolean isReadOnlySettingAvailable() {
		return (session != null && !session.isClosed());
	}

	private void errorIfReadOnlySettingNotAvailable() {
		if ( session == null ) {
			throw new TransientObjectException(
					"Proxy [" + entityName + "#" + id + "] is detached (i.e, session is null). The read-only/modifiable setting is only accessible when the proxy is associated with an open session."
			);
		}
		if ( !session.isOpenOrWaitingForAutoClose() ) {
			throw new SessionException(
					"Session is closed. The read-only/modifiable setting is only accessible when the proxy [" + entityName + "#" + id + "] is associated with an open session."
			);
		}
	}

	@Override
	public final boolean isReadOnly() {
		errorIfReadOnlySettingNotAvailable();
		return readOnly;
	}

	@Override
	public final void setReadOnly(boolean readOnly) {
		errorIfReadOnlySettingNotAvailable();
		// only update if readOnly is different from current setting
		if ( this.readOnly != readOnly ) {
			final EntityPersister persister = session.getFactory().getEntityPersister( entityName );
			if ( !persister.isMutable() && !readOnly ) {
				throw new IllegalStateException( "cannot make proxies [" + entityName + "#" + id + "] for immutable entities modifiable" );
			}
			this.readOnly = readOnly;
			if ( initialized ) {
				EntityKey key = generateEntityKeyOrNull( getIdentifier(), session, getEntityName() );
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				if ( key != null && persistenceContext.containsEntity( key ) ) {
					persistenceContext.setReadOnly( target, readOnly );
				}
			}
		}
	}

	/**
	 * Get the read-only/modifiable setting that should be put in affect when it is
	 * attached to a session.
	 * <p/>
	 * This method should only be called during serialization when read-only/modifiable setting
	 * is not available (i.e., isReadOnlySettingAvailable() == false)
	 *
	 * @return null, if the default setting should be used;
	 *         true, for read-only;
	 *         false, for modifiable
	 *
	 * @throws IllegalStateException if isReadOnlySettingAvailable() == true
	 */
	public final Boolean isReadOnlyBeforeAttachedToSession() {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call isReadOnlyBeforeAttachedToSession when isReadOnlySettingAvailable == true [" + entityName + "#" + id + "]"
			);
		}
		return readOnlyBeforeAttachedToSession;
	}

	/**
	 * Get whether the proxy can load data even
	 * if it's not attached to a session with an ongoing transaction.
	 *
	 * This method should only be called during serialization,
	 * and only makes sense after a call to {@link #prepareForPossibleLoadingOutsideTransaction()}.
	 *
	 * @return {@code true} if out-of-transaction loads are allowed, {@code false} otherwise.
	 */
	protected boolean isAllowLoadOutsideTransaction() {
		return allowLoadOutsideTransaction;
	}

	/**
	 * Get the session factory UUID.
	 *
	 * This method should only be called during serialization,
	 * and only makes sense after a call to {@link #prepareForPossibleLoadingOutsideTransaction()}.
	 *
	 * @return the session factory UUID.
	 */
	protected String getSessionFactoryUuid() {
		return sessionFactoryUuid;
	}

	/**
	 * Restore settings that are not passed to the constructor,
	 * but are still preserved during serialization.
	 *
	 * This method should only be called during deserialization, before associating
	 * the proxy with a session.
	 *
	 * @param readOnlyBeforeAttachedToSession the read-only/modifiable setting to use when
	 * associated with a session; null indicates that the default should be used.
	 * @param sessionFactoryUuid the session factory uuid, to be used if {@code allowLoadOutsideTransaction} is {@code true}.
	 * @param allowLoadOutsideTransaction whether the proxy can load data even
	 * if it's not attached to a session with an ongoing transaction.
	 *
	 * @throws IllegalStateException if isReadOnlySettingAvailable() == true
	 */
	/* package-private */
	final void afterDeserialization(Boolean readOnlyBeforeAttachedToSession,
			String sessionFactoryUuid, boolean allowLoadOutsideTransaction) {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call afterDeserialization when isReadOnlySettingAvailable == true [" + entityName + "#" + id + "]"
			);
		}
		this.readOnlyBeforeAttachedToSession = readOnlyBeforeAttachedToSession;

		this.sessionFactoryUuid = sessionFactoryUuid;
		this.allowLoadOutsideTransaction = allowLoadOutsideTransaction;
	}

	@Override
	public boolean isUnwrap() {
		return unwrap;
	}

	@Override
	public void setUnwrap(boolean unwrap) {
		this.unwrap = unwrap;
	}
}
