/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.proxy;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.TransientObjectException;
import org.hibernate.SessionException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Convenience base class for lazy initialization handlers.  Centralizes the basic plumbing of doing lazy
 * initialization freeing subclasses to acts as essentially adapters to their intended entity mode and/or
 * proxy generation strategy.
 *
 * @author Gavin King
 */
public abstract class AbstractLazyInitializer implements LazyInitializer {
	private String entityName;
	private Serializable id;
	private Object target;
	private boolean initialized;
	private boolean readOnly;
	private boolean unwrap;
	private transient SessionImplementor session;
	private Boolean readOnlyBeforeAttachedToSession;

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

	/**
	 * {@inheritDoc}
	 */
	public final String getEntityName() {
		return entityName;
	}

	/**
	 * {@inheritDoc}
	 */
	public final Serializable getIdentifier() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setIdentifier(Serializable id) {
		this.id = id;
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean isUninitialized() {
		return !initialized;
	}

	/**
	 * {@inheritDoc}
	 */
	public final SessionImplementor getSession() {
		return session;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setSession(SessionImplementor s) throws HibernateException {
		if ( s != session ) {
			// check for s == null first, since it is least expensive
			if ( s == null ){
				unsetSession();
			}
			else if ( isConnectedToSession() ) {
				//TODO: perhaps this should be some other RuntimeException...
				throw new HibernateException("illegally attempted to associate a proxy with two open Sessions");
			}
			else {
				// s != null
				session = s;
				if ( readOnlyBeforeAttachedToSession == null ) {
					// use the default read-only/modifiable setting
					final EntityPersister persister = s.getFactory().getEntityPersister( entityName );
					setReadOnly( s.getPersistenceContext().isDefaultReadOnly() || ! persister.isMutable() );
				}
				else {
					// use the read-only/modifiable setting indicated during deserialization
					setReadOnly( readOnlyBeforeAttachedToSession.booleanValue() );
					readOnlyBeforeAttachedToSession = null;
				}
			}
		}
	}

	private static EntityKey generateEntityKeyOrNull(Serializable id, SessionImplementor s, String entityName) {
		if ( id == null || s == null || entityName == null ) {
			return null;
		}
		return new EntityKey( id, s.getFactory().getEntityPersister( entityName ), s.getEntityMode() );
	}

	/**
	 * {@inheritDoc}
	 */
	public final void unsetSession() {
		session = null;
		readOnly = false;
		readOnlyBeforeAttachedToSession = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void initialize() throws HibernateException {
		if (!initialized) {
			if ( session==null ) {
				throw new LazyInitializationException("could not initialize proxy - no Session");
			}
			else if ( !session.isOpen() ) {
				throw new LazyInitializationException("could not initialize proxy - the owning Session was closed");
			}
			else if ( !session.isConnected() ) {
				throw new LazyInitializationException("could not initialize proxy - the owning Session is disconnected");
			}
			else {
				target = session.immediateLoad(entityName, id);
				initialized = true;
				checkTargetState();
			}
		}
		else {
			checkTargetState();
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

	/**
	 * Return the underlying persistent object, initializing if necessary
	 */
	public final Object getImplementation() {
		initialize();
		return target;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setImplementation(Object target) {
		this.target = target;
		initialized = true;
	}

	/**
	 * Return the underlying persistent object in the given <tt>Session</tt>, or null,
	 * do not initialize the proxy
	 */
	public final Object getImplementation(SessionImplementor s) throws HibernateException {
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

	/**
	 * {@inheritDoc}
	 */
	public final boolean isReadOnlySettingAvailable() {
		return ( session != null && ! session.isClosed() );
	}

	private void errorIfReadOnlySettingNotAvailable() {
		if ( session == null ) {
			throw new TransientObjectException(
					"Proxy is detached (i.e, session is null). The read-only/modifiable setting is only accessible when the proxy is associated with an open session." );
		}
		if ( session.isClosed() ) {
			throw new SessionException(
					"Session is closed. The read-only/modifiable setting is only accessible when the proxy is associated with an open session." );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean isReadOnly() {
		errorIfReadOnlySettingNotAvailable();
		return readOnly;
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setReadOnly(boolean readOnly) {
		errorIfReadOnlySettingNotAvailable();
		// only update if readOnly is different from current setting
		if ( this.readOnly != readOnly ) {
			final EntityPersister persister = session.getFactory().getEntityPersister( entityName );
			if ( ! persister.isMutable() && ! readOnly ) {
				throw new IllegalStateException( "cannot make proxies for immutable entities modifiable");
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
	 *
	 * This method should only be called during serialization when read-only/modifiable setting
	 * is not available (i.e., isReadOnlySettingAvailable() == false)
	 *
	 * @returns, null, if the default setting should be used;
	 *           true, for read-only;
	 *           false, for modifiable
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
	 *
	 * This method should only be called during deserialization, before associating
	 * the proxy with a session.
	 *
	 * @param readOnlyBeforeAttachedToSession, the read-only/modifiable setting to use when
	 * associated with a session; null indicates that the default should be used.
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

	/**
	 * {@inheritDoc}
	 */
	public boolean isUnwrap() {
		return unwrap;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setUnwrap(boolean unwrap) {
		this.unwrap = unwrap;
	}
}
