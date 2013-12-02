/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.annotations.QueryHints.CACHEABLE;
import static org.hibernate.annotations.QueryHints.CACHE_MODE;
import static org.hibernate.annotations.QueryHints.CACHE_REGION;
import static org.hibernate.annotations.QueryHints.COMMENT;
import static org.hibernate.annotations.QueryHints.FETCHGRAPH;
import static org.hibernate.annotations.QueryHints.FETCH_SIZE;
import static org.hibernate.annotations.QueryHints.FLUSH_MODE;
import static org.hibernate.annotations.QueryHints.LOADGRAPH;
import static org.hibernate.annotations.QueryHints.NATIVE_LOCKMODE;
import static org.hibernate.annotations.QueryHints.READ_ONLY;
import static org.hibernate.annotations.QueryHints.TIMEOUT_HIBERNATE;
import static org.hibernate.annotations.QueryHints.TIMEOUT_JPA;

/**
 * Defines the supported JPA query hints
 *
 * @author Steve Ebersole
 */
public class QueryHints {
	/**
	 * The hint key for specifying a query timeout per Hibernate O/RM, which defines the timeout in seconds.
	 *
	 * @deprecated use {@link #SPEC_HINT_TIMEOUT} instead
	 */
	@Deprecated
	public static final String HINT_TIMEOUT = TIMEOUT_HIBERNATE;

	/**
	 * The hint key for specifying a query timeout per JPA, which defines the timeout in milliseconds
	 */
	public static final String SPEC_HINT_TIMEOUT = TIMEOUT_JPA;

	/**
	 * The hint key for specifying a comment which is to be embedded into the SQL sent to the database.
	 */
	public static final String HINT_COMMENT = COMMENT;

	/**
	 * The hint key for specifying a JDBC fetch size, used when executing the resulting SQL.
	 */
	public static final String HINT_FETCH_SIZE = FETCH_SIZE;

	/**
	 * The hint key for specifying whether the query results should be cached for the next (cached) execution of the
	 * "same query".
	 */
	public static final String HINT_CACHEABLE = CACHEABLE;

	/**
	 * The hint key for specifying the name of the cache region (within Hibernate's query result cache region)
	 * to use for storing the query results.
	 */
	public static final String HINT_CACHE_REGION = CACHE_REGION;

	/**
	 * The hint key for specifying that objects loaded into the persistence context as a result of this query execution
	 * should be associated with the persistence context as read-only.
	 */
	public static final String HINT_READONLY = READ_ONLY;

	/**
	 * The hint key for specifying the cache mode ({@link org.hibernate.CacheMode}) to be in effect for the
	 * execution of the hinted query.
	 */
	public static final String HINT_CACHE_MODE = CACHE_MODE;

	/**
	 * The hint key for specifying the flush mode ({@link org.hibernate.FlushMode}) to be in effect for the
	 * execution of the hinted query.
	 */
	public static final String HINT_FLUSH_MODE = FLUSH_MODE;

	public static final String HINT_NATIVE_LOCKMODE = NATIVE_LOCKMODE;
	
	/**
	 * Hint providing a "fetchgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).
	 * 
	 * Note: Currently, attributes that are not specified are treated as FetchType.LAZY or FetchType.EAGER depending
	 * on the attribute's definition in metadata, rather than forcing FetchType.LAZY.
	 */
	public static final String HINT_FETCHGRAPH = FETCHGRAPH;
	
	/**
	 * Hint providing a "loadgraph" EntityGraph.  Attributes explicitly specified as AttributeNodes are treated as
	 * FetchType.EAGER (via join fetch or subsequent select).  Attributes that are not specified are treated as
	 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition in metadata
	 */
	public static final String HINT_LOADGRAPH = LOADGRAPH;

	private static final Set<String> HINTS = buildHintsSet();

	private static Set<String> buildHintsSet() {
		HashSet<String> hints = new HashSet<String>();
		hints.add( HINT_TIMEOUT );
		hints.add( SPEC_HINT_TIMEOUT );
		hints.add( HINT_COMMENT );
		hints.add( HINT_FETCH_SIZE );
		hints.add( HINT_CACHE_REGION );
		hints.add( HINT_CACHEABLE );
		hints.add( HINT_READONLY );
		hints.add( HINT_CACHE_MODE );
		hints.add( HINT_FLUSH_MODE );
		hints.add( HINT_NATIVE_LOCKMODE );
		hints.add( HINT_FETCHGRAPH );
		hints.add( HINT_LOADGRAPH );
		return java.util.Collections.unmodifiableSet( hints );
	}

	public static Set<String> getDefinedHints() {
		return HINTS;
	}

	protected QueryHints() {
	}
}
