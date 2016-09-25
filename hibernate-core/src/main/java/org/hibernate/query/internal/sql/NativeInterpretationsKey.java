/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal.sql;

import java.util.Collection;
import java.util.List;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.sql.sqm.exec.spi.Limit;

/**
 * @author Steve Ebersole
 */
public class NativeInterpretationsKey implements QueryInterpretations.Key {
	public static NativeInterpretationsKey generateFrom(NativeQueryImpl<?> nativeQuery) {
		if ( !isCacheable( nativeQuery ) ) {
			return null;
		}

		return new NativeInterpretationsKey(
				nativeQuery.getQueryString(),
				nativeQuery.isCallable(),
				nativeQuery.getQueryReturns(),
				nativeQuery.getSynchronizedQuerySpaces(),
				nativeQuery.getQueryOptions().getTupleTransformer(),
				nativeQuery.getQueryOptions().getResultListTransformer()
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private static boolean isCacheable(NativeQueryImpl query) {
		if ( hasLimit( query.getQueryOptions().getLimit() ) ) {
			return false;
		}

		return true;
	}

	private static boolean hasLimit(Limit limit) {
		return limit.getFirstRow() != null || limit.getMaxRows() != null;
	}


	private final String sql;
	private final boolean callable;
	private final List<NativeSQLQueryReturn> queryReturns;
	private final Collection<String> querySpaces;

	// todo : Returns?

	private final TupleTransformer tupleTransformer;
	private final ResultListTransformer resultListTransformer;

	/**
	 * NativeInterpretationsKey ctor.  Defined as public solely for use in
	 * tests; prefer using {@link #generateFrom(NativeQueryImpl)} instead.
	 */
	public NativeInterpretationsKey(
			String sql,
			boolean callable,
			List<NativeSQLQueryReturn> queryReturns,
			Collection<String> querySpaces,
			TupleTransformer tupleTransformer,
			ResultListTransformer resultListTransformer) {
		this.sql = sql;
		this.callable = callable;
		this.queryReturns = queryReturns;
		this.querySpaces = querySpaces;
		this.tupleTransformer = tupleTransformer;
		this.resultListTransformer = resultListTransformer;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		NativeInterpretationsKey that = (NativeInterpretationsKey) o;

		return callable == that.callable
				&& sql.equals( that.sql )
				&& querySpaces.equals( that.querySpaces )
				&& EqualsHelper.areEqual( queryReturns, that.queryReturns )
				&& EqualsHelper.areEqual( tupleTransformer, that.tupleTransformer )
				&& EqualsHelper.areEqual( resultListTransformer, that.resultListTransformer );

	}

	@Override
	public int hashCode() {
		int result = sql.hashCode();
		result = 31 * result + ( callable ? 1 : 0 );
		result = 31 * result + querySpaces.hashCode();
		result = 31 * result + ( tupleTransformer != null ? tupleTransformer.hashCode() : 0 );
		result = 31 * result + ( resultListTransformer != null ? resultListTransformer.hashCode() : 0 );
		return result;
	}
}
