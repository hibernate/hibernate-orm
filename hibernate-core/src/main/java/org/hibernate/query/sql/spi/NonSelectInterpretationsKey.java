/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.hibernate.query.spi.QueryInterpretationCache;

import static java.util.Collections.emptySet;

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
		this.querySpaces = querySpaces == null ? emptySet() : querySpaces;
	}

	@Override
	public String getQueryString() {
		return sql;
	}

	@Override
	public QueryInterpretationCache.Key prepareForStore() {
		return new NonSelectInterpretationsKey( sql,
				querySpaces.isEmpty() ? emptySet() : new HashSet<>( querySpaces ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NonSelectInterpretationsKey that) ) {
			return false;
		}
		return sql.equals( that.sql )
			&& querySpaces.equals( that.querySpaces );
	}

	@Override
	public int hashCode() {
		return Objects.hash( sql, querySpaces );
	}
}
