/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.LockOptions;
import org.hibernate.query.Limit;
import org.hibernate.sql.exec.internal.DelegatingExecutionContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Christian Beikov
 */
public class SqlOmittingQueryOptions extends DelegatingQueryOptions {

	private final boolean omitLimit;
	private final boolean omitLocks;

	public SqlOmittingQueryOptions(QueryOptions queryOptions, boolean omitLimit, boolean omitLocks) {
		super( queryOptions );
		this.omitLimit = omitLimit;
		this.omitLocks = omitLocks;
	}

	public static ExecutionContext omitSqlQueryOptions(ExecutionContext context) {
		return omitSqlQueryOptions( context, true, true );
	}

	public static ExecutionContext omitSqlQueryOptions(ExecutionContext context, JdbcSelect select) {
		return omitSqlQueryOptions( context, !select.usesLimitParameters(), false );
	}

	public static ExecutionContext omitSqlQueryOptions(ExecutionContext context, boolean omitLimit, boolean omitLocks) {
		final QueryOptions originalQueryOptions = context.getQueryOptions();
		final Limit limit = originalQueryOptions.getLimit();
		// No need for a context when there are no options we use during SQL rendering
		if ( originalQueryOptions.getLockOptions().isEmpty() ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return context;
			}
		}
		else if ( !omitLocks ) {
			if ( !omitLimit || limit == null || limit.isEmpty() ) {
				return context;
			}
		}
		final QueryOptions queryOptions = new SqlOmittingQueryOptions( originalQueryOptions, omitLimit, omitLocks );
		return new DelegatingExecutionContext( context ) {
			@Override
			public QueryOptions getQueryOptions() {
				return queryOptions;
			}
		};
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
		return omitLimit ? false : super.hasLimit();
	}
}
