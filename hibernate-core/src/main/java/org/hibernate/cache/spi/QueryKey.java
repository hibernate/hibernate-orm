/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryParameterBindings;

/**
 * A key that identifies a particular query with bound parameter values.  This is the object Hibernate uses
 * as its key into its query cache.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryKey implements Serializable {
	/**
	 * todo (6.0) : integrate work from original 6.0 branch
	 */
	public interface ParameterBindingsMemento {
	}

	public static QueryKey from(
			String sqlQueryString,
			Limit limit,
			QueryParameterBindings parameterBindings,
			SharedSessionContractImplementor persistenceContext) {
		// todo (6.0) : here is where we should centralize cacheable-or-not
		//		if this method returns null, the query should be considered un-cacheable
		//
		// todo (6.0) : should limited (first/max) results be cacheable?
		// todo (6.0) : should filtered results be cacheable?

		final Limit limitToUse = limit == null ? Limit.NONE : limit;

		return new QueryKey(
				sqlQueryString,
				parameterBindings.generateQueryKeyMemento( persistenceContext ),
				limitToUse.getFirstRow(),
				limitToUse.getMaxRows(),
				persistenceContext.getTenantIdentifier(),
				persistenceContext.getLoadQueryInfluencers().getEnabledFilterNames()
		);
	}


	private final String sqlQueryString;
	private final ParameterBindingsMemento parameterBindingsMemento;
	private final Integer firstRow;
	private final Integer maxRows;
	private final String tenantIdentifier;
	private final Set<String> enabledFilterNames;

	/**
	 * For performance reasons, the hashCode is cached; however, it is marked transient so that it can be
	 * recalculated as part of the serialization process which allows distributed query caches to work properly.
	 */
	private transient int hashCode;

	public QueryKey(
			String sql,
			ParameterBindingsMemento parameterBindingsMemento,
			Integer firstRow,
			Integer maxRows,
			String tenantIdentifier,
			Set<String> enabledFilterNames) {
		this.sqlQueryString = sql;
		this.parameterBindingsMemento = parameterBindingsMemento;
		this.firstRow = firstRow;
		this.maxRows = maxRows;
		this.tenantIdentifier = tenantIdentifier;
		this.enabledFilterNames = enabledFilterNames;
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
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.hashCode = generateHashCode();
	}

	private int generateHashCode() {
		int result = 13;
		result = 37 * result + sqlQueryString.hashCode();
		result = 37 * result + ( firstRow==null ? 0 : firstRow );
		result = 37 * result + ( maxRows==null ? 0 : maxRows );
		result = 37 * result + ( tenantIdentifier==null ? 0 : tenantIdentifier.hashCode() );
		// the collections are too complicated to incorporate into the hashcode.  but they really
		// aren't needed in the hashcode calculation - they are handled in `#equals` and the calculation
		// without them is a good hashing code.
		//
		// todo (6.0) : maybe even just base it on `sqlQueryString`?

		return result;
	}

	@Override
	@SuppressWarnings({"RedundantIfStatement", "EqualsWhichDoesntCheckParameterClass"})
	public boolean equals(Object other) {
		final QueryKey that;

		if ( other instanceof QueryKey ) { //instanceof does a null check, too
			that = (QueryKey) other;
		}
		else {
			return false;
		}

		if ( ! Objects.equals( sqlQueryString, that.sqlQueryString ) ) {
			return false;
		}

		if ( ! Objects.equals( tenantIdentifier, that.tenantIdentifier ) ) {
			return false;
		}

		if ( ! Objects.equals( firstRow, that.firstRow )
				|| ! Objects.equals( maxRows, that.maxRows ) ) {
			return false;
		}

		// Set's `#equals` impl does a deep check, so `Objects#equals` is a good check
		if ( ! Objects.equals( parameterBindingsMemento, that.parameterBindingsMemento ) ) {
			return false;
		}

		if ( ! Objects.equals( enabledFilterNames, that.enabledFilterNames ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
