//$Id: CacheMode.java 9194 2006-02-01 19:59:07Z steveebersole $
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
