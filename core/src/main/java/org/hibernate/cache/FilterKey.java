//$Id: FilterKey.java 8754 2005-12-05 23:36:59Z steveebersole $
package org.hibernate.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.engine.TypedValue;
import org.hibernate.impl.FilterImpl;
import org.hibernate.type.Type;

/**
 * Allows cached queries to be keyed by enabled filters.
 * 
 * @author Gavin King
 */
public final class FilterKey implements Serializable {
	private String filterName;
	private Map filterParameters = new HashMap();
	
	public FilterKey(String name, Map params, Map types, EntityMode entityMode) {
		filterName = name;
		Iterator iter = params.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			Type type = (Type) types.get( me.getKey() );
			filterParameters.put( me.getKey(), new TypedValue( type, me.getValue(), entityMode ) );
		}
	}
	
	public int hashCode() {
		int result = 13;
		result = 37 * result + filterName.hashCode();
		result = 37 * result + filterParameters.hashCode();
		return result;
	}
	
	public boolean equals(Object other) {
		if ( !(other instanceof FilterKey) ) return false;
		FilterKey that = (FilterKey) other;
		if ( !that.filterName.equals(filterName) ) return false;
		if ( !that.filterParameters.equals(filterParameters) ) return false;
		return true;
	}
	
	public String toString() {
		return "FilterKey[" + filterName + filterParameters + ']';
	}
	
	public static Set createFilterKeys(Map enabledFilters, EntityMode entityMode) {
		if ( enabledFilters.size()==0 ) return null;
		Set result = new HashSet();
		Iterator iter = enabledFilters.values().iterator();
		while ( iter.hasNext() ) {
			FilterImpl filter = (FilterImpl) iter.next();
			FilterKey key = new FilterKey(
					filter.getName(), 
					filter.getParameters(), 
					filter.getFilterDefinition().getParameterTypes(), 
					entityMode
				);
			result.add(key);
		}
		return result;
	}
}
