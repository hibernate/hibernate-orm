/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.internal.RootGraphImpl;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.relational.SchemaManager;
import org.hibernate.stat.Statistics;

import javax.naming.Referenceable;
import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.internal.TransactionManagement.manageTransaction;

/**
 * A {@code SessionFactory} represents an "instance" of Hibernate: it maintains
 * the runtime metamodel representing persistent entities, their attributes,
 * their associations, and their mappings to relational database tables, along
 * with {@linkplain org.hibernate.cfg.AvailableSettings configuration} that
 * affects the runtime behavior of Hibernate, and instances of services that
 * Hibernate needs to perform its duties.
 * <p>
 * Crucially, this is where a program comes to obtain {@linkplain Session sessions}.
 * Typically, a program has a single {@link SessionFactory} instance, and must
 * obtain a new {@link Session} instance from the factory each time it services
 * a client request. It is then also responsible for {@linkplain Session#close()
 * destroying} the session at the end of the client request.
 * <p>
 * The {@link #inSession} and {@link #inTransaction} methods provide a convenient
 * way to obtain a session, with or without starting a transaction, and have it
 * cleaned up automatically, relieving the program of the need to explicitly
 * call {@link Session#close()} and {@link Transaction#commit()}.
 * <p>
 * Alternatively, {@link #getCurrentSession()} provides support for the notion
 * of contextually-scoped sessions, where an implementation of the SPI interface
 * {@link org.hibernate.context.spi.CurrentSessionContext} is responsible for
 * creating, scoping, and destroying sessions.
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
 *     shared between the sessions it creates, and it {@linkplain #getCache()
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
 * <p>
 * The {@code SessionFactory} exposes part of the information in the runtime
 * metamodel via an {@linkplain #getMetamodel() instance} of the JPA-defined
 * {@link jakarta.persistence.metamodel.Metamodel}. This object is sometimes
 * used in a sophisticated way by libraries or frameworks to implement generic
 * concerns involving entity classes.
 * <p>
 * When Hibernate Processor is used, elements of this metamodel may also be
 * obtained in a typesafe way, via the generated metamodel classes. For an
 * entity class {@code Book}, the generated {@code Book_} class has:
 * <ul>
 * <li>a single member named {@code class_} of type
 *     {@link jakarta.persistence.metamodel.EntityType EntityType&lt;Book&gt;},
 *     and
 * <li>a member for each persistent attribute of {@code Book}, for example,
 *     {@code title} of type {@link jakarta.persistence.metamodel.SingularAttribute
 *     SingularAttribute&lt;Book,String&gt;}.
 * </ul>
 * <p>
 * Use of these statically-typed metamodel references is the preferred way of
 * working with the {@linkplain jakarta.persistence.criteria.CriteriaBuilder
 * criteria query API}, and with {@linkplain EntityGraph}s.
 * <p>
 * The factory also {@linkplain #getSchemaManager() provides} a
 * {@link SchemaManager} which allows, as a convenience for writing tests:
 * <ul>
 * <li>programmatic {@linkplain SchemaManager#exportMappedObjects(boolean)
 *     schema export} and {@linkplain SchemaManager#dropMappedObjects(boolean)
 *     schema removal},
 * <li>schema {@linkplain SchemaManager#validateMappedObjects() validation},
 *     and
 * <li>an operation for {@linkplain SchemaManager#truncateMappedObjects()
 *     cleaning up} data left behind by tests.
 * </ul>
 * <p>
 * Finally, the factory {@linkplain #getCriteriaBuilder() provides} a
 * {@link HibernateCriteriaBuilder}, an extension to the JPA-defined interface
 * {@link jakarta.persistence.criteria.CriteriaBuilder}, which may be used to
 * construct {@linkplain jakarta.persistence.criteria.CriteriaQuery criteria
 * queries}.
 * <p>
 * Every {@code SessionFactory} is a JPA {@link EntityManagerFactory}.
 * Furthermore, when Hibernate is acting as the JPA persistence provider, the
 * method {@link EntityManagerFactory#unwrap(Class)} may be used to obtain the
 * underlying {@code SessionFactory}.
 * <p>
 * The very simplest way to obtain a new {@code SessionFactory} is using a
 * {@link org.hibernate.cfg.Configuration} or
 * {@link org.hibernate.jpa.HibernatePersistenceConfiguration}.
 *
 * @see Session
 * @see org.hibernate.cfg.Configuration
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface SessionFactory extends EntityManagerFactory, Referenceable, Serializable {
	/**
	 * The JNDI name, used to bind the {@code SessionFactory} to JNDI.
	 */
	String getJndiName();

	/**
	 * Obtain a {@linkplain org.hibernate.SessionBuilder session builder}
	 * for creating new instances of {@link org.hibernate.Session} with
	 * certain customized options.
	 *
	 * @return The session builder
	 */
	SessionBuilder withOptions();

	/**
	 * Open a {@link Session}.
	 * <p>
	 * Any JDBC {@link Connection connection} will be obtained lazily from the
	 * {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 * as needed to perform requested work.
	 *
	 * @return The created session.
	 *
	 * @apiNote This operation is very similar to {@link #createEntityManager()}
	 *
	 * @throws HibernateException Indicates a problem opening the session; pretty rare here.
	 */
	Session openSession() throws HibernateException;

	/**
	 * Obtains the <em>current session</em>, an instance of {@link Session}
	 * implicitly associated with some context or scope. For example, the
	 * session might be associated with the current thread, or with the
	 * current JTA transaction.
	 * <p>
	 * The context used for scoping the current session (that is, the
	 * definition of what precisely "current" means here) is determined
	 * by an implementation of
	 * {@link org.hibernate.context.spi.CurrentSessionContext}. An
	 * implementation may be selected using the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS}.
	 * <p>
	 * If no {@link org.hibernate.context.spi.CurrentSessionContext} is
	 * explicitly configured, but JTA support is enabled, then
	 * {@link org.hibernate.context.internal.JTASessionContext} is used,
	 * and the current session is scoped to the active JTA transaction.
	 *
	 * @return The current session.
	 *
	 * @throws HibernateException Indicates an issue locating a suitable current session.
	 *
	 * @see org.hibernate.context.spi.CurrentSessionContext
	 * @see org.hibernate.cfg.AvailableSettings#CURRENT_SESSION_CONTEXT_CLASS
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
	 * @return The new stateless session.
	 */
	StatelessSession openStatelessSession();

	/**
	 * Open a new stateless session, utilizing the specified JDBC
	 * {@link Connection}.
	 *
	 * @param connection Connection provided by the application.
	 *
	 * @return The new stateless session.
	 */
	StatelessSession openStatelessSession(Connection connection);

	/**
	 * Open a {@link Session} and use it to perform the given action.
	 *
	 * @apiNote This method does not begin a transaction, and so
	 * the session is not automatically flushed before the method
	 * returns unless either:
	 * <ul>
	 * <li>the given action calls {@link Session#flush() flush()}
	 *     explicitly, or
	 * <li>a transaction is initiated by the given action, using
	 *     {@link Session#inTransaction}, for example.
	 * </ul>
	 *
	 * @see #inTransaction(Consumer)
	 */
	default void inSession(Consumer<? super Session> action) {
		try ( Session session = openSession() ) {
			action.accept( session );
		}
	}

	/**
	 * Open a {@link StatelessSession} and use it to perform the
	 * given action.
	 *
	 * @apiNote This method does not begin a transaction, and so
	 * the session is not automatically flushed before the method
	 * returns unless either:
	 * <ul>
	 * <li>the given action calls {@link Session#flush() flush()}
	 *     explicitly, or
	 * <li>a transaction is initiated by the given action, using
	 *     {@link Session#inTransaction}, for example.
	 * </ul>
	 *
	 * @see #inStatelessTransaction(Consumer)
	 *
	 * @since 6.3
	 */
	default void inStatelessSession(Consumer<? super StatelessSession> action) {
		try ( StatelessSession session = openStatelessSession() ) {
			action.accept( session );
		}
	}

	/**
	 * Open a {@link Session} and use it to perform the given action
	 * within the bounds of a transaction.
	 *
	 * @apiNote This method competes with the JPA-defined method
	 *          {@link #runInTransaction}
	 */
	default void inTransaction(Consumer<? super Session> action) {
		inSession( session -> manageTransaction( session, session.beginTransaction(), action ) );
	}

	/**
	 * Open a {@link StatelessSession} and use it to perform an action
	 * within the bounds of a transaction.
	 *
	 * @since 6.3
	 */
	default void inStatelessTransaction(Consumer<? super StatelessSession> action) {
		inStatelessSession( session -> manageTransaction( session, session.beginTransaction(), action ) );
	}

	/**
	 * Open a {@link Session} and use it to obtain a value.
	 *
	 * @apiNote This method does not begin a transaction, and so
	 * the session is not automatically flushed before the method
	 * returns unless either:
	 * <ul>
	 * <li>the given action calls {@link Session#flush() flush()}
	 *     explicitly, or
	 * <li>a transaction is initiated by the given action, using
	 *     {@link Session#inTransaction}, for example.
	 * </ul>
	 *
	 * @see #fromTransaction(Function)
	 */
	default <R> R fromSession(Function<? super Session,R> action) {
		try ( Session session = openSession() ) {
			return action.apply( session );
		}
	}

	/**
	 * Open a {@link StatelessSession} and use it to obtain a value.
	 *
	 * @apiNote This method does not begin a transaction, and so
	 * the session is not automatically flushed before the method
	 * returns unless either:
	 * <ul>
	 * <li>the given action calls {@link Session#flush() flush()}
	 *     explicitly, or
	 * <li>a transaction is initiated by the given action, using
	 *     {@link Session#inTransaction}, for example.
	 * </ul>
	 *
	 * @see #fromStatelessTransaction(Function)
	 *
	 * @since 6.3
	 */
	default <R> R fromStatelessSession(Function<? super StatelessSession,R> action) {
		try ( StatelessSession session = openStatelessSession() ) {
			return action.apply( session );
		}
	}

	/**
	 * Open a {@link Session} and use it to obtain a value
	 * within the bounds of a transaction.
	 *
	 * @apiNote This method competes with the JPA-defined method
	 *          {@link #callInTransaction}
	 */
	default <R> R fromTransaction(Function<? super Session,R> action) {
		return fromSession( session -> manageTransaction( session, session.beginTransaction(), action ) );
	}

	/**
	 * Open a {@link StatelessSession} and use it to obtain a value
	 * within the bounds of a transaction.
	 *
	 * @since 6.3
	 */
	default <R> R fromStatelessTransaction(Function<? super StatelessSession,R> action) {
		return fromStatelessSession( session -> manageTransaction( session, session.beginTransaction(), action ) );
	}

	/**
	 * Create a new {@link Session}.
	 */
	@Override
	Session createEntityManager();

	/**
	 * Create a new {@link Session}, with the given
	 * {@linkplain EntityManager#getProperties properties}.
	 */
	@Override
	Session createEntityManager(Map<?, ?> map);

	/**
	 * Create a new {@link Session}, with the given
	 * {@linkplain SynchronizationType synchronization type}.
	 *
	 * @throws IllegalStateException if the persistence unit has
	 * {@linkplain jakarta.persistence.PersistenceUnitTransactionType#RESOURCE_LOCAL
	 * resource-local} transaction management
	 */
	@Override
	Session createEntityManager(SynchronizationType synchronizationType);

	/**
	 * Create a new {@link Session}, with the given
	 * {@linkplain SynchronizationType synchronization type} and
	 * {@linkplain EntityManager#getProperties properties}.
	 *
	 * @throws IllegalStateException if the persistence unit has
	 * {@linkplain jakarta.persistence.PersistenceUnitTransactionType#RESOURCE_LOCAL
	 * resource-local} transaction management
	 */
	@Override
	Session createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map);

	/**
	 * Retrieve the {@linkplain Statistics statistics} for this factory.
	 *
	 * @return The statistics.
	 */
	Statistics getStatistics();

	/**
	 * A {@link SchemaManager} with the same default catalog and schema as
	 * pooled connections belonging to this factory. Intended mostly as a
	 * convenience for writing tests.
	 *
	 * @since 6.2
	 */
	@Override
	SchemaManager getSchemaManager();

	/**
	 * Obtain a {@link HibernateCriteriaBuilder} which may be used to
	 * {@linkplain HibernateCriteriaBuilder#createQuery(Class) construct}
	 * {@linkplain org.hibernate.query.criteria.JpaCriteriaQuery criteria
	 * queries}.
	 *
	 * @see SharedSessionContract#getCriteriaBuilder()
	 */
	@Override
	HibernateCriteriaBuilder getCriteriaBuilder();

	/**
	 * Destroy this {@code SessionFactory} and release all its resources,
	 * including caches and connection pools.
	 * <p>
	 * It is the responsibility of the application to ensure that there are
	 * no open {@linkplain Session sessions} before calling this method as
	 * the impact on those {@linkplain Session sessions} is indeterminate.
	 * <p>
	 * No-ops if already {@linkplain #isClosed() closed}.
	 *
	 * @throws HibernateException Indicates an issue closing the factory.
	 */
	@Override
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
	 *
	 * Create an {@link EntityGraph} for the given entity type.
	 *
	 * @param entityType The entity type for the graph
	 *
	 * @see #createGraphForDynamicEntity(String)
	 *
	 * @since 7.0
	 */
	default <T> RootGraph<T> createEntityGraph(Class<T> entityType) {
		return new RootGraphImpl<>( null, (EntityDomainType<T>) getMetamodel().entity( entityType ) );
	}

	/**
	 * Create an {@link EntityGraph} which may be used from loading a
	 * {@linkplain org.hibernate.metamodel.RepresentationMode#MAP dynamic}
	 * entity with {@link Session#find(EntityGraph, Object, FindOption...)}.
	 * <p>
	 * This allows a dynamic entity to be loaded without the need for a cast.
	 * <pre>
	 * var MyDynamicEntity_ = factory.createGraphForDynamicEntity("MyDynamicEntity");
	 * Map&lt;String,?&gt; myDynamicEntity = session.find(MyDynamicEntity_, id);
	 * </pre>
	 *
	 * @apiNote Dynamic entities are normally defined using XML mappings.
	 *
	 * @param entityName The name of the dynamic entity
	 *
	 * @since 7.0
	 *
	 * @see Session#find(EntityGraph, Object, FindOption...)
	 * @see #createEntityGraph(Class)
	 */
	RootGraph<Map<String,?>> createGraphForDynamicEntity(String entityName);

	/**
	 * Creates a RootGraph for the given {@code rootEntityClass} and parses the graph text into
	 * it.
	 *
	 * @param rootEntityClass The entity class to use as the base of the created root-graph
	 * @param graphText The textual representation of the graph
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @see GraphParser#parse(Class, CharSequence, SessionFactory)
	 * @see #createEntityGraph(Class)
	 *
	 * @apiNote The string representation is expected to just be an attribute list.  E.g.
	 * {@code "title, isbn, author(name, books)"}
	 *
	 * @since 7.0
	 */
	default <T> RootGraph<T> parseEntityGraph(Class<T> rootEntityClass, CharSequence graphText) {
		return GraphParser.parse( rootEntityClass, graphText.toString(), unwrap( SessionFactoryImplementor.class ) );
	}

	/**
	 * Creates a RootGraph for the given {@code rootEntityName} and parses the graph text into
	 * it.
	 *
	 * @param rootEntityName The name of the entity to use as the base of the created root-graph
	 * @param graphText The textual representation of the graph
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @see GraphParser#parse(String, CharSequence, SessionFactory)
	 * @see #createEntityGraph(Class)
	 *
	 * @apiNote The string representation is expected to just be an attribute list.  E.g.
	 * {@code "title, isbn, author(name, books)"}
	 *
	 * @since 7.0
	 */
	default <T> RootGraph<T> parseEntityGraph(String rootEntityName, CharSequence graphText) {
		return GraphParser.parse( rootEntityName, graphText.toString(), unwrap( SessionFactoryImplementor.class ) );
	}

	/**
	 * Creates a RootGraph based on the passed string representation.  Here, the
	 * string representation is expected to include the root entity name.
	 *
	 * @param graphText The textual representation of the graph
	 *
	 * @throws InvalidGraphException if the textual representation is invalid.
	 *
	 * @apiNote The string representation is expected to an attribute list prefixed
	 * with the name of the root entity.  E.g. {@code "Book: title, isbn, author(name, books)"}
	 *
	 * @since 7.0
	 */
	default <T> RootGraph<T> parseEntityGraph(CharSequence graphText) {
		return GraphParser.parse( graphText.toString(), unwrap( SessionFactoryImplementor.class ) );
	}

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
	 *
	 * @since 6.2
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

	/**
	 * Add or override the definition of a named query, returning
	 * a {@linkplain TypedQueryReference reference} to the query.
	 * Settings such as first and max results, hints, flush mode,
	 * cache mode, timeout, and lock options are preserved as part
	 * of the named query definition. Any arguments bound to query
	 * parameters are discarded.
	 *
	 * @param name the name to be assigned to the query
	 * @param query the query, including first and max results, hints,
	 *              flush mode, cache mode, timeout, and lock options
	 *
	 * @see #addNamedQuery(String, jakarta.persistence.Query)
	 * @see org.hibernate.query.QueryProducer#createQuery(TypedQueryReference)
	 * @see org.hibernate.query.QueryProducer#createSelectionQuery(String, Class)
	 *
	 * @since 7.0
	 */
	@Incubating
	<R> TypedQueryReference<R> addNamedQuery(String name, TypedQuery<R> query);

	/**
	 * The name assigned to this {@code SessionFactory}, if any.
	 * <ul>
	 * <li>When bootstrapping via JPA, this is the persistence unit name.
	 * <li>Otherwise, the name may be specified by the configuration property
	 *     {@value org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME}.
	 * </ul>
	 * <p>
	 * If {@value org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME_IS_JNDI}
	 * is enabled, then this name is used to bind this object to JNDI, unless
	 * {@value org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_JNDI_NAME}
	 * is also specified.
	 *
	 * @see org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME
	 * @see org.hibernate.cfg.PersistenceSettings#SESSION_FACTORY_NAME_IS_JNDI
	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getPersistenceUnitName
	 */
	@Override
	String getName();

	/**
	 * Get the {@linkplain SessionFactoryOptions options} used to build this factory.
	 *
	 * @return The special options used to build the factory.
	 *
	 * @deprecated There is no plan to remove this operation, but its use should be
	 *             avoided since {@link SessionFactoryOptions} is an SPI type, and so
	 *             this operation is a layer-breaker.
	 */
	@Deprecated(since = "6.2")
	SessionFactoryOptions getSessionFactoryOptions();
}
