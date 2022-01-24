/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.naming.Referenceable;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;

/**
 * A {@code SessionFactory} represents an "instance" of Hibernate: it maintains
 * the runtime metamodel representing persistent entities, their attributes,
 * their associations, and their mappings to relational database tables, along
 * with {@linkplain org.hibernate.cfg.AvailableSettings configuration} that
 * affects the runtime behavior of Hibernate, and instances of services that
 * Hibernate needs to perform its duties.
 * <p>
 * Crucially, this is where a program comes to obtain {@link Session sessions}.
 * Typically, a program has a single {@link SessionFactory} instance, and must
 * obtain a new {@link Session} instance from the factory each time it services
 * a client request.
 * <p>
 * Depending on how Hibernate is configured, the {@code SessionFactory} itself
 * might be responsible for the lifecycle of pooled JDBC connections and
 * transactions, or it may simply act as a client for a connection pool or
 * transaction manager provided by a container environment.
 * <p>
 * The internal state of a {@code SessionFactory} is considered in some sense
 * "immutable". While it interacts with stateful services like JDBC connection
 * pools, such state changes are never visible to its clients. In particular,
 * the runtime metamodel representing the entities and their O/R mappings is
 * fixed as soon as the {@code SessionFactory} is created. Of course, any
 * {@code SessionFactory} is threadsafe.
 * <p>
 * Every {@code SessionFactory} is a JPA {@link EntityManagerFactory}.
 * Furthermore, when Hibernate is acting as the JPA persistence provider, the
 * method {@link EntityManagerFactory#unwrap(Class)} may be used to obtain the
 * underlying {@code SessionFactory}.
 *
 * @see Session
 * @see org.hibernate.cfg.Configuration
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactory extends EntityManagerFactory, Referenceable, Serializable, java.io.Closeable {
	/**
	 * Get the special options used to build the factory.
	 *
	 * @return The special options used to build the factory.
	 */
	SessionFactoryOptions getSessionFactoryOptions();

	/**
	 * Obtain a {@linkplain SessionBuilder session builder} for creating
	 * new {@link Session}s with certain customized options.
	 *
	 * @return The session builder
	 */
	SessionBuilder withOptions();

	/**
	 * Open a {@link Session}.
	 * <p/>
	 * Any JDBC {@link Connection connection} will be obtained lazily from the
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 * as needed to perform requested work.
	 *
	 * @return The created session.
	 *
	 * @throws HibernateException Indicates a problem opening the session; pretty rare here.
	 */
	Session openSession() throws HibernateException;

	/**
	 * Obtains the <em>current session</em>, an instance of {@link Session}
	 * implicitly associated with some context. For example, the session
	 * might be associated with the current thread, or with the current
	 * JTA transaction.
	 * <p>
	 * The context used for scoping the current session (that is, the
	 * definition of what precisely "current" means here) is determined
	 * by an implementation of
	 * {@link org.hibernate.context.spi.CurrentSessionContext}. An
	 * implementation may be selected using the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS}.
	 * <p>
	 * If no {@link org.hibernate.context.spi.CurrentSessionContext} is
	 * explicitly configured, but JTA is configured, then
	 * {@link org.hibernate.context.internal.JTASessionContext} is used.
	 *
	 * @return The current session.
	 *
	 * @throws HibernateException Indicates an issue locating a suitable current session.
	 */
	Session getCurrentSession() throws HibernateException;

	/**
	 * Obtain a {@link StatelessSession} builder.
	 *
	 * @return The stateless session builder
	 */
	StatelessSessionBuilder withStatelessOptions();

	/**
	 * Open a new stateless session.
	 *
	 * @return The created stateless session.
	 */
	StatelessSession openStatelessSession();

	/**
	 * Open a new stateless session, utilizing the specified JDBC
	 * {@link Connection}.
	 *
	 * @param connection Connection provided by the application.
	 *
	 * @return The created stateless session.
	 */
	StatelessSession openStatelessSession(Connection connection);

	/**
	 * Open a Session and perform a action using it
	 */
	default void inSession(Consumer<Session> action) {
		try (Session session = openSession()) {
			action.accept( session );
		}
	}

	/**
	 * Open a {@link Session} and perform an action using the session
	 * within the bounds of a transaction.
	 */
	default void inTransaction(Consumer<Session> action) {
		inSession(
				session -> {
					final Transaction txn = session.beginTransaction();

					try {
						action.accept( session );

						if ( !txn.isActive() ) {
							throw new TransactionManagementException(
									"Execution of action caused managed transaction to be completed" );
						}
					}
					catch (RuntimeException e) {
						// an error happened in the action
						if ( txn.isActive() ) {
							try {
								txn.rollback();
							}
							catch (Exception ignore) {
							}
						}

						throw e;
					}

					// action completed with no errors - attempt to commit the transaction allowing
					// 		any RollbackException to propagate.  Note that when we get here we know the
					//		txn is active

					txn.commit();
				}
		);
	}

	class TransactionManagementException extends RuntimeException {
		TransactionManagementException(String message) {
			super( message );
		}
	}

	/**
	 * Open a Session and perform an action using it.
	 */
	default <R> R fromSession(Function<Session,R> action) {
		try (Session session = openSession()) {
			return action.apply( session );
		}
	}

	/**
	 * Open a {@link Session} and perform an action using the session
	 * within the bounds of a transaction.
	 */
	default <R> R fromTransaction(Function<Session,R> action) {
		return fromSession(
				session -> {
					final Transaction txn = session.beginTransaction();
					try {
						R result = action.apply( session );

						if ( !txn.isActive() ) {
							throw new TransactionManagementException(
									"Execution of action caused managed transaction to be completed" );
						}

						// action completed with no errors - attempt to commit the transaction allowing
						// 		any RollbackException to propagate.  Note that when we get here we know the
						//		txn is active

						txn.commit();

						return result;
					}
					catch (RuntimeException e) {
						// an error happened in the action
						if ( txn.isActive() ) {
							try {
								txn.rollback();
							}
							catch (Exception ignore) {
							}
						}

						throw e;
					}
				}
		);
	}

	/**
	 * Retrieve the {@linkplain Statistics statistics} for this factory.
	 *
	 * @return The statistics.
	 */
	Statistics getStatistics();

	/**
	 * Destroy this {@code SessionFactory} and release all its resources,
	 * including caches and connection pools.
	 * <p/>
	 * It is the responsibility of the application to ensure that there are
	 * no open {@linkplain Session sessions} before calling this method as
	 * the impact on those {@linkplain Session sessions} is indeterminate.
	 * <p/>
	 * No-ops if already {@linkplain #isClosed() closed}.
	 *
	 * @throws HibernateException Indicates an issue closing the factory.
	 */
	void close() throws HibernateException;

	/**
	 * Is this factory already closed?
	 *
	 * @return True if this factory is already closed; false otherwise.
	 */
	boolean isClosed();

	/**
	 * Obtain direct access to the underlying cache regions.
	 *
	 * @return The direct cache access API.
	 */
	@Override
	Cache getCache();

	/**
	 * Return all {@link EntityGraph}s registered for the given entity type.
	 * 
	 * @see #addNamedEntityGraph 
	 */
	<T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass);

	/**
	 * Obtain the set of names of all {@link FilterDefinition defined filters}.
	 *
	 * @return The set of filter names.
	 */
	Set<String> getDefinedFilterNames();

	/**
	 * Obtain the definition of a filter by name.
	 *
	 * @param filterName The name of the filter for which to obtain the definition.
	 * @return The filter definition.
	 * @throws HibernateException If no filter defined with the given name.
	 */
	FilterDefinition getFilterDefinition(String filterName) throws HibernateException;

	/**
	 * Determine if this session factory contains a fetch profile definition
	 * registered under the given name.
	 *
	 * @param name The name to check
	 * @return True if there is such a fetch profile; false otherwise.
	 */
	boolean containsFetchProfileDefinition(String name);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * Retrieve the {@link ClassMetadata} associated with the given entity class.
	 *
	 * @param entityClass The entity class
	 *
	 * @return The metadata associated with the given entity; may be null if no such
	 * entity was mapped.
	 *
	 * @throws HibernateException Generally null is returned instead of throwing.
	 *
	 * @deprecated Use the descriptors from {@link #getMetamodel()} instead
	 */
	@Deprecated
	ClassMetadata getClassMetadata(@SuppressWarnings("rawtypes") Class entityClass);

	/**
	 * Retrieve the {@link ClassMetadata} associated with the given entity class.
	 *
	 * @param entityName The entity class
	 *
	 * @return The metadata associated with the given entity; may be null if no such
	 * entity was mapped.
	 *
	 * @throws HibernateException Generally null is returned instead of throwing.
	 * @since 3.0
	 *
	 * @deprecated Use the descriptors from {@link #getMetamodel()} instead
	 */
	@Deprecated
	ClassMetadata getClassMetadata(String entityName);

	/**
	 * Get the {@link CollectionMetadata} associated with the named collection role.
	 *
	 * @param roleName The collection role (in form [owning-entity-name].[collection-property-name]).
	 *
	 * @return The metadata associated with the given collection; may be null if no such
	 * collection was mapped.
	 *
	 * @throws HibernateException Generally null is returned instead of throwing.
	 *
	 * @deprecated Use the descriptors from {@link #getMetamodel()} instead
	 */
	@Deprecated
	CollectionMetadata getCollectionMetadata(String roleName);
}
