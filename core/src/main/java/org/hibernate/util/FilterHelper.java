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
package org.hibernate.util;

import org.hibernate.sql.Template;
import org.hibernate.impl.FilterImpl;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;

import java.util.Map;
import java.util.Iterator;

/**
 * Implementation of FilterHelper.
 *
 * @author Steve Ebersole
 */
public class FilterHelper {

	private final String[] filterNames;
	private final String[] filterConditions;

	/**
	 * The map of defined filters.  This is expected to be in format
	 * where the filter names are the map keys, and the defined
	 * conditions are the values.
	 *
	 * @param filters The map of defined filters.
	 * @param dialect The sql dialect
	 * @param functionRegistry The SQL function registry
	 */
	public FilterHelper(Map filters, Dialect dialect, SQLFunctionRegistry functionRegistry) {
		int filterCount = filters.size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		Iterator iter = filters.entrySet().iterator();
		filterCount = 0;
		while ( iter.hasNext() ) {
			final Map.Entry entry = (Map.Entry) iter.next();
			filterNames[filterCount] = (String) entry.getKey();
			filterConditions[filterCount] = Template.renderWhereStringTemplate(
					(String) entry.getValue(),
					FilterImpl.MARKER,
					dialect,
					functionRegistry
				);
			filterConditions[filterCount] = StringHelper.replace( filterConditions[filterCount],
					":",
					":" + filterNames[filterCount] + "." );
			filterCount++;
		}
	}

	public boolean isAffectedBy(Map enabledFilters) {
		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				return true;
			}
		}
		return false;
	}

	public String render(String alias, Map enabledFilters) {
		StringBuffer buffer = new StringBuffer();
		render( buffer, alias, enabledFilters );
		return buffer.toString();
	}

	public void render(StringBuffer buffer, String alias, Map enabledFilters) {
		if ( filterNames != null && filterNames.length > 0 ) {
			for ( int i = 0, max = filterNames.length; i < max; i++ ) {
				if ( enabledFilters.containsKey( filterNames[i] ) ) {
					final String condition = filterConditions[i];
					if ( StringHelper.isNotEmpty( condition ) ) {
						buffer.append( " and " )
								.append( StringHelper.replace( condition, FilterImpl.MARKER, alias ) );
					}
				}
			}
		}
	}
}
