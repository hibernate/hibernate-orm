/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

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
	 * Create a {@link Query} instance for the named query.
	 *
	 * @param queryName the name of a pre-defined, named query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @throws IllegalArgumentException if a query has not been
	 * defined with the given name or if the query string is
	 * found to be invalid
	 */
	Query getNamedQuery(String queryName);

	/**
	 * Create a {@link Query} instance for the given HQL/JPQL query string.
	 *
	 * @param queryString The HQL/JPQL query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @see javax.persistence.EntityManager#createQuery(String)
	 */
	Query createQuery(String queryString);

	/**
	 * Create a typed {@link Query} instance for the given HQL/JPQL query string.
	 *
	 * @param queryString The HQL/JPQL query
	 *
	 * @return The Query instance for manipulation and execution
	 *
	 * @see javax.persistence.EntityManager#createQuery(String,Class)
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
	 * @see javax.persistence.EntityManager#createNamedQuery(String)
	 */
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
	 * @see javax.persistence.EntityManager#createNamedQuery(String,Class)
	 */
	<R> Query<R> createNamedQuery(String name, Class<R> resultClass);

	/**
	 * Create a {@link NativeQuery} instance for the given SQL query string.
	 *
	 * @param queryString The SQL query
	 *
	 * @return The query instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) use {@link #createNativeQuery(String)} instead
	 */
	@Deprecated
	default NativeQuery createSQLQuery(String queryString) {
		NativeQuery query = createNativeQuery( queryString );
		query.setComment( "dynamic native SQL query" );
		return query;
	}

	/**
	 * Create a NativeQuery instance for the given native (SQL) query
	 *
	 * @param sqlString a native SQL query string
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see javax.persistence.EntityManager#createNativeQuery(String)
	 */
	NativeQuery createNativeQuery(String sqlString);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultClass The Java type to map results to
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see javax.persistence.EntityManager#createNativeQuery(String,Class)
	 */
	<R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass);

	/**
	 * Create a NativeQuery instance for the given native (SQL) query using
	 * implicit mapping to the specified Java type.
	 *
	 * @param sqlString Native (SQL) query string
	 * @param resultSetMapping The explicit (named) result mapping
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @see javax.persistence.EntityManager#createNativeQuery(String,Class)
	 * @see javax.persistence.SqlResultSetMapping
	 */
	NativeQuery createNativeQuery(String sqlString, String resultSetMapping);

	/**
	 * Get a NativeQuery instance for a named native SQL query
	 *
	 * @param name The name of the pre-defined query
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 *
	 * @deprecated (since 5.2) use {@link #getNamedNativeQuery(String)} instead
	 */
	@Deprecated
	default NativeQuery getNamedSQLQuery(String name) {
		return getNamedNativeQuery( name );
	}

	/**
	 * Get a NativeQuery instance for a named native SQL query
	 *
	 * @param name The name of the pre-defined query
	 *
	 * @return The NativeQuery instance for manipulation and execution
	 */
	NativeQuery getNamedNativeQuery(String name);
}
