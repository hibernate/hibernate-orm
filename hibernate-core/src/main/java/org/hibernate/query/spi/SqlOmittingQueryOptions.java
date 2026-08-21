/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * @apiNote This class is considered internal implementation
 * and will move to an internal package in a future version.
 * Application programs should never depend directly on this class.
 *
 * @author Christian Beikov
 */
@Internal // used by Hibernate Reactive
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

	public static QueryOptions omitSqlQueryOptions(QueryOptions originalOptions, JdbcSelect select) {
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
		return omitLocks ? new LockOptions() : super.getLockOptions();
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
