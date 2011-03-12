/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls how the session interacts with the second-level
 * cache and query cache.
 *
 * @see Session#setCacheMode(CacheMode)
 * @author Gavin King
 */
public final class CacheMode implements Serializable {
	private final String name;
	private final boolean isPutEnabled;
	private final boolean isGetEnabled;
	private static final Map INSTANCES = new HashMap();

	private CacheMode(String name, boolean isPutEnabled, boolean isGetEnabled) {
		this.name=name;
		this.isPutEnabled = isPutEnabled;
		this.isGetEnabled = isGetEnabled;
	}
	public String toString() {
		return name;
	}
	public boolean isPutEnabled() {
		return isPutEnabled;
	}
	public boolean isGetEnabled() {
		return isGetEnabled;
	}
	/**
	 * The session may read items from the cache, and add items to the cache
	 */
	public static final CacheMode NORMAL = new CacheMode("NORMAL", true, true);
	/**
	 * The session will never interact with the cache, except to invalidate
	 * cache items when updates occur
	 */
	public static final CacheMode IGNORE = new CacheMode("IGNORE", false, false);
	/**
	 * The session may read items from the cache, but will not add items, 
	 * except to invalidate items when updates occur
	 */
	public static final CacheMode GET = new CacheMode("GET", false, true);
	/**
	 * The session will never read items from the cache, but will add items
	 * to the cache as it reads them from the database.
	 */
	public static final CacheMode PUT = new CacheMode("PUT", true, false);
	
	/**
	 * The session will never read items from the cache, but will add items
	 * to the cache as it reads them from the database. In this mode, the
	 * effect of <tt>hibernate.cache.use_minimal_puts</tt> is bypassed, in
	 * order to <em>force</em> a cache refresh
	 */
	public static final CacheMode REFRESH = new CacheMode("REFRESH", true, false);
	
	static {
		INSTANCES.put( NORMAL.name, NORMAL );
		INSTANCES.put( IGNORE.name, IGNORE );
		INSTANCES.put( GET.name, GET );
		INSTANCES.put( PUT.name, PUT );
		INSTANCES.put( REFRESH.name, REFRESH );
	}

	private Object readResolve() {
		return INSTANCES.get( name );
	}

	public static CacheMode parse(String name) {
		return ( CacheMode ) INSTANCES.get( name );
	}
}
