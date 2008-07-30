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