//$Id: ScrollMode.java 4369 2004-08-18 00:28:43Z oneovthafew $
package org.hibernate;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Specifies the type of JDBC scrollable result set to use
 * underneath a <tt>ScrollableResults</tt>
 *
 * @see Query#scroll(ScrollMode)
 * @see ScrollableResults
 * @author Gavin King
 */
public final class ScrollMode implements Serializable {
	private final int resultSetType;
	private final String name;
	private static final Map INSTANCES = new HashMap();

	private ScrollMode(int level, String name) {
		this.resultSetType=level;
		this.name=name;
	}
	
	public String toString() {
		return name;
	}
	
	/**
	 * @return the JDBC result set type code
	 */
	public int toResultSetType() {
		return resultSetType;
	}
	
	/**
	 * @see java.sql.ResultSet.TYPE_FORWARD_ONLY
	 */
	public static final ScrollMode FORWARD_ONLY = new ScrollMode(ResultSet.TYPE_FORWARD_ONLY, "FORWARD_ONLY");
	/**
	 * @see java.sql.ResultSet.TYPE_SCROLL_SENSITIVE
	 */
	public static final ScrollMode SCROLL_SENSITIVE = new ScrollMode(ResultSet.TYPE_SCROLL_SENSITIVE, "SCROLL_SENSITIVE");
	/**
	 * Note that since the Hibernate session acts as a cache, you
	 * might need to expicitly evict objects, if you need to see
	 * changes made by other transactions. 
	 * @see java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE
	 */
	public static final ScrollMode SCROLL_INSENSITIVE = new ScrollMode(ResultSet.TYPE_SCROLL_INSENSITIVE, "SCROLL_INSENSITIVE");
	
	public boolean lessThan(ScrollMode other) {
		return this.resultSetType<other.resultSetType;
	}

	static {
		INSTANCES.put( FORWARD_ONLY.name, FORWARD_ONLY );
		INSTANCES.put( SCROLL_INSENSITIVE.name, SCROLL_INSENSITIVE );
		INSTANCES.put( SCROLL_SENSITIVE.name, SCROLL_SENSITIVE );
	}

	private Object readResolve() {
		return INSTANCES.get(name);
	}

}






