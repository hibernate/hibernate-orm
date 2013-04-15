/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;

/**
 * A command-oriented API for performing bulk operations against a database.
 * <p/>
 * A stateless session does not implement a first-level cache nor interact
 * with any second-level cache, nor does it implement transactional
 * write-behind or automatic dirty checking, nor do operations cascade to
 * associated instances. Collections are ignored by a stateless session.
 * Operations performed via a stateless session bypass Hibernate's event model
 * and interceptors.  Stateless sessions are vulnerable to data aliasing
 * effects, due to the lack of a first-level cache.
 * <p/>
 * For certain kinds of transactions, a stateless session may perform slightly
 * faster than a stateful session.
 *
 * @author Gavin King
 */
public interface StatelessSession extends SharedSessionContract {
	/**
	 * Close the stateless session and release the JDBC connection.
	 */
	public void close();

	/**
	 * Insert a row.
	 *
	 * @param entity a new transient instance
	 *
	 * @return The identifier of the inserted entity
	 */
	public Serializable insert(Object entity);

	/**
	 * Insert a row.
	 *
	 * @param entityName The entityName for the entity to be inserted
	 * @param entity a new transient instance
	 *
	 * @return the identifier of the instance
	 */
	public Serializable insert(String entityName, Object entity);

	/**
	 * Update a row.
	 *
	 * @param entity a detached entity instance
	 */
	public void update(Object entity);

	/**
	 * Update a row.
	 *
	 * @param entityName The entityName for the entity to be updated
	 * @param entity a detached entity instance
	 */
	public void update(String entityName, Object entity);

	/**
	 * Delete a row.
	 *
	 * @param entity a detached entity instance
	 */
	public void delete(Object entity);

	/**
	 * Delete a row.
	 *
	 * @param entityName The entityName for the entity to be deleted
	 * @param entity a detached entity instance
	 */
	public void delete(String entityName, Object entity);

	/**
	 * Retrieve a row.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	public Object get(String entityName, Serializable id);

	/**
	 * Retrieve a row.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	public Object get(Class entityClass, Serializable id);

	/**
	 * Retrieve a row, obtaining the specified lock mode.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	public Object get(String entityName, Serializable id, LockMode lockMode);

	/**
	 * Retrieve a row, obtaining the specified lock mode.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	public Object get(Class entityClass, Serializable id, LockMode lockMode);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entity The entity to be refreshed.
	 */
	public void refresh(Object entity);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entityName The entityName for the entity to be refreshed.
	 * @param entity The entity to be refreshed.
	 */
	public void refresh(String entityName, Object entity);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entity The entity to be refreshed.
	 * @param lockMode The LockMode to be applied.
	 */
	public void refresh(Object entity, LockMode lockMode);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entityName The entityName for the entity to be refreshed.
	 * @param entity The entity to be refreshed.
	 * @param lockMode The LockMode to be applied.
	 */
	public void refresh(String entityName, Object entity, LockMode lockMode);

	/**
	 * Returns the current JDBC connection associated with this
	 * instance.<br>
	 * <br>
	 * If the session is using aggressive connection release (as in a
	 * CMT environment), it is the application's responsibility to
	 * close the connection returned by this call. Otherwise, the
	 * application should not close the connection.
	 *
	 * @deprecated just missed when deprecating same method from {@link Session}
	 *
	 * @return The connection associated with this stateless session
	 */
	@Deprecated
	public Connection connection();
}
