/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sqm;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sqm.query.SqmStatement;

/**
 * @author Steve Ebersole
 */
public class SqmInterpretationsKey implements QueryInterpretations.Key {
	public static SqmInterpretationsKey generateFrom(QuerySqmImpl query) {
		if ( !isCacheable( query ) ) {
			return null;
		}

		return new SqmInterpretationsKey(
				query.getSqmStatement(),
				query.getResultType(),
				query.getQueryOptions()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private static boolean isCacheable(QuerySqmImpl query) {
		if ( query.getEntityGraphHint() != null ) {
			return false;
		}

		if ( hasMultiValuedParameters( query.getParameterMetadata() ) ) {
			return false;
		}

		if ( hasLimit( query.getQueryOptions().getLimit() ) ) {
			return false;
		}

		if ( definesLocking( query.getQueryOptions().getLockOptions() ) ) {
			return false;
		}

		return true;
	}

	private static boolean hasMultiValuedParameters(ParameterMetadata parameterMetadata) {
		for ( QueryParameter<?> queryParameter : parameterMetadata.collectAllParameters() ) {
			if ( queryParameter.allowsMultiValuedBinding() ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLimit(Limit limit) {
		return limit.getFirstRow() != null || limit.getMaxRows() != null;
	}

	private static boolean definesLocking(LockOptions lockOptions) {
		final LockMode mostRestrictiveLockMode = lockOptions.findGreatestLockMode();
		return mostRestrictiveLockMode.greaterThan( LockMode.READ );
	}


	private final SqmStatement sqmStatement;
	private final Class resultType;
	private final TupleTransformer tupleTransformer;
	private final ResultListTransformer resultListTransformer;

	private SqmInterpretationsKey(
			SqmStatement sqmStatement,
			Class resultType,
			QueryOptions queryOptions) {
		this.sqmStatement = sqmStatement;
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
		return sqmStatement.equals( that.sqmStatement )
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
		int result = sqmStatement.hashCode();
		result = 31 * result + ( resultType != null ? resultType.hashCode() : 0 );
		result = 31 * result + ( tupleTransformer != null ? tupleTransformer.hashCode() : 0 );
		result = 31 * result + ( resultListTransformer != null ? resultListTransformer.hashCode() : 0 );
		return result;
	}
}
