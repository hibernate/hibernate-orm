/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.hibernate.query.programmatic.MutationSpecification;
import org.hibernate.query.programmatic.SelectionSpecification;

/**
 * Contract for things that can produce instances of {@link Query} and {@link NativeQuery}.
 * Implementors include {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession}.
 * Many operations of the interface have the same or very similar signatures to operations of
 * {@link jakarta.persistence.EntityManager}. They are declared here to allow reuse by
 * {@code StatelessSession}.
 * <p>
 * Unlike the corresponding operations of {@code EntityManager}, operations for creating untyped
 * instances of {@code Query} are all marked as deprecated. Clients must migrate to the use of
 * the equivalent operations which accept a {@link Class} and return a typed {@code Query}.
 *
 * @author Steve Ebersole
 */
public interface QueryProducer {
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
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(String queryString);

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
	 * recommended. In this special case, this method has the same
	 * semantics as the overload {@link #createQuery(String)}.
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
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

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
	<R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference);

	/**
	 * Create a {@link Query} for the given JPA {@link CriteriaQuery}.
	 */
	<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery);

	/**
	 * Create a {@link MutationQuery} for the given JPA {@link CriteriaUpdate}
	 *
	 * @deprecated use {@link #createMutationQuery(CriteriaUpdate)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(CriteriaUpdate updateQuery);

	/**
	 * Create a {@link MutationQuery} for the given JPA {@link CriteriaDelete}
	 *
	 * @deprecated use {@link #createMutationQuery(CriteriaDelete)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(CriteriaDelete deleteQuery);

	/**
	 * Create a {@link NativeQuery} instance for the given native SQL query.
	 *
	 * @param sqlString a native SQL query string
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String)
	 *
	 * @deprecated use {@link #createNativeQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery createNativeQuery(String sqlString);

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
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 *
	 * @apiNote Changes in JPA 3.2 required de-typing this to be compilable with their changes
	 */
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
	 *
	 * @deprecated use {@link #createNativeQuery(String, String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery createNativeQuery(String sqlString, String resultSetMappingName);

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
	 * Create a {@link SelectionQuery} reference for the given HQL
	 * {@code select} statement.
	 * <p>
	 * If the statement has no explicit {@code select} list, the
	 * select list is inferred:
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
	 * {@link #createSelectionQuery(String, Class)} which takes a
	 * result type is strongly recommended in preference to this
	 * method, since it returns a typed {@code SelectionQuery} object,
	 * and because it is able to use the given result type to infer
	 * the {@code select} list, and even sometimes the {@code from}
	 * clause.
	 *
	 * @throws IllegalSelectQueryException if the given HQL query
	 *         is an {@code insert}, {@code update} or {@code delete}
	 *         statement
	 */
	@Deprecated(since = "6.3")
	SelectionQuery<?> createSelectionQuery(String hqlString);

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
	 * recommended. In this special case, this method has the same
	 * semantics as the overload {@link #createSelectionQuery(String)}.
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
	 * Create a {@link SelectionQuery} reference for the given
	 * {@link CriteriaQuery}.
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaQuery)
	 */
	<R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria);

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
	 * @param name the name of a predefined named query
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 *
	 * @see jakarta.persistence.EntityManager#createNamedQuery(String)
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createNamedQuery(String name);

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
	<R> Query<R> createNamedQuery(String name, Class<R> resultClass);

	/**
	 * Create a {@link SelectionQuery} instance for the named
	 * {@link jakarta.persistence.NamedQuery}.
	 *
	 * @implNote This method interprets some queries with an implicit
	 * {@code select} list in a quite unintuitive way. In some future
	 * release, this method will be modified to throw an exception
	 * when passed a query with a missing {@code select}. For now, use
	 * {@link #createNamedSelectionQuery(String, Class)} to avoid
	 * ambiguity.
	 *
	 * @throws IllegalSelectQueryException if the given HQL query is not a select query
	 * @throws UnknownNamedQueryException if no query has been defined with the given name
	 *
	 * @deprecated use {@link #createNamedSelectionQuery(String, Class)}
	 */
	@Deprecated(since = "6.3")
	SelectionQuery<?> createNamedSelectionQuery(String name);

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
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} based on a base HQL statement,
	 * allowing the addition of {@linkplain SelectionSpecification#addOrdering sorting}
	 * and {@linkplain SelectionSpecification#addRestriction restrictions}.
	 *
	 * @param hql The base HQL query.
	 * @param resultType The result type which will ultimately be returned from the {@linkplain SelectionQuery}
	 *
	 * @param <T> The root entity type for the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 *
	 * @throws IllegalSelectQueryException The given HQL is expected to be a {@code select} query.  This method will
	 * throw an exception if not.
	 */
	@Incubating
	<T> SelectionSpecification<T> createSelectionSpecification(String hql, Class<T> resultType)
			throws IllegalSelectQueryException;

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} for the given entity type,
	 * allowing the addition of {@linkplain SelectionSpecification#addOrdering sorting}
	 * and {@linkplain SelectionSpecification#addRestriction restrictions}.
	 * This is effectively the same as calling {@linkplain QueryProducer#createSelectionSpecification(String, Class)}
	 * with {@code "from {rootEntityType}"} as the HQL.
	 *
	 * @param rootEntityType The entity type which is the root of the query.
	 *
	 * @param <T> The entity type which is the root of the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 */
	@Incubating
	<T> SelectionSpecification<T> createSelectionSpecification(Class<T> rootEntityType);

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain MutationQuery} based on a base HQL statement,
	 * allowing the addition of {@linkplain MutationSpecification#addRestriction restrictions}.
	 *
	 * @param hql The base HQL query (expected to be an {@code update} or {@code delete} query).
	 * @param mutationTarget The entity which is the target of the mutation.
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 * {@code mutationTarget} and {@code <T>} are both expected to refer to the mutation target.
	 *
	 * @throws IllegalMutationQueryException Only {@code update} and {@code delete} are supported;
	 * this method will throw an exception if the given HQL query is not an {@code update} or {@code delete}.
	 */
	@Incubating
	<T> MutationSpecification<T> createMutationSpecification(String hql, Class<T> mutationTarget)
			throws IllegalMutationQueryException;

	/**
	 * Create a {@link Query} instance for the named query.
	 *
	 * @param queryName the name of a predefined named query
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query getNamedQuery(String queryName);

	/**
	 * Get a {@link NativeQuery} instance for a named native SQL query
	 *
	 * @param name The name of the predefined query
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery getNamedNativeQuery(String name);

	/**
	 * Get a {@link NativeQuery} instance for a named native SQL query
	 *
	 * @param name The name of the predefined query
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery getNamedNativeQuery(String name, String resultSetMapping);
}
