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
 * Furthermore, the basic operations of a stateless session do not have
 * corresponding {@linkplain jakarta.persistence.CascadeType cascade types},
 * and so an operation performed via a stateless session never cascades to
 * associated instances.
 * <p>
 * The basic operations of a stateless session are {@link #get(Class, Object)},
 * {@link #insert(Object)}, {@link #update(Object)}, {@link #delete(Object)},
 * and {@link #upsert(Object)}. These operations are always performed
 * synchronously, resulting in immediate access to the database. Notice that
 * update is an explicit operation. There is no "flush" operation for a
 * stateless session, and so modifications to entities are never automatically
 * detected and made persistent.
 * <p>
 * Similarly, lazy association fetching is an explicit operation. A collection
 * or proxy may be fetched by calling {@link #fetch(Object)}.
 * <p>
 * Stateless sessions are vulnerable to data aliasing effects, due to the
 * lack of a first-level cache.
 * <p>
 * On the other hand, for certain kinds of transactions, a stateless session
 * may perform slightly faster than a stateful session.
 * <p>
 * Certain rules applying to stateful sessions are relaxed in a stateless
 * session:
 * <ul>
 * <li>it is not necessary to discard a session and its entities after an
 *     exception is thrown by a stateless sessions, and
 * <li>when an exception is thrown by a stateless session, the current
 *     transaction is not automatically marked for rollback.
 * </ul>
 *
 * @author Gavin King
 */
public interface StatelessSession extends SharedSessionContract {
	/**
	 * Close the stateless session and release the JDBC connection.
	 */
	void close();

	/**
	 * Insert a record.
	 * <p>
	 * If the entity {@code @Id} field is declared to be generated,
	 * for example, if it is annotated {@code @GeneratedId}, the id
	 * is generated and assigned to the given instance.
	 * <p>
	 * The {@link jakarta.persistence.PostPersist} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entity a new transient instance
	 *
	 * @return The identifier of the inserted entity
	 */
	Object insert(Object entity);

	/**
	 * Insert a record.
	 * <p>
	 * The {@link jakarta.persistence.PostPersist} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entityName The entityName for the entity to be inserted
	 * @param entity a new transient instance
	 *
	 * @return the identifier of the instance
	 */
	Object insert(String entityName, Object entity);

	/**
	 * Update a record.
	 * <p>
	 * The {@link jakarta.persistence.PostUpdate} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entity a detached entity instance
	 */
	void update(Object entity);

	/**
	 * Update a record.
	 * <p>
	 * The {@link jakarta.persistence.PostUpdate} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entityName The entityName for the entity to be updated
	 * @param entity a detached entity instance
	 */
	void update(String entityName, Object entity);

	/**
	 * Delete a record.
	 * <p>
	 * The {@link jakarta.persistence.PostRemove} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entity a detached entity instance
	 */
	void delete(Object entity);

	/**
	 * Delete a record.
	 * <p>
	 * The {@link jakarta.persistence.PostRemove} callback will be
	 * triggered if the operation is successful.
	 *
	 * @param entityName The entityName for the entity to be deleted
	 * @param entity a detached entity instance
	 */
	void delete(String entityName, Object entity);

	/**
	 * Use a SQL {@code merge into} statement to perform an upsert,
	 * that is, to insert the record if it does not exist, or update
	 * it if it already exists.
	 * <p>
	 * This method never performs id generation, and does not accept
	 * an entity instance with a null identifier. When id generation
	 * is required, use {@link #insert(Object)}.
	 * <p>
	 * On the other hand, {@code upsert()} does accept an entity
	 * instance with an assigned identifier value, even if the entity
	 * {@code @Id} field is declared to be generated, for example, if
	 * it is annotated {@code @GeneratedId}. Thus, this method may be
	 * used to import data from an external source.
	 *
	 * @param entity a detached entity instance, or a new instance
	 *               with an assigned identifier
	 * @throws TransientObjectException is the entity has a null id
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
	 * @throws TransientObjectException is the entity has a null id
	 *
	 * @since 6.3
	 */
	@Incubating
	void upsert(String entityName, Object entity);

	/**
	 * Retrieve a record.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	Object get(String entityName, Object id);

	/**
	 * Retrieve a record.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 *
	 * @return a detached entity instance
	 */
	<T> T get(Class<T> entityClass, Object id);

	/**
	 * Retrieve a record, obtaining the specified lock mode.
	 *
	 * @param entityName The name of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	Object get(String entityName, Object id, LockMode lockMode);

	/**
	 * Retrieve a record, obtaining the specified lock mode.
	 *
	 * @param entityClass The class of the entity to retrieve
	 * @param id The id of the entity to retrieve
	 * @param lockMode The lock mode to apply to the entity
	 *
	 * @return a detached entity instance
	 */
	<T> T get(Class<T> entityClass, Object id, LockMode lockMode);

	/**
	 * Retrieve a record, fetching associations specified by the
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
	 * Retrieve a record, fetching associations specified by the
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
	 * Fetch an association or collection that's configured for lazy loading.
	 * <pre>
	 * Book book = session.get(Book.class, isbn);  // book is immediately detached
	 * session.fetch(book.getAuthors());           // fetch the associated authors
	 * book.getAuthors().forEach(author -> ... );  // iterate the collection
	 * </pre>
	 * <p>
	 * Warning: this operation in a stateless session is quite sensitive
	 * to data aliasing effects and should be used with great care. It's
	 * usually better to fetch associations using eager join fetching.
	 *
	 * @param association a lazy-loaded association
	 *
	 * @see org.hibernate.Hibernate#initialize(Object)
	 *
	 * @since 6.0
	 */
	void fetch(Object association);

	/**
	 * Return the identifier value of the given entity, which may be detached.
	 *
	 * @param entity a persistent instance associated with this session
	 *
	 * @return the identifier
	 *
	 * @since 6.6
	 */
	Object getIdentifier(Object entity);
}
