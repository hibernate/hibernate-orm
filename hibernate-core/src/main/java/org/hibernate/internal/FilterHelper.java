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
package org.hibernate.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.Template;

/**
 * Implementation of FilterHelper.
 *
 * @author Steve Ebersole
 * @author Rob Worsnop
 */
public class FilterHelper {

	private final String[] filterNames;
	private final String[] filterConditions;
	private final boolean[] filterAutoAliasFlags;
	private final Map<String,String>[] filterAliasTableMaps;

	/**
	 * The map of defined filters.  This is expected to be in format
	 * where the filter names are the map keys, and the defined
	 * conditions are the values.
	 *
	 * @param filters The map of defined filters.
	 * @param factory The session factory
	 */
	public FilterHelper(List<FilterConfiguration> filters, SessionFactoryImplementor factory) {
		int filterCount = filters.size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		filterAutoAliasFlags = new boolean[filterCount];
		filterAliasTableMaps = new Map[filterCount];
		filterCount = 0;
		for ( final FilterConfiguration filter : filters ) {
			filterAutoAliasFlags[filterCount] = false;
			filterNames[filterCount] = filter.getName();
			filterConditions[filterCount] = filter.getCondition();
			filterAliasTableMaps[filterCount] = filter.getAliasTableMap( factory );
			if ( (filterAliasTableMaps[filterCount].isEmpty() || isTableFromPersistentClass( filterAliasTableMaps[filterCount] )) && filter
					.useAutoAliasInjection() ) {
				filterConditions[filterCount] = Template.renderWhereStringTemplate(
						filter.getCondition(),
						FilterImpl.MARKER,
						factory.getDialect(),
						factory.getSqlFunctionRegistry()
				);
				filterAutoAliasFlags[filterCount] = true;
			}
			filterConditions[filterCount] = StringHelper.replace(
					filterConditions[filterCount],
					":",
					":" + filterNames[filterCount] + "."
			);
			filterCount++;
		}
	}
	
	private static boolean isTableFromPersistentClass(Map<String,String> aliasTableMap){
		return aliasTableMap.size() == 1 && aliasTableMap.containsKey(null);
	}

	public boolean isAffectedBy(Map enabledFilters) {
		for ( String filterName : filterNames ) {
			if ( enabledFilters.containsKey( filterName ) ) {
				return true;
			}
		}
		return false;
	}

	public String render(FilterAliasGenerator aliasGenerator, Map enabledFilters) {
		StringBuilder buffer = new StringBuilder();
		render( buffer, aliasGenerator, enabledFilters );
		return buffer.toString();
	}

	public void render(StringBuilder buffer, FilterAliasGenerator aliasGenerator, Map enabledFilters) {
		if ( CollectionHelper.isEmpty( filterNames ) ) {
			return;
		}
		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				final String condition = filterConditions[i];
				if ( StringHelper.isNotEmpty( condition ) ) {
					buffer.append( " and " ).append( render( aliasGenerator, i ) );
				}
			}
		}
	}
	
	private String render(FilterAliasGenerator aliasGenerator, int filterIndex){
		Map<String,String> aliasTableMap = filterAliasTableMaps[filterIndex];
		String condition = filterConditions[filterIndex];
		if (filterAutoAliasFlags[filterIndex]){
			return StringHelper.replace(condition, FilterImpl.MARKER, aliasGenerator.getAlias(aliasTableMap.get(null)));
		} else if (isTableFromPersistentClass(aliasTableMap)){
			return condition.replace("{alias}", aliasGenerator.getAlias(aliasTableMap.get(null)));
		} else {
			for (Map.Entry<String, String> entry : aliasTableMap.entrySet()){
				condition = condition.replace("{"+entry.getKey()+"}", aliasGenerator.getAlias(entry.getValue()));
			}
			return condition;
		}
	}
}
