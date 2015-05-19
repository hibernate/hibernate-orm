/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;
import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Handles fetching of the underlying entity for a proxy
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface LazyInitializer {
	/**
	 * Initialize the proxy, fetching the target entity if necessary.
	 *
	 * @throws HibernateException Indicates a problem initializing the proxy.
	 */
	public void initialize() throws HibernateException;

	/**
	 * Retrieve the identifier value for the entity our owning proxy represents.
	 *
	 * @return The identifier value.
	 */
	public Serializable getIdentifier();

	/**
	 * Set the identifier value for the entity our owning proxy represents.
	 *
	 * @param id The identifier value.
	 */
	public void setIdentifier(Serializable id);

	/**
	 * The entity-name of the entity our owning proxy represents.
	 *
	 * @return The entity-name.
	 */
	public String getEntityName();

	/**
	 * Get the actual class of the entity.  Generally, {@link #getEntityName()} should be used instead.
	 *
	 * @return The actual entity class.
	 */
	public Class getPersistentClass();

	/**
	 * Is the proxy uninitialzed?
	 *
	 * @return True if uninitialized; false otherwise.
	 */
	public boolean isUninitialized();

	/**
	 * Return the underlying persistent object, initializing if necessary
	 *
	 * @return The underlying target entity.
	 */
	public Object getImplementation();

	/**
	 * Return the underlying persistent object in the given session, or null if not contained in this session's
	 * persistence context.
	 *
	 * @param session The session to check
	 *
	 * @return The target, or null.
	 *
	 * @throws HibernateException Indicates problem locating the target.
	 */
	public Object getImplementation(SessionImplementor session) throws HibernateException;

	/**
	 * Initialize the proxy manually by injecting its target.
	 *
	 * @param target The proxy target (the actual entity being proxied).
	 */
	public void setImplementation(Object target);

	/**
	 * Is the proxy's read-only/modifiable setting available?
	 * @return true, if the setting is available
	 *         false, if the proxy is detached or its associated session is closed
	 */
	public boolean isReadOnlySettingAvailable();

	/**
	 * Is the proxy read-only?.
	 *
	 * The read-only/modifiable setting is not available when the proxy is
	 * detached or its associated session is closed.
	 *
	 * To check if the read-only/modifiable setting is available:
	 *
	 * @return true, if this proxy is read-only; false, otherwise
	 * @throws org.hibernate.TransientObjectException if the proxy is detached (getSession() == null)
	 * @throws org.hibernate.SessionException if the proxy is associated with a sesssion that is closed
	 *
	 * @see org.hibernate.proxy.LazyInitializer#isReadOnlySettingAvailable()
	 * @see org.hibernate.Session#isReadOnly(Object entityOrProxy)
	 */
	public boolean isReadOnly();

	/**
	 * Set an associated modifiable proxy to read-only mode, or a read-only
	 * proxy to modifiable mode. If the proxy is currently initialized, its
	 * implementation will be set to the same mode; otherwise, when the
	 * proxy is initialized, its implementation will have the same read-only/
	 * modifiable setting as the proxy. In read-only mode, no snapshot is
	 * maintained and the instance is never dirty checked.
	 *
	 * If the associated proxy already has the specified read-only/modifiable
	 * setting, then this method does nothing.
	 *
	 * @param readOnly if true, the associated proxy is made read-only;
	 *                  if false, the associated proxy is made modifiable.
	 * @throws org.hibernate.TransientObjectException if the proxy is not association with a session
	 * @throws org.hibernate.SessionException if the proxy is associated with a session that is closed
	 * 
	 * @see org.hibernate.Session#setReadOnly(Object entityOrProxy, boolean readOnly)
	 */
	public void setReadOnly(boolean readOnly);

	/**
	 * Get the session to which this proxy is associated, or null if it is not attached.
	 *
	 * @return The associated session.
	 */
	public SessionImplementor getSession();

	/**
	 * Associate the proxy with the given session.
	 * <p/>
	 * Care should be given to make certain that the proxy is added to the session's persistence context as well
	 * to maintain the symetry of the association.  That must be done seperately as this method simply sets an
	 * internal reference.  We do also check that if there is already an associated session that the proxy
	 * reference was removed from that previous session's persistence contet.
	 *
	 * @param session The session
	 * @throws HibernateException Indicates that the proxy was still contained in the persistence context of the
	 * "previous session".
	 */
	public void setSession(SessionImplementor session) throws HibernateException;

	/**
	 * Unset this initializer's reference to session.  It is assumed that the caller is also taking care or
	 * cleaning up the owning proxy's reference in the persistence context.
	 * <p/>
	 * Generally speaking this is intended to be called only during {@link org.hibernate.Session#evict} and
	 * {@link org.hibernate.Session#clear} processing; most other use-cases should call {@link #setSession} instead.
	 */
	public void unsetSession();
	
	public void setUnwrap(boolean unwrap);
	public boolean isUnwrap();
}
