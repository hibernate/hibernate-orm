/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.Limit;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryPlanCache;

/**
 * @author Steve Ebersole
 */
public class SqmInterpretationsKey implements QueryPlanCache.Key {
	@SuppressWarnings("WeakerAccess")
	public static SqmInterpretationsKey generateFrom(QuerySqmImpl query) {
		if ( !isCacheable( query ) ) {
			return null;
		}

		return new SqmInterpretationsKey(
				query.getQueryString(),
				query.getResultType(),
				query.getQueryOptions()
		);
	}

	@SuppressWarnings("WeakerAccess")
	public static QueryPlanCache.Key generateNonSelectKey(QuerySqmImpl query) {
		// todo (6.0) : do we want to cache non-select plans?  If so, what requirements?
		// for now... no caching of non-select plans
		return null;
	}

	@SuppressWarnings("RedundantIfStatement")
	private static boolean isCacheable(QuerySqmImpl<?> query) {
		if ( query.getQueryOptions().getAppliedGraph() != null ) {
			// At the moment we cannot cache query plan if there is an
			// EntityGraph involved.
			return false;
		}

		if ( query.getParameterMetadata().hasAnyMatching( QueryParameter::allowsMultiValuedBinding ) ) {
			// cannot cache query plans if there is are multi-valued param bindings
			return false;
		}

		if ( hasLimit( query.getQueryOptions().getLimit() ) ) {
			// cannot cache query plans if there is a limit defined
			return false;
		}

		if ( definesLocking( query.getQueryOptions().getLockOptions() ) ) {
			// cannot cache query plans if it defines locking
			return false;
		}

		return true;
	}

	private static boolean hasLimit(Limit limit) {
		return limit.getFirstRow() != null || limit.getMaxRows() != null;
	}

	private static boolean definesLocking(LockOptions lockOptions) {
		final LockMode mostRestrictiveLockMode = lockOptions.findGreatestLockMode();
		return mostRestrictiveLockMode.greaterThan( LockMode.READ );
	}


	private final String query;
	private final Class resultType;
	private final TupleTransformer tupleTransformer;
	private final ResultListTransformer resultListTransformer;

	private SqmInterpretationsKey(
			String query,
			Class resultType,
			QueryOptions queryOptions) {
		this.query = query;
		this.resultType = resultType;
		this.tupleTransformer = queryOptions.getTupleTransformer();
		this.resultListTransformer = queryOptions.getResultListTransformer();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final SqmInterpretationsKey that = (SqmInterpretationsKey) o;
		return query.equals( that.query )
				&& areEqual( resultType, that.resultType )
				&& areEqual( tupleTransformer, that.tupleTransformer )
				&& areEqual( resultListTransformer, that.resultListTransformer );
	}

	private <T> boolean areEqual(T o1, T o2) {
		if ( o1 == null ) {
			return o2 == null;
		}
		else {
			return o1.equals( o2 );
		}
	}

	@Override
	public int hashCode() {
		int result = query.hashCode();
		result = 31 * result + ( resultType != null ? resultType.hashCode() : 0 );
		result = 31 * result + ( tupleTransformer != null ? tupleTransformer.hashCode() : 0 );
		result = 31 * result + ( resultListTransformer != null ? resultListTransformer.hashCode() : 0 );
		return result;
	}
}
