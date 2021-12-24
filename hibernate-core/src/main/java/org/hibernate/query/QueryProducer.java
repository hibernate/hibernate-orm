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
 * Contract for things that can produce Query instances.  Expected implementors include
 * Session and StatelessSession.
 * <p/>
 * It defines these query creation methods in the signature defined by EntityManager.  In a way
 * it defines a subset of the EntityManager contract to be reused by both Session and StatelessSession.
 *
 * @author Steve Ebersole
 */
public interface QueryProducer {
	/**
	 * Create a {@link Query} instance for the given HQL/JPQL query string.
	 *
	 * @param queryString The HQL/JPQL query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String)
	 * @deprecated use {@link #createQuery(String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	Query createQuery(String queryString);

	/**
	 * Create a typed {@link Query} instance for the given HQL/JPQL query string.
	 *
	 * @param queryString The HQL/JPQL query
	 * @param resultClass The type of the query result
	 * @return The Query instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(String,Class)
	 */
	<R> Query<R> createQuery(String queryString, Class<R> resultClass);

	/**
	 * The JPA-defined named query creation method.  This form can represent an
	 * HQL/JPQL query or a native query.
	 *
	 * @param name the name of a pre-defined, named query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 *
	 * @see jakarta.persistence.EntityManager#createNamedQuery(String)
	 * 
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	Query createNamedQuery(String name);

	/**
	 * The JPA-defined named, typed query creation method.  This form can only
	 * represent an HQL/JPQL query (not a native query).
	 *
	 * @param name the name of a query defined in metadata
	 * @param resultClass the type of the query result
	 *
	 * @return The Query instance for manipulation and execution
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
	 * Create a NativeQuery instance for the given native (SQL) query
	 *
	 * @param sqlString a native SQL query string
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String)
	 *
	 * @deprecated use {@link #createNativeQuery(String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	NativeQuery createNativeQuery(String sqlString);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 * <p>
	 * If the given class is an entity class, this method is equivalent to
	 * {@code createNativeQuery(sqlString).addEntity("alias1", resultClass)}.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultClass The Java entity type to map results to
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 * <p>
	 * If the given class is an entity class, this method is equivalent to
	 * {@code createNativeQuery(sqlString).addEntity(tableAlias, resultClass)}.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultClass The Java entity type to map results to
	 * @param tableAlias The table alias for columns in the result set
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultSetMappingName The explicit result mapping name
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 * @see jakarta.persistence.SqlResultSetMapping
	 * 
	 * @deprecated use {@link #createNativeQuery(String, String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	NativeQuery createNativeQuery(String sqlString, String resultSetMappingName);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultSetMappingName The explicit result mapping name
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see jakarta.persistence.EntityManager#createNativeQuery(String,Class)
	 * @see jakarta.persistence.SqlResultSetMapping
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass);

	/**
	 * Create a Query for the given JPA {@link CriteriaQuery}
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaQuery)
	 */
	<R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery);

	/**
	 * Create a Query for the given JPA {@link CriteriaUpdate}
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaUpdate)
	 */
	Query<Void> createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery);

	/**
	 * Create a Query for the given JPA {@link CriteriaDelete}
	 *
	 * @see jakarta.persistence.EntityManager#createQuery(CriteriaDelete)
	 */
	Query<Void> createQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery);

	/**
	 * Create a {@link Query} instance for the named query.
	 *
	 * @param queryName the name of a pre-defined, named query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	c	 */
	@Deprecated @SuppressWarnings("rawtypes")
	Query getNamedQuery(String queryName);

	/**
	 * Get a NativeQuery instance for a named native SQL query
	 *
	 * @param name The name of the pre-defined query
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	NativeQuery getNamedNativeQuery(String name);

	/**
	 * Get a NativeQuery instance for a named native SQL query
	 *
	 * @param name The name of the pre-defined query
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @deprecated use {@link #createNamedQuery(String, Class)}
	 */
	@Deprecated @SuppressWarnings("rawtypes")
	NativeQuery getNamedNativeQuery(String name, String resultSetMapping);
}
