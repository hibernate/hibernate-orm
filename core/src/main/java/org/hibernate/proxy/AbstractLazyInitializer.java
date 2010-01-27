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

import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.TransientObjectException;
import org.hibernate.SessionException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;

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
		this.readOnly = false;
		// initialize other fields depending on session state
		if ( session == null ) {
			// would be better to call unsetSession(), but it is not final...
			session = null;
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
			readOnly = false;
			// check for s == null first, since it is least expensive
			if ( s == null ){
				// would be better to call unsetSession(), but it is not final...
				session = null;
			}
			else if ( isConnectedToSession() ) {
				//TODO: perhaps this should be some other RuntimeException...
				throw new HibernateException("illegally attempted to associate a proxy with two open Sessions");
			}
			else {
				session = s;
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
	public void unsetSession() {
		session = null;
		readOnly = false;
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
	public boolean isReadOnly() {
		errorIfReadOnlySettingNotAvailable();
		if ( !isConnectedToSession() ) {
			throw new TransientObjectException(
					"The read-only/modifiable setting is only accessible when the proxy is associated with a session." );
		}
		return readOnly;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setReadOnly(boolean readOnly) {
		errorIfReadOnlySettingNotAvailable();
		// only update if readOnly is different from current setting
		if ( this.readOnly != readOnly ) {
			Object proxy = getProxyOrNull();
			if ( proxy == null ) {
				throw new TransientObjectException(
						"Cannot set the read-only/modifiable mode unless the proxy is associated with a session." );
			}
			this.readOnly = readOnly;
			if ( initialized ) {
				session.getPersistenceContext().setReadOnly( target, readOnly );
			}
		}
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
