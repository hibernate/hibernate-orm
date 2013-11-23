/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
