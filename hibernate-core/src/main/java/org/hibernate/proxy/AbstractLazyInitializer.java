/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.proxy;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Convenience base class for lazy initialization handlers.  Centralizes the basic plumbing of doing lazy
 * initialization, freeing subclasses to acts as essentially adapters to their intended entity mode and/or
 * proxy generation strategy.
 *
 * @author Gavin King
 */
public abstract class AbstractLazyInitializer implements LazyInitializer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractLazyInitializer.class );

	private final String entityName;
	private Object id;
	private Object target;
	private boolean initialized;
	private boolean readOnly;
	private boolean unwrap;
	private transient SharedSessionContractImplementor session;
	private Boolean readOnlyBeforeAttachedToSession;

	private String sessionFactoryUuid;
	private String sessionFactoryName;
	private boolean allowLoadOutsideTransaction;

	/**
	 * Main constructor.
	 *
	 * @param entityName The name of the entity being proxied.
	 * @param id The identifier of the entity being proxied.
	 * @param session The session owning the proxy.
	 */
	protected AbstractLazyInitializer(String entityName, Object id, SharedSessionContractImplementor session) {
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
	public final Object getInternalIdentifier() {
		return id;
	}

	@Override
	public final Object getIdentifier() {
		if ( isUninitialized() && isInitializeProxyWhenAccessingIdentifier() ) {
			initialize();
		}
		return id;
	}

	private MappingMetamodelImplementor getMappingMetamodel() {
		return session.getFactory().getMappingMetamodel();
	}

	private EntityPersister getEntityDescriptor() {
		return getMappingMetamodel().getEntityDescriptor( entityName );
	}

	private SessionFactoryOptions getSessionFactoryOptions() {
		return session.getFactory().getSessionFactoryOptions();
	}

	private boolean isInitializeProxyWhenAccessingIdentifier() {
		return session != null && getSessionFactoryOptions().getJpaCompliance().isJpaProxyComplianceEnabled();
	}

	@Override
	public final void setIdentifier(Object id) {
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
	public final void setSession(SharedSessionContractImplementor session) throws HibernateException {
		if ( session != this.session ) {
			// check for session == null first, since it is less expensive
			if ( session == null ) {
				unsetSession();
			}
			else if ( isConnectedToSession() ) {
				//TODO: perhaps this should be some other RuntimeException...
				LOG.attemptToAssociateProxyWithTwoOpenSessions( entityName, id );
				throw new HibernateException( "Illegally attempted to associate proxy ["
						+ entityName + "#" + id + "] with two open sessions" );
			}
			else {
				// session != null
				this.session = session;
				if ( readOnlyBeforeAttachedToSession == null ) {
					// use the default read-only/modifiable setting
					setReadOnly( session.getPersistenceContext().isDefaultReadOnly()
							|| !getEntityDescriptor().isMutable() );
				}
				else {
					// use the read-only/modifiable setting indicated during deserialization
					setReadOnly( readOnlyBeforeAttachedToSession );
					readOnlyBeforeAttachedToSession = null;
				}
			}
		}
	}

	private static EntityKey generateEntityKeyOrNull(Object id, SharedSessionContractImplementor session, String entityName) {
		if ( id == null || session == null || entityName == null ) {
			return null;
		}
		else {
			final EntityPersister entityDescriptor =
					session.getFactory().getMappingMetamodel()
							.getEntityDescriptor( entityName );
			return session.generateEntityKey( id, entityDescriptor );
		}
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
			try {
				if ( allowLoadOutsideTransaction ) {
					permissiveInitialization();
				}
				else if ( session == null ) {
					throw new LazyInitializationException( "Could not initialize proxy ["
							+ entityName + "#" + id + "] - no session" );
				}
				else if ( !session.isOpenOrWaitingForAutoClose() ) {
					throw new LazyInitializationException( "Could not initialize proxy ["
							+ entityName + "#" + id + "] - the owning session was closed" );
				}
				else if ( !session.isConnected() ) {
					throw new LazyInitializationException( "Could not initialize proxy ["
							+ entityName + "#" + id + "] - the owning session is disconnected" );
				}
				else {
					target = session.immediateLoad( entityName, id );
					initialized = true;
					checkTargetState( session );
				}
			}
			finally {
				if ( session != null && !session.isTransactionInProgress() ) {
					session.getJdbcCoordinator().afterTransaction();
				}
			}
		}
		else {
			checkTargetState( session );
		}
	}

	protected void permissiveInitialization() {
		if ( session == null ) {
			//we have a detached collection that is set to null, reattach
			if ( sessionFactoryUuid == null ) {
				throw new LazyInitializationException( "Could not initialize proxy ["
						+ entityName + "#" + id + "] - no session" );
			}
			try {
				final SessionFactoryImplementor factory =
						SessionFactoryRegistry.INSTANCE.getSessionFactory( sessionFactoryUuid );
				final SessionImplementor session = factory.openSession();
				session.getPersistenceContext().setDefaultReadOnly( true );
				session.setHibernateFlushMode( FlushMode.MANUAL );

				final boolean isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

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
					checkTargetState( session );
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
			checkTargetState( session );
		}
		else {
			throw new LazyInitializationException( "Could not initialize proxy ["
					+ entityName + "#" + id + "] - session was closed or disconnected" );
		}
	}

	/**
	 * Attempt to initialize the proxy without loading anything from the database.
	 * <p>
	 * This will only have an effect if the proxy is still attached to a session,
	 * and the entity being proxied has been loaded and added to the persistence
	 * context of that session since the proxy was created.
	 */
	public final void initializeWithoutLoadIfPossible() {
		if ( !initialized && session != null && session.isOpenOrWaitingForAutoClose() ) {
			final EntityPersister entityDescriptor = getMappingMetamodel().getEntityDescriptor( getEntityName() );
			final EntityKey key = session.generateEntityKey( getInternalIdentifier(), entityDescriptor );
			final Object entity = session.getPersistenceContextInternal().getEntity( key );
			if ( entity != null ) {
				setImplementation( entity );
			}
		}
	}

	/**
	 * Initialize internal state based on the currently attached session, in order
	 * to be ready to load data even after the proxy is detached from the session.
	 */
	protected void prepareForPossibleLoadingOutsideTransaction() {
		if ( session != null ) {
			allowLoadOutsideTransaction =
					getSessionFactoryOptions().isInitializeLazyStateOutsideTransactionsEnabled();

			if ( sessionFactoryUuid == null ) {
				// we're going to need the UUID even if the SessionFactory configuration doesn't
				// allow any operations on it, as we need it to match deserialized objects with
				// the originating SessionFactory: at very least it's useful to actually get
				// such configuration, so to know if such operation isn't allowed or configured otherwise.
				sessionFactoryUuid = session.getFactory().getUuid();
			}
			if ( sessionFactoryName == null ) {
				sessionFactoryName = session.getFactory().getName();
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
		final EntityKey entityKey = generateEntityKeyOrNull( getInternalIdentifier(), session, getEntityName() );
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
	public final Object getImplementation(SharedSessionContractImplementor session) throws HibernateException {
		final EntityKey entityKey = generateEntityKeyOrNull( getInternalIdentifier(), session, getEntityName() );
		return entityKey == null ? null : session.getPersistenceContext().getEntity( entityKey );
	}

	@Override
	public String getImplementationEntityName() {
		if ( session == null ) {
			throw new LazyInitializationException( "Could not retrieve real entity name ["
					+ entityName + "#" + id + "] - no session" );
		}
		if ( getEntityDescriptor().getEntityMetamodel().hasSubclasses() ) {
			initialize();
			return session.getFactory().bestGuessEntityName( target );
		}
		return entityName;
	}

	/**
	 * Getter for property "target".
	 * <p>
	 * Same as {@link #getImplementation()} except that this method will not force initialization.
	 *
	 * @return Value for property "target".
	 */
	protected final Object getTarget() {
		return target;
	}

	@Override
	public final boolean isReadOnlySettingAvailable() {
		return session != null && !session.isClosed();
	}

	private void errorIfReadOnlySettingNotAvailable() {
		if ( session == null ) {
			throw new IllegalStateException(
					"Proxy for [" + entityName + "#" + id + "] is not associated with a session"
							+ " (the read-only/modifiable setting is only accessible when the proxy is associated with an open session)"
			);
		}
		if ( !session.isOpenOrWaitingForAutoClose() ) {
			throw new SessionException(
					"Proxy for [" + entityName + "#" + id + "] is associated with a closed session"
							+ " (the read-only/modifiable setting is only accessible when the proxy is associated with an open session)"
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
			final EntityPersister entityDescriptor = getEntityDescriptor();
			if ( !entityDescriptor.isMutable() && !readOnly ) {
				throw new IllegalStateException( "Cannot make proxy [" + entityName + "#" + id + "] for immutable entity modifiable" );
			}
			this.readOnly = readOnly;
			if ( initialized ) {
				EntityKey key = generateEntityKeyOrNull( getInternalIdentifier(), session, getEntityName() );
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				if ( key != null && persistenceContext.containsEntity( key ) ) {
					persistenceContext.setReadOnly( target, readOnly );
				}
			}
		}
	}

	/**
	 * Get the read-only/modifiable setting that should be put in effect when it is
	 * attached to a session.
	 * <p>
	 * This method should only be called during serialization when read-only/modifiable
	 * setting is not available, that is, if {@code isReadOnlySettingAvailable() == false}
	 *
	 * @return {@code null}, if the default setting should be used;
	 *         {@code true}, for read-only;
	 *         {@code false}, for modifiable
	 *
	 * @throws IllegalStateException if {@code isReadOnlySettingAvailable() == true}
	 */
	public final Boolean isReadOnlyBeforeAttachedToSession() {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call isReadOnlyBeforeAttachedToSession when isReadOnlySettingAvailable == true ["
							+ entityName + "#" + id + "]"
			);
		}
		return readOnlyBeforeAttachedToSession;
	}

	/**
	 * Get whether the proxy can load data even if it's not attached to a session
	 * with an ongoing transaction.
	 * <p>
	 * This method should only be called during serialization, and only makes sense
	 * after a call to {@link #prepareForPossibleLoadingOutsideTransaction()}.
	 *
	 * @return {@code true} if out-of-transaction loads are allowed,
	 *         {@code false} otherwise.
	 */
	protected boolean isAllowLoadOutsideTransaction() {
		return allowLoadOutsideTransaction;
	}

	/**
	 * Get the session factory UUID.
	 * <p>
	 * This method should only be called during serialization, and only makes sense
	 * after a call to {@link #prepareForPossibleLoadingOutsideTransaction()}.
	 *
	 * @return the session factory UUID.
	 */
	protected String getSessionFactoryUuid() {
		return sessionFactoryUuid;
	}

	/**
	 * Get the session factory name.
	 * <p>
	 * This method should only be called during serialization, and only makes sense
	 * after a call to {@link #prepareForPossibleLoadingOutsideTransaction()}.
	 *
	 * @return the session factory name.
	 */
	protected String getSessionFactoryName() {
		return sessionFactoryName;
	}

	/**
	 * Restore settings that are not passed to the constructor,
	 * but are still preserved during serialization.
	 * <p>
	 * This method should only be called during deserialization, before associating
	 * the proxy with a session.
	 *
	 * @param readOnlyBeforeAttachedToSession the read-only/modifiable setting to
	 *        use when associated with a session; null indicates that the default
	 *        should be used.
	 * @param sessionFactoryUuid the session factory uuid, to be used if
	 *        {@code allowLoadOutsideTransaction} is {@code true}.
	 * @param allowLoadOutsideTransaction whether the proxy can load data even
	 * if it's not attached to a session with an ongoing transaction.
	 *
	 * @throws IllegalStateException if {@code isReadOnlySettingAvailable() == true}
	 */
	/* package-private */
	final void afterDeserialization(Boolean readOnlyBeforeAttachedToSession,
			String sessionFactoryUuid, String sessionFactoryName, boolean allowLoadOutsideTransaction) {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call afterDeserialization when isReadOnlySettingAvailable == true ["
							+ entityName + "#" + id + "]"
			);
		}
		this.readOnlyBeforeAttachedToSession = readOnlyBeforeAttachedToSession;

		this.sessionFactoryUuid = sessionFactoryUuid;
		this.sessionFactoryName = sessionFactoryName;
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
