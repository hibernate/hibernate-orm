/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

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
	 * Create a {@link Query} instance for the given HQL/JPQL query, or
	 * HQL/JPQL insert, update, or delete statement.
	 *
	 * @param queryString The HQL/JPQL query
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 *
	 * @deprecated use {@link #createQuery(String, Class)},
	 * {@link #createUntypedQuery}, {@link #createMutationQuery(String)}
	 * or {@link #createMutationQuery} depending on intention
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	Query createQuery(String queryString);

	/**
	 * Create an {@link UntypedQuery} reference for the given HQL.
	 * Only valid for select queries
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 */
	UntypedQuery createUntypedQuery(String hqlString);

	/**
	 * Create a typed {@link Query} instance for the given HQL/JPQL query string.
	 * <p>
	 * The returned {@code Query} may be executed by calling
	 * {@link Query#getResultList()} or {@link Query#getSingleResult()}.
	 *
	 * @param queryString The HQL/JPQL query
	 * @param resultClass The type of the query result
	 * @return The Query instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String,Class)
	 */
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

	/**
	 * Create a MutationQuery reference for the given HQL insert,
	 * update, or delete statement.
	 */
	MutationQuery createMutationQuery(String hqlString);

	/**
	 * Create a typed {@link Query} instance for the given named query.
	 * The named query might be defined in HQL or in native SQL.
	 *
	 * @param name the name of a pre-defined, named query
	 *
	 * @return The {@link Query} instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 *
	 * @see jakarta.persistence.EntityManager#createNamedQuery(String)
	 * 
	 * @deprecated use {@link #createNamedQuery(String, Class)} or {@link #createNamedMutationQuery(String)}
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
	 * Create a {@link MutationQuery} instance for the given named insert,
	 * update, or delete HQL query. The named query might be defined as
	 * {@linkplain  jakarta.persistence.NamedQuery HQL}) or
	 * {@linkplain  jakarta.persistence.NamedNativeQuery native-SQL}.
	 *
	 * @throws IllegalArgumentException if no query has been
	 * defined with the given name or if the query string is
	 * found to be invalid (a selection e.g.)
	 */
	MutationQuery createNamedMutationQuery(String name);

	/**
	 * Create a {@link NativeQuery} instance for the given native (SQL) query
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
	 * Create a {@link NativeQuery} instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 * <p>
	 * If the given class is an entity class, this method is equivalent to
	 * {@code createNativeQuery(sqlString).addEntity("alias1", resultClass)}.
	 *
	 * @param sqlString The native (SQL) query string
	 * @param resultClass The Java entity type to map results to
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass);

	/**
	 * Create a {@link NativeQuery} instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 * <p>
	 * If the given class is an entity class, this method is equivalent to
	 * {@code createNativeQuery(sqlString).addEntity(tableAlias, resultClass)}.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultClass The Java entity type to map results to
	 * @param tableAlias The table alias for columns in the result set
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias);

	/**
	 * Create a {@link NativeQuery} instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 *
	 * @param sqlString The native (SQL) query string
	 * @param resultSetMappingName The explicit result mapping name
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 * @see jakarta.persistence.SqlResultSetMapping
	 * 
	 * @deprecated use {@link #createNativeQuery(String, String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery createNativeQuery(String sqlString, String resultSetMappingName);

	/**
	 * Create a {@link NativeQuery} instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
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
	 * Create a {@link NativeQuery} instance for the given native (SQL) statement
	 *
	 * @param sqlString a native SQL statement string
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 */
	MutationNativeQuery createNativeMutationQuery(String sqlString);

	/**
	 * Create a {@link Query} for the given JPA {@link CriteriaQuery}
	 */
	<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery);

	/**
	 * Create a {@link MutationQuery} for the given JPA {@link CriteriaUpdate}
	 */
	MutationQuery createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery);

	/**
	 * Create a {@link MutationQuery} for the given JPA {@link CriteriaDelete}
	 */
	MutationQuery createQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery);

	/**
	 * Create a {@link Query} instance for the named query.
	 *
	 * @param queryName the name of a pre-defined, named query
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
	 * @param name The name of the pre-defined query
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
	 * @param name The name of the pre-defined query
	 *
	 * @return The {@link NativeQuery} instance for manipulation and execution
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated(since = "6.0") @SuppressWarnings("rawtypes")
	NativeQuery getNamedNativeQuery(String name, String resultSetMapping);
}
