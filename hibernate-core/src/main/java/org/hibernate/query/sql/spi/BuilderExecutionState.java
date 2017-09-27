/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.sql.results.spi.FetchParent;

/**
 * todo (6.0) : isnt this really just a "from clause index"?
 *
 * @author Steve Ebersole
 */
public class BuilderExecutionState {
	private Map<String,FetchParent> fetchParentByAliasMap;

	public FetchParent getFetchParentByParentAlias(String parentAlias) {
		if ( fetchParentByAliasMap == null ) {
			return null;
		}

		return fetchParentByAliasMap.get( parentAlias );
	}

	public void registerFetchParentByAlias(String alias, FetchParent fetchParent) {
		if ( fetchParentByAliasMap == null ) {
			fetchParentByAliasMap = new HashMap<>();
		}

		fetchParentByAliasMap.put( alias, fetchParent );
	}
}
