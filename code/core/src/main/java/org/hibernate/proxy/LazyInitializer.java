//$Id: LazyInitializer.java 7246 2005-06-20 20:32:36Z oneovthafew $
package org.hibernate.proxy;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * Handles fetching of the underlying entity for a proxy
 * @author Gavin King
 */
public interface LazyInitializer {
	
	/**
	 * Initialize the proxy, fetching the target
	 * entity if necessary
	 */
	public abstract void initialize() throws HibernateException;
	
	/**
	 * Get the identifier held by the proxy
	 */
	public abstract Serializable getIdentifier();

	/**
	 * Set the identifier property of the proxy
	 */
	public abstract void setIdentifier(Serializable id);
	
	/**
	 * Get the entity name
	 */
	public abstract String getEntityName();
	
	/**
	 * Get the actual class of the entity (don't
	 * use this, use the entityName)
	 */
	public abstract Class getPersistentClass();
	
	/**
	 * Is the proxy uninitialzed?
	 */
	public abstract boolean isUninitialized();
	
	/**
	 * Initialize the proxy manually
	 */
	public abstract void setImplementation(Object target);
	
	/**
	 * Get the session, if this proxy is attached
	 */
	public abstract SessionImplementor getSession();
	
	/**
	 * Attach the proxy to a session
	 */
	public abstract void setSession(SessionImplementor s) throws HibernateException;

	/**
	 * Return the underlying persistent object, initializing if necessary
	 */
	public abstract Object getImplementation();

	/**
	 * Return the underlying persistent object in the given <tt>Session</tt>, or null
	 */
	public abstract Object getImplementation(SessionImplementor s)
			throws HibernateException;
	
	public void setUnwrap(boolean unwrap);
	public boolean isUnwrap();
}