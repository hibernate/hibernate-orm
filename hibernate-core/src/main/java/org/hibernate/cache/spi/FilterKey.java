/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.Filter;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.FilterImpl;
import org.hibernate.type.Type;

/**
 * Allows cached queries to be keyed by enabled filters.
 * 
 * @author Gavin King
 */
public final class FilterKey implements Serializable {
	private final String filterName;
	private final Map<String,TypedValue> filterParameters = new HashMap<String,TypedValue>();

	FilterKey(String name, Map<String,?> params, Map<String,Type> types) {
		filterName = name;
		for ( Map.Entry<String, ?> paramEntry : params.entrySet() ) {
			final Type type = types.get( paramEntry.getKey() );
			filterParameters.put( paramEntry.getKey(), new TypedValue( type, paramEntry.getValue() ) );
		}
	}

	@Override
	public int hashCode() {
		int result = 13;
		result = 37 * result + filterName.hashCode();
		result = 37 * result + filterParameters.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if ( !(other instanceof FilterKey) ) {
			return false;
		}

		final FilterKey that = (FilterKey) other;
		return that.filterName.equals( filterName )
				&& that.filterParameters.equals( filterParameters );
	}

	@Override
	public String toString() {
		return "FilterKey[" + filterName + filterParameters + ']';
	}

	/**
	 * Constructs a number of FilterKey instances, given the currently enabled filters
	 *
	 * @param enabledFilters The currently enabled filters
	 *
	 * @return The filter keys, one per enabled filter
	 */
	public static Set<FilterKey> createFilterKeys(Map<String,Filter> enabledFilters) {
		if ( enabledFilters.size() == 0 ) {
			return null;
		}
		final Set<FilterKey> result = new HashSet<FilterKey>();
		for ( Filter filter : enabledFilters.values() ) {
			final FilterKey key = new FilterKey(
					filter.getName(),
					( (FilterImpl) filter ).getParameters(),
					filter.getFilterDefinition().getParameterTypes()
			);
			result.add( key );
		}
		return result;
	}
}
