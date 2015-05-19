/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;

/**
 * @author Gavin King
 */
public class SubselectFetch {
	private final Set resultingEntityKeys;
	private final String queryString;
	private final String alias;
	private final Loadable loadable;
	private final QueryParameters queryParameters;
	private final Map namedParameterLocMap;

	public SubselectFetch(
			//final String queryString,
			final String alias,
			final Loadable loadable,
			final QueryParameters queryParameters,
			final Set resultingEntityKeys,
			final Map namedParameterLocMap) {
		this.resultingEntityKeys = resultingEntityKeys;
		this.queryParameters = queryParameters;
		this.namedParameterLocMap = namedParameterLocMap;
		this.loadable = loadable;
		this.alias = alias;

		//TODO: ugly here:
		final String queryString = queryParameters.getFilteredSQL();
		int fromIndex = queryString.indexOf( " from " );
		int orderByIndex = queryString.lastIndexOf( "order by" );
		this.queryString = orderByIndex > 0
				? queryString.substring( fromIndex, orderByIndex )
				: queryString.substring( fromIndex );
	}

	public QueryParameters getQueryParameters() {
		return queryParameters;
	}

	/**
	 * Get the Set of EntityKeys
	 */
	public Set getResult() {
		return resultingEntityKeys;
	}

	public String toSubselectString(String ukname) {
		String[] joinColumns = ukname == null
				? StringHelper.qualify( alias, loadable.getIdentifierColumnNames() )
				: ( (PropertyMapping) loadable ).toColumns( alias, ukname );

		return "select " + StringHelper.join( ", ", joinColumns ) + queryString;
	}

	@Override
	public String toString() {
		return "SubselectFetch(" + queryString + ')';
	}

	public Map getNamedParameterLocMap() {
		return namedParameterLocMap;
	}

}
