/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import jakarta.persistence.EntityGraph;
import org.hibernate.graph.GraphSemantic;

/**
 * A command-oriented API often used for performing bulk operations against
 * the database. A stateless session has no persistence context, and always
 * works directly with detached entity instances. When a method of this
 * interface is called, any necessary interaction with the database happens
 * immediately and synchronously.
 * <p>
 * Viewed in opposition to {@link Session}, the {@code StatelessSession} is
 * a whole competing programming model, one preferred by some developers
 * for its simplicity and somewhat lower level of abstraction. But the two
 * kinds of session are not enemies, and may comfortably coexist in a single
 * program.
 * <p>
 * A stateless session comes some with designed-in limitations:
 * <ul>
 * <li>it does not have a first-level cache,
 * <li>nor interact with any second-level cache,
 * <li>nor does it implement transactional write-behind or automatic dirty
 *     checking.
 * </ul>
 * <p>
 * Furthermore, operations performed via a stateless session:
 * <ul>
 * <li>never cascade to associated instances, no matter what the
 *     {@link jakarta.persistence.CascadeType}, and
 * <li>bypass Hibernate's {@linkplain org.hibernate.event.spi event model},
 *     lifecycle callbacks, and {@linkplain Interceptor interceptors}.
 * </ul>
 * <p>
 * Stateless sessions are vulnerable to data aliasing effects, due to the
 * lack of a first-level cache.
 * <p>
 * On the other hand, for certain kinds of transactions, a stateless session
 * may perform slightly faster than a stateful session.
 *
 * @author Gavin King
 */
public interface StatelessSession extends SharedSessionContract {
	/**
	 * Close the stateless session and release the JDBC connection.
	 */
	void close();

	/**
	 * Insert a row.
	 *
	 * @param entity a new transient instance
	 *
	 * @return The identifier of the inserted entity
	 */
	Object insert(Object entity);

	/**
	 * Insert a row.
	 *
	 * @param entityName The entityName for the entity to be inserted
	 * @param entity a new transient instance
	 *
	 * @return the identifier of the instance
	 */
	Object insert(String entityName, Object entity);

	/**
	 * Update a row.
	 *
	 * @param entity a detached entity instance
	 */
	void update(Object entity);

	/**
	 * Update a row.
	 *
	 * @param entityName The entityName for the entity to be updated
	 * @param entity a detached entity instance
	 */
	void update(String entityName, Object entity);

	/**
	 * Delete a row.
	 *
	 * @param entity a detached entity instance
	 */
	void delete(Object entity);

	/**
	 * Delete a row.
	 *
	 * @param entityName The entityName for the entity to be deleted
	 * @param entity a detached entity instance
	 */
	void delete(String entityName, Object entity);

	/**
	 * Use a SQL {@code merge into} statement to perform an upsert.
	 *
	 * @param entity a detached entity instance
	 * @throws TransientObjectException is the entity is transient
	 *
	 * @since 6.3
	 */
	@Incubating
	void upsert(Object entity);

	/**
	 * Use a SQL {@code merge into} statement to perform an upsert.
	 *
	 * @param entityName The entityName for the entity to be merged
	 * @param entity a detached entity instance
	 * @throws TransientObjectException is the entity is transient
	 *
	 * @since 6.3
	 */
	@Incubating
	void upsert(String entityName, Object entity);

	/**
	 * Retrieve a row.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	Object get(String entityName, Object id);

	/**
	 * Retrieve a row.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	<T> T get(Class<T> entityClass, Object id);

	/**
	 * Retrieve a row, obtaining the specified lock mode.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	Object get(String entityName, Object id, LockMode lockMode);

	/**
	 * Retrieve a row, obtaining the specified lock mode.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	<T> T get(Class<T> entityClass, Object id, LockMode lockMode);

	/**
	 * Retrieve a row, fetching associations specified by the
	 * given {@link EntityGraph}.
	 *
	 * @param graph The {@link EntityGraph}
	 * @param graphSemantic a {@link GraphSemantic} specifying
	 *                      how the graph should be interpreted
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 *
	 * @since 6.3
	 */
	<T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id);

	/**
	 * Retrieve a row, fetching associations specified by the
	 * given {@link EntityGraph}, and obtaining the specified
	 * lock mode.
	 *
	 * @param graph The {@link EntityGraph}
	 * @param graphSemantic a {@link GraphSemantic} specifying
	 *                      how the graph should be interpreted
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 *
	 * @since 6.3
	 */
	<T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entity The entity to be refreshed.
	 */
	void refresh(Object entity);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entityName The entityName for the entity to be refreshed.
	 * @param entity The entity to be refreshed.
	 */
	void refresh(String entityName, Object entity);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entity The entity to be refreshed.
	 * @param lockMode The LockMode to be applied.
	 */
	void refresh(Object entity, LockMode lockMode);

	/**
	 * Refresh the entity instance state from the database.
	 *
	 * @param entityName The entityName for the entity to be refreshed.
	 * @param entity The entity to be refreshed.
	 * @param lockMode The LockMode to be applied.
	 */
	void refresh(String entityName, Object entity, LockMode lockMode);

	/**
	 * Fetch an association that's configured for lazy loading.
	 * <p>
	 * Warning: this operation in a stateless session is quite sensitive
	 * to data aliasing effects and should be used with great care.
	 *
	 * @param association a lazy-loaded association
	 *
	 * @see org.hibernate.Hibernate#initialize(Object)
	 *
	 * @since 6.0
	 */
	void fetch(Object association);
}
