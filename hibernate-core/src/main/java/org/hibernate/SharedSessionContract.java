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
import jakarta.persistence.EntityHandler;
import jakarta.persistence.FindOption;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.SemanticException;
import org.hibernate.query.UnknownNamedQueryException;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;

import static org.hibernate.internal.TransactionManagement.manageTransaction;

/**
 * Declares operations that are common between {@link Session} and {@link StatelessSession}.
 *
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends EntityHandler, AutoCloseable, Serializable {

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

	/// Corollary to [#find(Class,Object)] for dynamic models.
	///
	/// @param entityName The entity name
	/// @param id The identifier
	/// @return The persistent instance, or null
	/// @throws IllegalArgumentException If the given name does not match
	/// a mapped dynamic entity
	/// @see SessionFactory#createGraphForDynamicEntity(String)
	/// @see #find(EntityGraph, Object, FindOption...)
	default Object find(String entityName, Object id) {
		return find( entityName, id, LockMode.NONE );
	}

	/// Corollary to [#find(Class,Object,FindOption...)] for dynamic models.
	///
	/// @param entityName The entity name
	/// @param key The key (primary or natural, based on [KeyType])
	/// @param findOptions Options for the load operation.
	///
	/// @return The persistent instance, or null
	///
	/// @throws IllegalArgumentException If the given name does not match
	/// a mapped dynamic entity
	///
	/// @see SessionFactory#createGraphForDynamicEntity(String)
	/// @see #find(EntityGraph,Object,FindOption...)
	Object find(String entityName, Object key, FindOption... findOptions);

	/// Form of [#find(String,Object)] throwing [jakarta.persistence.EntityNotFoundException]
	/// if no entity exists for that id rather than returning null.
	///
	/// @param entityName The entity name
	/// @param id The identifier
	/// @return The persistent instance
	/// @throws IllegalArgumentException If the given name does not match
	/// a mapped dynamic entity
	/// @throws jakarta.persistence.EntityNotFoundException if no entity
	/// was found for the given `id`
	/// @see SessionFactory#createGraphForDynamicEntity(String)
	/// @see #get(EntityGraph, Object, FindOption...)
	default Object get(String entityName, Object id) {
		return get( entityName, id, LockMode.NONE );
	}

	/// Form of [#find(String,Object,FindOption...)] throwing [jakarta.persistence.EntityNotFoundException]
	/// if no entity exists for that id rather than returning null.
	///
	/// @param entityName The entity name
	/// @param key The key (primary or natural, based on [KeyType])
	/// @param findOptions Options for the load operation.
	///
	/// @return a persistent instance
	///
	/// @throws IllegalArgumentException If the given name does not match
	/// a mapped dynamic entity
	/// @throws jakarta.persistence.EntityNotFoundException if no entity
	/// was found for the given `key`
	///
	/// @see SessionFactory#createGraphForDynamicEntity(String)
	/// @see #get(EntityGraph,Object,FindOption...)
	Object get(String entityName, Object key, FindOption... findOptions);

	/**
	 * Create a typed {@link Query} instance for the given HQL query
	 * string and given query result type.
	 * <ul>
	 * <li>If the query has a single item in the {@code select} list,
	 *     then the select item must be assignable to the given result
	 *     type.
	 * <li>Otherwise, if there are multiple select items, then the
	 *     select items will be packaged into an instance of the
	 *     result type. The result type must have an appropriate
	 *     constructor with parameter types matching the select items,
	 *     or it must be one of the types {@code Object[]},
	 *     {@link java.util.List}, {@link java.util.Map}, or
	 *     {@link jakarta.persistence.Tuple}.
	 * </ul>
	 * <p>
	 * If a query has no explicit {@code select} list, the select list
	 * is inferred from the given query result type:
	 * <ul>
	 * <li>if the result type is an entity type, the query must have
	 *     exactly one root entity in the {@code from} clause, it must
	 *     be assignable to the result type, and the inferred select
	 *     list will contain just that entity, or
	 * <li>otherwise, the select list contains every root entity and
	 *     every non-{@code fetch} joined entity, and each query result
	 *     will be packaged into an instance of the result type, just
	 *     as specified above.
	 * </ul>
	 * <p>
	 * If a query has no explicit {@code from} clause, and the given
	 * result type is an entity type, the root entity is inferred to
	 * be the result type.
	 * <p>
	 * Passing {@code Object.class} as the query result type is not
	 * recommended.
	 * <p>
	 * The returned {@code Query} may be executed by calling
	 * {@link Query#getResultList()} or {@link Query#getSingleResult()}.
	 *
	 * @param queryString The HQL query
	 * @param resultClass The {@link Class} object representing the
	 *                    query result type, which should not be
	 *                    {@code Object.class}
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String,Class)
	 */
	@Override
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

	/**
	 * @see jakarta.persistence.EntityHandler#createQuery(String,EntityGraph)
	 */
	@Override
	<T> Query<T> createQuery(String s, EntityGraph<T> entityGraph);

	/**
	 * Create a {@link SelectionQuery} instance for the given HQL query
	 * string and given query result type.
	 * <ul>
	 * <li>If the query has a single item in the {@code select} list,
	 *     then the select item must be assignable to the given result
	 *     type.
	 * <li>Otherwise, if there are multiple select items, then the
	 *     select items will be packaged into an instance of the
	 *     result type. The result type must have an appropriate
	 *     constructor with parameter types matching the select items,
	 *     or it must be one of the types {@code Object[]},
	 *     {@link java.util.List}, {@link java.util.Map}, or
	 *     {@link jakarta.persistence.Tuple}.
	 * </ul>
	 * <p>
	 * If a query has no explicit {@code select} list, the select list
	 * is inferred from the given query result type:
	 * <ul>
	 * <li>if the result type is an entity type, the query must have
	 *     exactly one root entity in the {@code from} clause, it must
	 *     be assignable to the result type, and the inferred select
	 *     list will contain just that entity, or
	 * <li>otherwise, the select list contains every root entity and
	 *     every non-{@code fetch} joined entity, and each query result
	 *     will be packaged into an instance of the result type, just
	 *     as specified above.
	 * </ul>
	 * <p>
	 * If a query has no explicit {@code from} clause, and the given
	 * result type is an entity type, the root entity is inferred to
	 * be the result type.
	 * <p>
	 * Passing {@code Object.class} as the query result type is not
	 * recommended.
	 * <p>
	 * The returned {@code Query} may be executed by calling
	 * {@link Query#getResultList()} or {@link Query#getSingleResult()}.

	 * @param hqlString The HQL {@code select} query as a string
	 * @param resultType The {@link Class} object representing the
	 *                   query result type, which should not be
	 *                   {@code Object.class}
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 *
	 * @throws IllegalSelectQueryException if the given HQL query
	 *         is an {@code insert}, {@code update} or {@code delete}
	 *         statement
	 */
	<R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType);

	/**
	 * Create a {@link SelectionQuery} instance for the given HQL query
	 * string and given {@link EntityGraph}, which is interpreted as a
	 * {@linkplain org.hibernate.graph.GraphSemantic#LOAD load graph}.
	 * The query result type is the root entity of the given graph.
	 * <ul>
	 * <li>If the query has an explicit {@code select} clause, there must
	 *     be a single item in the {@code select} list, and the select
	 *     item must be assignable to the root type of the given graph.
	 * <li>Otherwise, if a query has no explicit {@code select} list, the
	 *     select list is inferred from the given entity graph. The query
	 *     must have exactly one root entity in the {@code from} clause,
	 *     it must be assignable to the root type of the given graph, and
	 *     the inferred select list will contain just that entity.
	 * </ul>
	 * <p>
	 * If a query has no explicit {@code from} clause, and the given
	 * result type is an entity type, the root entity is inferred to
	 * be the result type.
	 * <p>
	 * The returned {@code Query} may be executed by calling
	 * {@link Query#getResultList()} or {@link Query#getSingleResult()}.

	 * @param hqlString The HQL {@code select} query as a string
	 * @param resultGraph An {@link EntityGraph} whose root type is the
	 *                    query result type, which is interpreted as a
	 *                    {@linkplain org.hibernate.graph.GraphSemantic#LOAD
	 *                    load graph}
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 *
	 * @throws IllegalSelectQueryException if the given HQL query
	 *         is an {@code insert}, {@code update} or {@code delete}
	 *         statement
	 *
	 * @since 7.0
	 */
	<R> SelectionQuery<R> createSelectionQuery(String hqlString, EntityGraph<R> resultGraph);

	/**
	 * Create a {@link MutationQuery} reference for the given HQL insert,
	 * update, or delete statement.
	 *
	 * @param hqlString The HQL {@code insert}, {@code update}, or
	 *                  {@code delete} statement
	 *
	 * @throws IllegalMutationQueryException if the given HQL query
	 *         is a {@code select} query
	 */
	MutationQuery createMutationQuery(String hqlString);

	/**
	 * Create a typed {@link Query} instance for the given typed query reference.
	 *
	 * @param typedQueryReference the type query reference
	 *
	 * @return The {@link Query} instance for execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the name of the typed query reference or if
	 * the query result is found to not be assignable to
	 * result class of the typed query reference
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(TypedQueryReference)
	 */
	@Override
	<R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference);

	/**
	 * Create a {@link Query} for the given JPA {@link CriteriaQuery}.
	 */
	@Override
	<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery);

	/**
	 * @see jakarta.persistence.EntityHandler#createQuery(CriteriaSelect)
	 */
	@Override
	<T> Query<T> createQuery(CriteriaSelect<T> criteriaSelect);

	/**
	 * @see jakarta.persistence.EntityHandler#createQuery(CriteriaUpdate)
	 */
	@Override
	Query<?> createQuery(CriteriaUpdate<?> criteriaUpdate);

	/**
	 * @see jakarta.persistence.EntityHandler#createQuery(CriteriaDelete)
	 */
	@Override
	Query<?> createQuery(CriteriaDelete<?> criteriaDelete);

	/**
	 * Create a {@link SelectionQuery} reference for the given
	 * {@link CriteriaQuery}.
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaSelect)
	 */
	<R> SelectionQuery<R> createSelectionQuery(CriteriaSelect<R> criteria);

	/**
	 * Create a {@link SelectionQuery} reference for the given
	 * {@link CriteriaQuery}.
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaQuery)
	 */
	<R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria);

	/**
	 * Create a {@link MutationQuery} from the given update criteria tree
	 */
	MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery);

	/**
	 * Create a {@link MutationQuery} from the given delete criteria tree
	 */
	MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery);

	/**
	 * Create a {@link MutationQuery} from the given insert criteria tree
	 */
	MutationQuery createMutationQuery(@SuppressWarnings("rawtypes") JpaCriteriaInsert insert);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL query.
	 *
	 * @param sqlString The native (SQL) query string
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityHandler#createNativeQuery(String)
	 */
	@Override
	NativeQuery<?> createNativeQuery(String sqlString);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL query
	 * using an implicit mapping to the specified Java type.
	 * <ul>
	 * <li>If the given class is an entity class, this method is equivalent
	 *     to {@code createNativeQuery(sqlString).addEntity(resultClass)}.
	 * <li>If the given class has a registered
	 *     {@link org.hibernate.type.descriptor.java.JavaType}, then the
	 *     query must return a result set with a single column whose
	 *     {@code JdbcType} is compatible with that {@code JavaType}.
	 * <li>Otherwise, the select items will be packaged into an instance of
	 *     the result type. The result type must have an appropriate
	 *     constructor with parameter types matching the select items, or it
	 *     must be one of the types {@code Object[]}, {@link java.util.List},
	 *     {@link java.util.Map}, or {@link jakarta.persistence.Tuple}.
	 * </ul>
	 *
	 * @param sqlString The native (SQL) query string
	 * @param resultClass The Java type to map results to
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityHandler#createNativeQuery(String,Class)
	 */
	@Override
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL query
	 * using an implicit mapping to the specified Java entity type.
	 * <p>
	 * The given class must be an entity class. This method is equivalent to
	 * {@code createNativeQuery(sqlString).addEntity(tableAlias, resultClass)}.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultClass The Java entity class to map results to
	 * @param tableAlias The table alias for columns in the result set
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL query
	 * using an explicit mapping to the specified Java type.
	 * <p>
	 * The given result set mapping name must identify a mapping defined by
	 * a {@link jakarta.persistence.SqlResultSetMapping} annotation.
	 *
	 * @param sqlString The native (SQL) query string
	 * @param resultSetMappingName The explicit result mapping name
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 * @see jakarta.persistence.SqlResultSetMapping
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL statement.
	 *
	 * @param sqlString a native SQL statement string
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 */
	MutationQuery createNativeMutationQuery(String sqlString);

	/**
	 * Create a typed {@link Query} instance for the given named query.
	 * The named query might be defined in HQL or in native SQL.
	 *
	 * @param name the name of a query defined in metadata
	 * @param resultClass the type of the query result
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid or if the query result is found to
	 * not be assignable to the specified type
	 *
	 * @see jakarta.persistence.EntityManager#createNamedQuery(String,Class)
	 */
	@Override
	<R> Query<R> createNamedQuery(String name, Class<R> resultClass);

	@Override
	Query<?> createNamedQuery(String s);

	<R> NativeQuery<R> createNamedQuery(String name, String resultSetMappingName);
	<R> NativeQuery<R> createNamedQuery(String name, String resultSetMappingName, Class<R> resultClass);

	@Override
	NativeQuery<?> createNativeQuery(String sql, String resultSetMapping);

	@Override
	<T> TypedQuery<T> createNativeQuery(String sql, ResultSetMapping<T> resultSetMapping);

	/**
	 * Create a {@link SelectionQuery} instance for the named
	 * {@link jakarta.persistence.NamedQuery} with the given result type.
	 *
	 * @throws IllegalSelectQueryException if the given HQL query is not a select query
	 * @throws UnknownNamedQueryException if no query has been defined with the given name
	 */
	<R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType);

	/**
	 * Create a {@link MutationQuery} instance for the given named insert,
	 * update, or delete HQL query. The named query might be defined as
	 * {@linkplain  jakarta.persistence.NamedQuery HQL}) or
	 * {@linkplain  jakarta.persistence.NamedNativeQuery native-SQL}.
	 *
	 * @throws IllegalMutationQueryException if the given HQL query is a select query
	 * @throws UnknownNamedQueryException if no query has been defined with the given name
	 */
	MutationQuery createNamedMutationQuery(String name);

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
	 * @see jakarta.persistence.EntityManager#createEntityGraph(String)
	 *
	 * @since 6.3
	 */
	<T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName);

	/**
	 * Obtain an immutable reference to a predefined
	 * {@linkplain jakarta.persistence.NamedEntityGraph named entity graph}.
	 *
	 * @param graphName the name of the predefined named entity graph
	 * @throws IllegalArgumentException if there is no predefined graph
	 *         with the given name
	 *
	 * @apiNote This method returns {@code RootGraph<?>}, requiring an
	 * unchecked typecast before use. It's cleaner to obtain a graph using
	 * the static metamodel for the class which defines the graph, or by
	 * calling {@link SessionFactory#getNamedEntityGraphs(Class)} instead.
	 *
	 * @see jakarta.persistence.EntityManager#getEntityGraph(String)
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
	 * Obtain a {@link StatelessSession} builder with the ability to copy certain
	 * information from this session.
	 *
	 * @return the session builder
	 *
	 * @since 7.2
	 */
	@Incubating
	SharedStatelessSessionBuilder statelessWithOptions();

	/**
	 * Obtain a {@link Session} builder with the ability to copy certain
	 * information from this session.
	 *
	 * @return the session builder
	 */
	SharedSessionBuilder sessionWithOptions();

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations


	/**
	 * Create a {@link Query} instance for the given HQL query, or
	 * HQL insert, update, or delete statement.
	 * <p>
	 * If a query has no explicit {@code select} list, the select list
	 * is inferred:
	 * <ul>
	 * <li>if there is exactly one root entity in the {@code from}
	 *     clause, and it has no non-{@code fetch} joins, then that
	 *     root entity is the only element of the select list, or
	 * <li>if there is an entity with the alias {@code this}, then
	 *     that entity is the only element of the select list, or
	 * <li>otherwise, the query is considered ambiguous, and this
	 *     method throws a {@link SemanticException}.
	 * </ul>
	 * <p>
	 * The query must have an explicit {@code from} clause, which
	 * can never be inferred.
	 *
	 * @deprecated The overloaded form
	 * {@link #createQuery(String, Class)} which takes a result type
	 * is strongly recommended in preference to this method, since it
	 * returns a typed {@code Query} object, and because it is able to
	 * use the given result type to infer the {@code select} list, and
	 * even sometimes the {@code from} clause. Alternatively,
	 * {@link #createSelectionQuery(String, Class)} is preferred for
	 * queries, and {@link #createMutationQuery(String)} for insert,
	 * update, and delete statements.
	 *
	 * @apiNote Returns a raw {@code Query} type instead of a wildcard
	 * type {@code Query<?>}, to match the signature of the JPA method
	 * {@link jakarta.persistence.EntityManager#createQuery(String)}.
	 *
	 * @param queryString The HQL query
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 */
	@Override
	@Deprecated
	@SuppressWarnings("rawtypes")
	Query createQuery(String queryString);
}
