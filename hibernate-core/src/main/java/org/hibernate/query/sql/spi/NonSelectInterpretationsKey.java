/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Collection;

import org.hibernate.query.spi.QueryPlanCache;

/**
 * QueryInterpretations key for non-select NativeQuery instances
 *
 * @author Steve Ebersole
 */
public class NonSelectInterpretationsKey implements QueryPlanCache.Key {
	private final String sql;
	private final Collection<String> querySpaces;

	public NonSelectInterpretationsKey(String sql, Collection<String> querySpaces) {
		this.sql = sql;
		this.querySpaces = querySpaces;
	}
}
