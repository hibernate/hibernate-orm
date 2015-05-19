/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

/**
 * Consolidation of hints available to Hibernate JPA queries.  Mainly used to define features available on
 * Hibernate queries that have no corollary in JPA queries.
 */
public class QueryHints {
	/**
	 * Disallow instantiation.
	 */
	private QueryHints() {
	}

	/**
	 * The cache mode to use.
	 *
	 * @see org.hibernate.Query#setCacheMode
	 * @see org.hibernate.SQLQuery#setCacheMode
	 */
	public static final String CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * The cache region to use.
	 *
	 * @see org.hibernate.Query#setCacheRegion
	 * @see org.hibernate.SQLQuery#setCacheRegion
	 */
	public static final String CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Are the query results cacheable?
	 *
	 * @see org.hibernate.Query#setCacheable
	 * @see org.hibernate.SQLQuery#setCacheable
	 */
	public static final String CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Is the query callable?  Note: only valid for named native sql queries.
	 */
	public static final String CALLABLE = "org.hibernate.callable";

	/**
	 * Defines a comment to be applied to the SQL sent to the database.
	 *
	 * @see org.hibernate.Query#setComment
	 * @see org.hibernate.SQLQuery#setComment
	 */
	public static final String COMMENT = "org.hibernate.comment";

	/**
	 * Defines the JDBC fetch size to use.
	 *
	 * @see org.hibernate.Query#setFetchSize
	 * @see org.hibernate.SQLQuery#setFetchSize
	 */
	public static final String FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * The flush mode to associate with the execution of the query.
	 *
	 * @see org.hibernate.Query#setFlushMode
	 * @see org.hibernate.SQLQuery#setFlushMode
	 * @see org.hibernate.Session#setFlushMode
	 */
	public static final String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Should entities returned from the query be set in read only mode?
	 *
	 * @see org.hibernate.Query#setReadOnly
	 * @see org.hibernate.SQLQuery#setReadOnly
	 * @see org.hibernate.Session#setReadOnly
	 */
	public static final String READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Apply a Hibernate query timeout, which is defined in <b>seconds</b>.
	 *
	 * @see org.hibernate.Query#setTimeout
	 * @see org.hibernate.SQLQuery#setTimeout
	 */
	public static final String TIMEOUT_HIBERNATE = "org.hibernate.timeout";

	/**
	 * Apply a JPA query timeout, which is defined in <b>milliseconds</b>.
	 */
	public static final String TIMEOUT_JPA = "javax.persistence.query.timeout";

	/**
	 * Available to apply lock mode to a native SQL query since JPA requires that
	 * {@link javax.persistence.Query#setLockMode} throw an IllegalStateException if called for a native query.
	 * <p/>
	 * Accepts a {@link javax.persistence.LockModeType} or a {@link org.hibernate.LockMode}
	 */
	public static final String NATIVE_LOCKMODE = "org.hibernate.lockMode";
	
	/**
	 * Hint providing a "fetchgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).
	 * 
	 * Note: Currently, attributes that are not specified are treated as FetchType.LAZY or FetchType.EAGER depending
	 * on the attribute's definition in metadata, rather than forcing FetchType.LAZY.
	 */
	public static final String FETCHGRAPH = "javax.persistence.fetchgraph";
	
	/**
	 * Hint providing a "loadgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).  Attributes that are not specified are treated as
	 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition in metadata
	 */
	public static final String LOADGRAPH = "javax.persistence.loadgraph";

}
