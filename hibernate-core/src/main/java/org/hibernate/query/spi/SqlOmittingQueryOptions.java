/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.LockOptions;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * @author Christian Beikov
 */
public class SqlOmittingQueryOptions extends DelegatingQueryOptions {

	private final boolean omitLimit;
	private final boolean omitLocks;
	private final ListResultsConsumer.UniqueSemantic uniqueSemantic;

	public SqlOmittingQueryOptions(QueryOptions queryOptions, boolean omitLimit, boolean omitLocks) {
		super( queryOptions );
		this.omitLimit = omitLimit;
		this.omitLocks = omitLocks;
		uniqueSemantic = null;
	}

	public SqlOmittingQueryOptions(QueryOptions queryOptions, boolean omitLimit, boolean omitLocks, ListResultsConsumer.UniqueSemantic semantic) {
		super( queryOptions );
		this.omitLimit = omitLimit;
		this.omitLocks = omitLocks;
		this.uniqueSemantic = semantic;
	}

	public static QueryOptions omitSqlQueryOptions(QueryOptions originalOptions) {
		return omitSqlQueryOptions( originalOptions, true, true );
	}

	public static QueryOptions omitSqlQueryOptions(QueryOptions originalOptions, JdbcOperationQuerySelect select) {
		return omitSqlQueryOptions( originalOptions, !select.usesLimitParameters(), false );
	}

	public static QueryOptions omitSqlQueryOptions(QueryOptions originalOptions, boolean omitLimit, boolean omitLocks) {
		final Limit limit = originalOptions.getLimit();

		// No need for a context when there are no options we use during SQL rendering
		if ( originalOptions.getLockOptions().isEmpty() ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return originalOptions;
			}
		}

		if ( !omitLocks ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return originalOptions;
			}
		}

		return new SqlOmittingQueryOptions( originalOptions, omitLimit, omitLocks );
	}

	public static QueryOptions omitSqlQueryOptionsWithUniqueSemanticFilter(QueryOptions originalOptions, boolean omitLimit, boolean omitLocks) {
		final Limit limit = originalOptions.getLimit();

		// No need for a context when there are no options we use during SQL rendering
		if ( originalOptions.getLockOptions().isEmpty() ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return originalOptions;
			}
		}

		if ( !omitLocks ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return originalOptions;
			}
		}

		return new SqlOmittingQueryOptions( originalOptions, omitLimit, omitLocks, ListResultsConsumer.UniqueSemantic.FILTER );
	}

	@Override
	public LockOptions getLockOptions() {
		return omitLocks ? LockOptions.NONE : super.getLockOptions();
	}

	@Override
	public Integer getFetchSize() {
		return null;
	}

	@Override
	public Limit getLimit() {
		return omitLimit ? Limit.NONE : super.getLimit();
	}

	@Override
	public Integer getFirstRow() {
		return omitLimit ? null : super.getFirstRow();
	}

	@Override
	public Integer getMaxRows() {
		return omitLimit ? null : super.getMaxRows();
	}

	@Override
	public Limit getEffectiveLimit() {
		return omitLimit ? Limit.NONE : super.getEffectiveLimit();
	}

	@Override
	public boolean hasLimit() {
		return !omitLimit && super.hasLimit();
	}

	@Override
	public ListResultsConsumer.UniqueSemantic getUniqueSemantic() {
		return uniqueSemantic;
	}
}
