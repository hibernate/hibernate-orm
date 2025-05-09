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
import org.hibernate.query.criteria.JpaCriteriaInsert;

/**
 * An object which can produce instances of {@link SelectionQuery} and {@link MutationQuery}.
 * Implementors include {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession}.
 * Many operations of the interface have the same or very similar signatures to operations of
 * {@link jakarta.persistence.EntityManager}. They are declared here to allow reuse by
 * {@code StatelessSession}.
 * <p>
 * There are three fundamental ways to express a query:
 * <ul>
 * <li>in <em>Hibernate Query Language</em>, an object-oriented query dialect of SQL which is
 *     a superset of the <em>Jakarta Persistence Query Language</em>,
 * <li>in the native SQL dialect of the database, or
 * <li>using the {@linkplain jakarta.persistence.criteria.CriteriaBuilder Criteria API} defined
 *     by JPA, along with {@linkplain org.hibernate.query.criteria.HibernateCriteriaBuilder
 *     extensions} defined by Hibernate.
 * </ul>
 * <p>
 * In each case, the object used to execute the query depends on whether the query is a
 * selection query or a mutation query.
 * <ul>
 * <li>selection queries are executed via an instance of {@link SelectionQuery}, while
 * <li>mutation queries are executed via an instance of {@link MutationQuery}, but
 * <li>since JPA makes no such distinction within its API, the type {@link Query} is a mixin of
 *     {@code SelectionQuery}, {@code MutationQuery}, and {@link jakarta.persistence.TypedQuery}.
 * </ul>
 * This interface declares operations for creating instances of these objects.
 * <table style="width:100%;margin:10px">
 *     <tr>
 *         <th style="width:10%"></th>
 *         <th style="text-align:left;width:45%">Selection</th>
 *         <th style="text-align:left;width:45%">Mutation</th>
 *     </tr>
 *     <tr>
 *         <td>HQL</td>
 *         <td>{@link #createSelectionQuery(String,Class)} and
 *             {@link #createSelectionQuery(String,EntityGraph)}</td>
 *         <td>{@link #createMutationQuery(String)}</td>
 *     </tr>
 *     <tr>
 *         <td>SQL</td>
 *         <td>{@link #createNativeQuery(String,Class)} and
 *             {@link #createNativeQuery(String,String,Class)}</td>
 *         <td>{@link #createNativeMutationQuery(String)}</td>
 *     </tr>
 *     <tr>
 *         <td>Criteria</td>
 *         <td>{@link #createSelectionQuery(CriteriaQuery)}</td>
 *         <td>{@link #createMutationQuery(CriteriaUpdate)},
 *             {@link #createMutationQuery(CriteriaDelete)}, and
 *             {@link #createMutationQuery(JpaCriteriaInsert)}</td>
 *     </tr>
 *     <tr>
 *         <td>Named queries</td>
 *         <td>{@link #createNamedSelectionQuery(String,Class)}</td>
 *         <td>{@link #createNamedMutationQuery(String)}</td>
 *     </tr>
 * </table>
 * <p>
 * Operations like {@link #createSelectionQuery(String, Class) createSelectionQuery()},
 * {@link #createNamedSelectionQuery(String, Class) createNamedSelectionQuery()}, and
 * {@link #createNativeQuery(String, Class) createNativeQuery()} accept a Java
 * {@linkplain Class class object} indicating the <em>result type</em> of the query.
 * <ul>
 * <li>The result type might be an {@linkplain jakarta.persistence.Entity entity} class, when
 *     the query returns an entity:
 * <pre>
 * List&lt;Book&gt; allBooks =
 *         session.createNativeQuery("select * from books order by title", Book.class)
 *                 .getResultList();
 * </pre>
 * <li>It might be a {@linkplain org.hibernate.type.descriptor.java.JavaType basic type} like
 *     {@code String} or {@code Long}:
 * <pre>
 * List&lt;String&gt; allTitles =
 *         session.createNativeQuery("select distinct title from books order by title", String.class)
 *                 .getResultList();
 * </pre>
 * <li>Finally, the result type might be a class used to package the elements of a {@code select}
 *     list, such as a Java record with an appropriate constructor, {@code Map}, {@code List}, or
 *     {@code Object[]}:
 * <pre>
 * record IsbnAndTitle(String isbn, String title) {}
 *
 * List&lt;IsbnAndTitle&gt; allBooks =
 *         session.createNativeQuery("select isbn, title from books order by title", IsbnAndTitle.class)
 *                 .getResultList();
 * </pre>
 * </ul>
 * For a {@linkplain #createQuery(CriteriaQuery) criteria query}, the result type is already
 * determined by {@link CriteriaQuery#getResultType()}.
 *
 * @apiNote Unlike the corresponding operations of {@code EntityManager}, operations for creating
 * untyped instances of {@code Query} are all marked as deprecated. Clients must migrate to the
 * use of the equivalent operations which accept a {@link Class} and return a typed {@code Query}.
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
