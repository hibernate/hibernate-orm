/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PersistenceException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import static org.hibernate.internal.TransactionManagement.manageTransaction;

/**
 * Declares operations that are common between {@link Session} and {@link StatelessSession}.
 *
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends QueryProducer, AutoCloseable, Serializable {
	/**
	 * Obtain the tenant identifier associated with this session, as a string.
	 *
	 * @return The tenant identifier associated with this session, or {@code null}
	 *
	 * @see org.hibernate.context.spi.CurrentTenantIdentifierResolver
	 * @see SessionBuilder#tenantIdentifier(Object)
	 */
	String getTenantIdentifier();

	/**
	 * Obtain the tenant identifier associated with this session.
	 *
	 * @return The tenant identifier associated with this session, or {@code null}
	 * @since 6.4
	 *
	 * @see org.hibernate.context.spi.CurrentTenantIdentifierResolver
	 * @see SessionBuilder#tenantIdentifier(Object)
	 */
	Object getTenantIdentifierValue();

	/**
	 * Get the current {@linkplain CacheMode cache mode} for this session.
	 *
	 * @return the current cache mode
	 */
	CacheMode getCacheMode();

	/**
	 * Set the current {@linkplain CacheMode cache mode} for this session.
	 * <p>
	 * The cache mode determines the manner in which this session can interact with
	 * the second level cache.
	 *
	 * @param cacheMode the new cache mode
	 */
	void setCacheMode(CacheMode cacheMode);

	/**
	 * End the session by releasing the JDBC connection and cleaning up.
	 *
	 * @throws HibernateException Indicates problems cleaning up.
	 */
	@Override
	void close() throws HibernateException;

	/**
	 * Check if the session is still open.
	 *
	 * @return {@code true} if it is open
	 */
	boolean isOpen();

	/**
	 * Check if the session is currently connected.
	 *
	 * @return {@code true} if it is connected
	 */
	boolean isConnected();

	/**
	 * Begin a unit of work and return the associated {@link Transaction} object.
	 * If a new underlying transaction is required, begin the transaction. Otherwise,
	 * continue the new work in the context of the existing underlying transaction.
	 *
	 * @apiNote
	 * The JPA-standard way to begin a new resource-local transaction is by calling
	 * {@link #getTransaction getTransaction().begin()}. But it's not always safe to
	 * execute this idiom.
	 * <ul>
	 * <li>JPA doesn't allow an {@link jakarta.persistence.EntityTransaction
	 * EntityTransaction} to represent a JTA transaction context. Therefore, when
	 * {@linkplain org.hibernate.jpa.spi.JpaCompliance#isJpaTransactionComplianceEnabled
	 * strict JPA transaction compliance} is enabled via, for example, setting
	 * {@value org.hibernate.cfg.JpaComplianceSettings#JPA_TRANSACTION_COMPLIANCE},
	 * the call to {@code getTransaction()} fails if transactions are managed by JTA.
	 * <p>
	 * On the other hand, this method does not fail when JTA transaction management
	 * is used, not even if strict JPA transaction compliance is enabled.
	 * <li>Even when resource-local transactions are in use, and even when strict JPA
	 * transaction compliance is <em>disabled</em>, the call to {@code begin()}
	 * fails if a transaction is already {@linkplain Transaction#isActive active}.
	 * <p>
	 * This method never fails when a transaction is already active. Instead,
	 * {@code beginTransaction()} simply returns the {@link Transaction} object
	 * representing the active transaction.
	 * </ul>
	 *
	 * @return an instance of {@link Transaction} representing the new transaction
	 *
	 * @see #getTransaction()
	 * @see Transaction#begin()
	 */
	Transaction beginTransaction();

	/**
	 * Get the {@link Transaction} instance associated with this session.
	 *
	 * @apiNote
	 * This method is the JPA-standard way to obtain an instance of
	 * {@link jakarta.persistence.EntityTransaction EntityTransaction}
	 * representing a resource-local transaction. But JPA doesn't allow an
	 * {@code EntityTransaction} to represent a JTA transaction. Therefore, when
	 * {@linkplain org.hibernate.jpa.spi.JpaCompliance#isJpaTransactionComplianceEnabled
	 * strict JPA transaction compliance} is enabled via, for example, setting
	 * {@value org.hibernate.cfg.JpaComplianceSettings#JPA_TRANSACTION_COMPLIANCE},
	 * this method fails if transactions are managed by JTA.
	 * <p>
	 * On the other hand, when JTA transaction management is used, and when
	 * strict JPA transaction compliance is <em>disabled</em>, this method happily
	 * returns a {@link Transaction} representing the current JTA transaction context.
	 *
	 * @return an instance of {@link Transaction} representing the transaction
	 *         associated with this session
	 *
	 * @see jakarta.persistence.EntityManager#getTransaction()
	 */
	Transaction getTransaction();

	/**
	 * Join the currently-active JTA transaction.
	 *
	 * @see jakarta.persistence.EntityManager#joinTransaction()
	 *
	 * @since 6.2
	 */
	void joinTransaction();

	/**
	 * Check if the session is joined to the current transaction.
	 *
	 * @see #joinTransaction()
	 * @see jakarta.persistence.EntityManager#isJoinedToTransaction()
	 *
	 * @since 6.2
	 */
	boolean isJoinedToTransaction();

	/**
	 * Obtain a {@link ProcedureCall} based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see jakarta.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall getNamedProcedureCall(String name);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings. Each given class is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings);

	/**
	 * Obtain a {@link ProcedureCall} based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see jakarta.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall createNamedStoredProcedureQuery(String name);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result
	 * set entity mappings. Each given class is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses);

	/**
	 * Create a {@link ProcedureCall} to a stored procedure with the given result set
	 * entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings);

	/**
	 * Get the session-level JDBC batch size for the current session.
	 *
	 * @return the current session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	Integer getJdbcBatchSize();

	/**
	 * Set the session-level JDBC batch size. Override the
	 * {@linkplain org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * factory-level} JDBC batch size controlled by the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE}.
	 *
	 * @apiNote Setting a session-level JDBC batch size for a
	 * {@link StatelessSession} triggers a sort of write-behind behaviour
	 * where operations are batched and executed asynchronously, undermining
	 * the semantics of the stateless programming model. We recommend the use
	 * of explicitly-batching operations like
	 * {@link StatelessSession#insertMultiple insertMultiple()},
	 * {@link StatelessSession#updateMultiple updateMultiple()}, and
	 * {@link StatelessSession#deleteMultiple deleteMultiple()} instead.
	 *
	 * @param jdbcBatchSize the new session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.cfg.AvailableSettings#STATEMENT_BATCH_SIZE
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	void setJdbcBatchSize(Integer jdbcBatchSize);

	/**
	 * Obtain a {@link HibernateCriteriaBuilder} which may be used to
	 * {@linkplain HibernateCriteriaBuilder#createQuery(Class) construct}
	 * {@linkplain org.hibernate.query.criteria.JpaCriteriaQuery criteria
	 * queries}.
	 *
	 * @return an instance of {@link HibernateCriteriaBuilder}
	 *
	 * @throws IllegalStateException if the session has been closed
	 *
	 * @see SessionFactory#getCriteriaBuilder()
	 */
	HibernateCriteriaBuilder getCriteriaBuilder();

	/**
	 * Perform work using the {@link java.sql.Connection} underlying by this session.
	 *
	 * @param work The work to be performed.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 *
	 * @apiNote This method competes with the JPA-defined method
	 *          {@link jakarta.persistence.EntityManager#runWithConnection}
	 */
	void doWork(Work work) throws HibernateException;

	/**
	 * Perform work using the {@link java.sql.Connection} underlying by this session,
	 * and return a result.
	 *
	 * @param work The work to be performed.
	 * @param <T> The type of the result returned from the work
	 *
	 * @return the result of calling {@link ReturningWork#execute}.
	 *
	 * @throws HibernateException Generally indicates wrapped {@link java.sql.SQLException}
	 *
	 * @apiNote This method competes with the JPA-defined method
	 *          {@link jakarta.persistence.EntityManager#callWithConnection}
	 */
	<T> T doReturningWork(ReturningWork<T> work);

	/**
	 * Create a new mutable instance of {@link EntityGraph}, with only
	 * a root node, allowing programmatic definition of the graph from
	 * scratch.
	 *
	 * @param rootType the root entity class of the graph
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.graph.EntityGraphs#createGraph(jakarta.persistence.metamodel.EntityType)
	 */
	<T> RootGraph<T> createEntityGraph(Class<T> rootType);

	/**
	 * Create a new mutable instance of {@link EntityGraph}, based on
	 * a predefined {@linkplain jakarta.persistence.NamedEntityGraph
	 * named entity graph}, allowing customization of the graph, or
	 * return {@code null} if there is no predefined graph with the
	 * given name.
	 *
	 * @param graphName the name of the graph
	 *
	 * @apiNote This method returns {@code RootGraph<?>}, requiring an
	 * unchecked typecast before use. It's cleaner to obtain a graph using
	 * {@link #createEntityGraph(Class, String)} instead.
	 *
	 * @see SessionFactory#getNamedEntityGraphs(Class)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 *
	 * @since 6.3
	 */
	RootGraph<?> createEntityGraph(String graphName);

	/**
	 * Create a new mutable instance of {@link EntityGraph}, based on
	 * a predefined {@linkplain jakarta.persistence.NamedEntityGraph
	 * named entity graph}, allowing customization of the graph, or
	 * return {@code null} if there is no predefined graph with the
	 * given name.
	 *
	 * @param rootType the root entity class of the graph
	 * @param graphName the name of the predefined named entity graph
	 *
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 *
	 * @throws IllegalArgumentException if the graph with the given
	 *         name does not have the given entity type as its root
	 *
	 * @since 6.3
	 */
	<T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName);

	/**
	 * Obtain an immutable reference to a predefined
	 * {@linkplain jakarta.persistence.NamedEntityGraph named entity graph}
	 * or return {@code null} if there is no predefined graph with the given
	 * name.
	 *
	 * @param graphName the name of the predefined named entity graph
	 *
	 * @apiNote This method returns {@code RootGraph<?>}, requiring an
	 * unchecked typecast before use. It's cleaner to obtain a graph using
	 * the static metamodel for the class which defines the graph, or by
	 * calling {@link SessionFactory#getNamedEntityGraphs(Class)} instead.
	 *
	 * @see SessionFactory#getNamedEntityGraphs(Class)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 *
	 * @since 6.3
	 */
	RootGraph<?> getEntityGraph(String graphName);

	/**
	 * Retrieve all named {@link EntityGraph}s with the given root entity type.
	 *
	 * @see jakarta.persistence.EntityManagerFactory#getNamedEntityGraphs(Class)
	 * @see jakarta.persistence.EntityManagerFactory#addNamedEntityGraph(String, EntityGraph)
	 *
	 * @since 6.3
	 */
	<T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass);

	/**
	 * Enable the named {@linkplain Filter filter} for this current session.
	 * <p>
	 * The returned {@link Filter} object must be used to bind arguments
	 * to parameters of the filter, and every parameter must be set before
	 * any other operation of this session is called.
	 *
	 * @param filterName the name of the filter to be enabled.
	 *
	 * @return the {@link Filter} instance representing the enabled filter.
	 *
	 * @throws UnknownFilterException if there is no such filter
	 *
	 * @see org.hibernate.annotations.FilterDef
	 */
	Filter enableFilter(String filterName);

	/**
	 * Retrieve a currently enabled {@linkplain Filter filter} by name.
	 *
	 * @param filterName the name of the filter to be retrieved.
	 *
	 * @return the {@link Filter} instance representing the enabled filter.
	 */
	Filter getEnabledFilter(String filterName);

	/**
	 * Disable the named {@linkplain Filter filter} for the current session.
	 *
	 * @param filterName the name of the filter to be disabled.
	 */
	void disableFilter(String filterName);

	/**
	 * The factory which created this session.
	 */
	SessionFactory getFactory();

	/**
	 * Perform an action within the bounds of a {@linkplain Transaction
	 * transaction} associated with this session.
	 *
	 * @param action a void function which accepts the {@link Transaction}
	 *
	 * @since 7.0
	 */
	default void inTransaction(Consumer<? super Transaction> action) {
		final Transaction transaction = beginTransaction();
		manageTransaction( transaction, transaction, action );
	}

	/**
	 * Obtain a value within the bounds of a {@linkplain Transaction
	 * transaction} associated with this session.
	 *
	 * @param action a function which accepts the {@link Transaction} and
	 *        returns the value
	 *
	 * @since 7.0
	 */
	default <R> R fromTransaction(Function<? super Transaction,R> action) {
		final Transaction transaction = beginTransaction();
		return manageTransaction( transaction, transaction, action );
	}

	/**
	 * Return an object of the specified type to allow access to
	 * a provider-specific API.
	 *
	 * @param type the class of the object to be returned.
	 * This is usually either the underlying class
	 * implementing {@code SharedSessionContract} or an
	 * interface it implements.
	 * @return an instance of the specified class
	 * @throws PersistenceException if the provider does not
	 * support the given type
	 */
	default <T> T unwrap(Class<T> type) {
		// Not checking type.isInstance(...) because some implementations
		// might want to hide that they implement some types.
		// Implementations wanting a more liberal behavior need to override this method.
		if ( type.isAssignableFrom( SharedSessionContract.class ) ) {
			return type.cast( this );
		}

		throw new PersistenceException(
				"Hibernate cannot unwrap '" + getClass().getName() + "' as '" + type.getName() + "'" );
	}
}
