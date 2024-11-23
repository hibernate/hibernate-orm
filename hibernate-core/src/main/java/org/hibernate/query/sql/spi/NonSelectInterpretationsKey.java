/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.hibernate.query.spi.QueryInterpretationCache;

/**
 * QueryInterpretations key for non-select NativeQuery instances
 *
 * @author Steve Ebersole
 */
public class NonSelectInterpretationsKey implements QueryInterpretationCache.Key {
	private final String sql;
	private final Collection<String> querySpaces;

	public NonSelectInterpretationsKey(String sql, Collection<String> querySpaces) {
		this.sql = sql;
		this.querySpaces = querySpaces == null ? Collections.emptySet() : querySpaces;
	}

	@Override
	public String getQueryString() {
		return sql;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new NonSelectInterpretationsKey(
				sql,
				querySpaces.isEmpty() ? Collections.emptySet() : new HashSet<>( querySpaces )
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NonSelectInterpretationsKey that = (NonSelectInterpretationsKey) o;

		if ( !sql.equals( that.sql ) ) {
			return false;
		}
		return querySpaces.equals( that.querySpaces );
	}

	@Override
	public int hashCode() {
		int result = sql.hashCode();
		result = 31 * result + querySpaces.hashCode();
		return result;
	}
}
