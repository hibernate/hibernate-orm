/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;

import java.io.Serializable;
import javax.naming.NamingException;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

/**
 * Convenience base class for lazy initialization handlers.  Centralizes the basic plumbing of doing lazy
 * initialization freeing subclasses to acts as essentially adapters to their intended entity mode and/or
 * proxy generation strategy.
 *
 * @author Gavin King
 */
public abstract class AbstractLazyInitializer implements LazyInitializer {
	private static final Logger log = Logger.getLogger( AbstractLazyInitializer.class );

	private String entityName;
	private Serializable id;
	private Object target;
	private boolean initialized;
	private boolean readOnly;
	private boolean unwrap;
	private transient SessionImplementor session;
	private Boolean readOnlyBeforeAttachedToSession;

	private String sessionFactoryUuid;
	private boolean allowLoadOutsideTransaction;

	/**
	 * For serialization from the non-pojo initializers (HHH-3309)
	 */
	protected AbstractLazyInitializer() {
	}

	/**
	 * Main constructor.
	 *
	 * @param entityName The name of the entity being proxied.
	 * @param id The identifier of the entity being proxied.
	 * @param session The session owning the proxy.
	 */
	protected AbstractLazyInitializer(String entityName, Serializable id, SessionImplementor session) {
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
		return id;
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
	public final SessionImplementor getSession() {
		return session;
	}

	@Override
	public final void setSession(SessionImplementor s) throws HibernateException {
		if ( s != session ) {
			// check for s == null first, since it is least expensive
			if ( s == null ) {
				unsetSession();
			}
			else if ( isConnectedToSession() ) {
				//TODO: perhaps this should be some other RuntimeException...
				throw new HibernateException( "illegally attempted to associate a proxy with two open Sessions" );
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

	private static EntityKey generateEntityKeyOrNull(Serializable id, SessionImplementor s, String entityName) {
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
				throw new LazyInitializationException( "could not initialize proxy - no Session" );
			}
			else if ( !session.isOpen() ) {
				throw new LazyInitializationException( "could not initialize proxy - the owning Session was closed" );
			}
			else if ( !session.isConnected() ) {
				throw new LazyInitializationException( "could not initialize proxy - the owning Session is disconnected" );
			}
			else {
				target = session.immediateLoad( entityName, id );
				initialized = true;
				checkTargetState();
			}
		}
		else {
			checkTargetState();
		}
	}

	protected void permissiveInitialization() {
		if ( session == null ) {
			//we have a detached collection thats set to null, reattach
			if ( sessionFactoryUuid == null ) {
				throw new LazyInitializationException( "could not initialize proxy - no Session" );
			}
			try {
				SessionFactoryImplementor sf = (SessionFactoryImplementor)
						SessionFactoryRegistry.INSTANCE.getSessionFactory( sessionFactoryUuid );
				SessionImplementor session = (SessionImplementor) sf.openSession();
				session.getPersistenceContext().setDefaultReadOnly( true );
				session.setFlushMode( FlushMode.MANUAL );

				boolean isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();

				if ( !isJTA ) {
					// Explicitly handle the transactions only if we're not in
					// a JTA environment.  A lazy loading temporary session can
					// be created even if a current session and transaction are
					// open (ex: session.clear() was used).  We must prevent
					// multiple transactions.
					( ( Session) session ).beginTransaction();
				}

				try {
					target = session.immediateLoad( entityName, id );
				}
				finally {
					// make sure the just opened temp session gets closed!
					try {
						if ( !isJTA ) {
							( ( Session) session ).getTransaction().commit();
						}
						( (Session) session ).close();
					}
					catch (Exception e) {
						log.warn( "Unable to close temporary session used to load lazy proxy associated to no session" );
					}
				}
				initialized = true;
				checkTargetState();
			}
			catch (Exception e) {
				e.printStackTrace();
				throw new LazyInitializationException( e.getMessage() );
			}
		}
		else if ( session.isOpen() && session.isConnected() ) {
			target = session.immediateLoad( entityName, id );
			initialized = true;
			checkTargetState();
		}
		else {
			throw new LazyInitializationException( "could not initialize proxy - Session was closed or disced" );
		}
	}

	protected void prepareForPossibleLoadingOutsideTransaction() {
		if ( session != null ) {
			allowLoadOutsideTransaction = session.getFactory().getSettings().isInitializeLazyStateOutsideTransactionsEnabled();

			if ( allowLoadOutsideTransaction && sessionFactoryUuid == null ) {
				try {
					sessionFactoryUuid = (String) session.getFactory().getReference().get( "uuid" ).getContent();
				}
				catch (NamingException e) {
					//not much we can do if this fails...
				}
			}
		}
	}

	private void checkTargetState() {
		if ( !unwrap ) {
			if ( target == null ) {
				getSession().getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
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
		if ( entityKey != null && session != null && session.isOpen() ) {
			return session.getPersistenceContext().getProxy( entityKey );
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
	public final Object getImplementation(SessionImplementor s) throws HibernateException {
		final EntityKey entityKey = generateEntityKeyOrNull( getIdentifier(), s, getEntityName() );
		return (entityKey == null ? null : s.getPersistenceContext().getEntity( entityKey ));
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
					"Proxy is detached (i.e, session is null). The read-only/modifiable setting is only accessible when the proxy is associated with an open session."
			);
		}
		if ( session.isClosed() ) {
			throw new SessionException(
					"Session is closed. The read-only/modifiable setting is only accessible when the proxy is associated with an open session."
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
				throw new IllegalStateException( "cannot make proxies for immutable entities modifiable" );
			}
			this.readOnly = readOnly;
			if ( initialized ) {
				EntityKey key = generateEntityKeyOrNull( getIdentifier(), session, getEntityName() );
				if ( key != null && session.getPersistenceContext().containsEntity( key ) ) {
					session.getPersistenceContext().setReadOnly( target, readOnly );
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
	protected final Boolean isReadOnlyBeforeAttachedToSession() {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call isReadOnlyBeforeAttachedToSession when isReadOnlySettingAvailable == true"
			);
		}
		return readOnlyBeforeAttachedToSession;
	}

	/**
	 * Set the read-only/modifiable setting that should be put in affect when it is
	 * attached to a session.
	 * <p/>
	 * This method should only be called during deserialization, before associating
	 * the proxy with a session.
	 *
	 * @param readOnlyBeforeAttachedToSession, the read-only/modifiable setting to use when
	 * associated with a session; null indicates that the default should be used.
	 *
	 * @throws IllegalStateException if isReadOnlySettingAvailable() == true
	 */
	/* package-private */
	final void setReadOnlyBeforeAttachedToSession(Boolean readOnlyBeforeAttachedToSession) {
		if ( isReadOnlySettingAvailable() ) {
			throw new IllegalStateException(
					"Cannot call setReadOnlyBeforeAttachedToSession when isReadOnlySettingAvailable == true"
			);
		}
		this.readOnlyBeforeAttachedToSession = readOnlyBeforeAttachedToSession;
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
