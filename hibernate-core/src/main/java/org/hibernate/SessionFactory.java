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
import org.hibernate.graph.RootGraph;
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
 * There are some interesting exceptions to this principle:
 * <ul>
 * <li>Each {@code SessionFactory} has its own isolated second-level cache,
 *     shared between the sessions it creates, and it {@link #getCache()
 *     exposes the cache} to clients as a stateful object with entries that
 *     may be queried and managed directly.
 * <li>Similarly, the factory {@linkplain #getStatistics() exposes} a
 *     {@link Statistics} object which accumulates information summarizing
 *     the activity of sessions created by the factory. It provides statistics
 *     about interactions with JDBC and with the second-level cache.
 * <li>Somewhat regrettably, The JPA 2.1 specification chose to locate the
 *     operations {@link #addNamedQuery(String, jakarta.persistence.Query)}
 *     and {@link #addNamedEntityGraph(String, EntityGraph)} on the interface
 *     {@link EntityManagerFactory} which {@code SessionFactory} inherits.
 *     Of course, these methods are usually called at the time the
 *     {@code EntityManagerFactory} is created. It's difficult to imagine a
 *     motivation to call either method later, when the factory already has
 *     active sessions.
 * </ul>
 * The {@code SessionFactory} exposes part of the information in the runtime
 * metamodel via an {@linkplain #getMetamodel() instance} of the JPA-defined
 * {@link jakarta.persistence.metamodel.Metamodel}. This object is sometimes
 * used in a sophisticated way by libraries or frameworks to implement generic
 * concerns involving entity classes.
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
	 * Open a {@link Session} and use it to perform an action.
	 */
	default void inSession(Consumer<Session> action) {
		try (Session session = openSession()) {
			action.accept( session );
		}
	}

	/**
	 * Open a {@link Session} and use it to perform an action
	 * within the bounds of a transaction.
	 */
	default void inTransaction(Consumer<Session> action) {
		inSession(
				session -> {
					final Transaction transaction = session.beginTransaction();
					try {
						action.accept( session );
						if ( !transaction.isActive() ) {
							throw new TransactionManagementException(
									"Execution of action caused managed transaction to be completed" );
						}
					}
					catch (RuntimeException exception) {
						// an error happened in the action
						if ( transaction.isActive() ) {
							try {
								transaction.rollback();
							}
							catch (Exception ignore) {
							}
						}

						throw exception;
					}
					// The action completed without throwing an exception,
					// so we attempt to commit the transaction, allowing
					// any RollbackException to propagate. Note that when
					// we get here we know that the transaction is active
					transaction.commit();
				}
		);
	}

	class TransactionManagementException extends RuntimeException {
		TransactionManagementException(String message) {
			super( message );
		}
	}

	/**
	 * Open a {@link Session} and use it to obtain a value.
	 */
	default <R> R fromSession(Function<Session,R> action) {
		try (Session session = openSession()) {
			return action.apply( session );
		}
	}

	/**
	 * Open a {@link Session} and use it to perform an action
	 * within the bounds of a transaction.
	 */
	default <R> R fromTransaction(Function<Session,R> action) {
		return fromSession(
				session -> {
					final Transaction transaction = session.beginTransaction();
					try {
						R result = action.apply( session );
						if ( !transaction.isActive() ) {
							throw new TransactionManagementException(
									"Execution of action caused managed transaction to be completed" );
						}
						// The action completed without throwing an exception,
						// so we attempt to commit the transaction, allowing
						// any RollbackException to propagate. Note that when
						// we get here we know that the transaction is active
						transaction.commit();
						return result;
					}
					catch (RuntimeException exception) {
						// an error happened in the action or during commit()
						if ( transaction.isActive() ) {
							try {
								transaction.rollback();
							}
							catch (Exception ignore) {
							}
						}
						throw exception;
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
	 * Return the root {@link EntityGraph} with the given name, or {@code null}
	 * if there is no graph with the given name.
	 *
	 * @param name the name given to some {@link jakarta.persistence.NamedEntityGraph}
	 * @return an instance of {@link RootGraph}
	 *
	 * @see #addNamedEntityGraph
	 */
	RootGraph<?> findEntityGraphByName(String name);

	/**
	 * Obtain the set of names of all {@link org.hibernate.annotations.FilterDef
	 * defined filters}.
	 *
	 * @return The set of filter names given by
	 *         {@link org.hibernate.annotations.FilterDef} annotations
	 */
	Set<String> getDefinedFilterNames();

	/**
	 * Obtain the definition of a filter by name.
	 *
	 * @param filterName The name of the filter for which to obtain the definition.
	 * @return The filter definition.
	 * @throws HibernateException If no filter defined with the given name.
	 *
	 * @deprecated There is no plan to remove this operation, but its use should be
	 *             avoided since {@link FilterDefinition} is an SPI type, and so this
	 *             operation is a layer-breaker.
	 */
	@Deprecated(since = "6.2")
	FilterDefinition getFilterDefinition(String filterName) throws HibernateException;

	/**
	 * Obtain the set of names of all {@link org.hibernate.annotations.FetchProfile
	 * defined fetch profiles}.
	 *
	 * @return The set of fetch profile names given by
	 *         {@link org.hibernate.annotations.FetchProfile} annotations.
	 */
	Set<String> getDefinedFetchProfileNames();

	/**
	 * Determine if there is a fetch profile definition registered under the given name.
	 *
	 * @param name The name to check
	 * @return True if there is such a fetch profile; false otherwise.
	 */
	default boolean containsFetchProfileDefinition(String name) {
		return getDefinedFilterNames().contains( name );
	}

}
