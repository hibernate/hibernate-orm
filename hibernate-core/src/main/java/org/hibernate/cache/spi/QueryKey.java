/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * A key that identifies a particular query with bound parameter values.
 * This object is used as a key into the {@linkplain QueryResultsCache
 * query results cache}.
 * <p>
 * Note that the fields of this object must contain every explicit and
 * implicit setting and parameter argument that affects the result list
 * of the query, including things like the {@link #maxRows limit} and
 * {@link #firstRow offset} and {@link #enabledFilterNames enabled filters}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryKey implements Serializable {
	/**
	 * todo (6.0) : integrate work from original 6.0 branch
	 */
	public interface ParameterBindingsMemento extends Serializable {
	}

	public static QueryKey from(
			String sqlQueryString,
			Limit limit,
			QueryParameterBindings parameterBindings,
			SharedSessionContractImplementor session) {
		// todo (6.0) : here is where we should centralize cacheable-or-not
		//		if this method returns null, the query should be considered un-cacheable
		//
		// todo (6.0) : should limited (first/max) results be cacheable?
		// todo (6.0) : should filtered results be cacheable?
		final Limit limitToUse = limit == null ? Limit.NONE : limit;
		return new QueryKey(
				sqlQueryString,
				parameterBindings.generateQueryKeyMemento( session ),
				limitToUse.getFirstRow(),
				limitToUse.getMaxRows(),
				session.getLoadQueryInfluencers().getEnabledFilterNames()
		);
	}

	private final String sqlQueryString;
	private final ParameterBindingsMemento parameterBindingsMemento;
	private final Integer firstRow;
	private final Integer maxRows;
	private final String[] enabledFilterNames;

	/**
	 * For performance reasons, the hashCode is cached; however, it is marked transient so that it can be
	 * recalculated as part of the serialization process which allows distributed query caches to work properly.
	 */
	private transient int hashCode;

	public QueryKey(
			String sqlQueryString,
			ParameterBindingsMemento parameterBindingsMemento,
			Integer firstRow,
			Integer maxRows,
			Set<String> enabledFilterNames) {
		this.sqlQueryString = sqlQueryString;
		this.parameterBindingsMemento = parameterBindingsMemento;
		this.firstRow = firstRow;
		this.maxRows = maxRows;
		this.enabledFilterNames = enabledFilterNames.toArray( String[]::new );
		this.hashCode = generateHashCode();
	}

	/**
	 * Deserialization hook used to re-init the cached hashcode which is needed for proper clustering support.
	 *
	 * @param in The object input stream.
	 *
	 * @throws IOException Thrown by normal deserialization
	 * @throws ClassNotFoundException Thrown by normal deserialization
	 */
	@Serial
	private void readObject(java.io.ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 13;
		result = 37 * result + sqlQueryString.hashCode();
		// Don't include the firstRow and maxRows in the hash
		// as these values are rarely useful for query caching
//		result = 37 * result + ( firstRow==null ? 0 : firstRow );
//		result = 37 * result + ( maxRows==null ? 0 : maxRows );
		result = 37 * result + parameterBindingsMemento.hashCode();
		result = 37 * result + Arrays.hashCode( enabledFilterNames );
		return result;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof QueryKey that
			&& Objects.equals( this.sqlQueryString, that.sqlQueryString )
			&& Objects.equals( this.firstRow, that.firstRow )
			&& Objects.equals( this.maxRows, that.maxRows )
			// Set's `#equals` impl does a deep check, so `Objects#equals` is a good check
			&& Objects.equals( this.parameterBindingsMemento, that.parameterBindingsMemento )
			&& Arrays.equals( this.enabledFilterNames, that.enabledFilterNames );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
