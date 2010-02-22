/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
 * Boston, MA  02110-1301  USA\
 */
package org.hibernate.ejb;

import java.util.Set;
import java.util.HashSet;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class QueryHints {
	/**
	 * @deprecated HINT_TIMEOUT (org.hibernate.timeout),
	 * instead use SPEC_HINT_TIMEOUT (javax.persistence.query.timeout)
	 */
	public static final String HINT_TIMEOUT = "org.hibernate.timeout"; // Query timeout in seconds
	public static final String SPEC_HINT_TIMEOUT = "javax.persistence.query.timeout"; // timeout in milliseconds
	public static final String HINT_COMMENT = "org.hibernate.comment";
	public static final String HINT_FETCH_SIZE = "org.hibernate.fetchSize";
	public static final String HINT_CACHE_REGION = "org.hibernate.cacheRegion";
	public static final String HINT_CACHEABLE = "org.hibernate.cacheable";
	public static final String HINT_READONLY = "org.hibernate.readOnly";
	public static final String HINT_CACHE_MODE = "org.hibernate.cacheMode";
	public static final String HINT_FLUSH_MODE = "org.hibernate.flushMode";

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
		return java.util.Collections.unmodifiableSet( hints );
	}

	public static Set<String> getDefinedHints() {
		return HINTS;
	}
}
