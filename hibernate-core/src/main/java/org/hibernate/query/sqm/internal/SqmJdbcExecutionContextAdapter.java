/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcSelect;

import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;

/**
 * ExecutionContext adapter delegating to a DomainQueryExecutionContext
 */
public class SqmJdbcExecutionContextAdapter extends BaseExecutionContext {
	/**
	 * Creates an adapter which drops any locking or paging details from the query options
	 */
	public static SqmJdbcExecutionContextAdapter omittingLockingAndPaging(DomainQueryExecutionContext sqmExecutionContext) {
		return new SqmJdbcExecutionContextAdapter( sqmExecutionContext );
	}

	/**
	 * Creates an adapter which honors any locking or paging details specified in the query options
	 */
	public static SqmJdbcExecutionContextAdapter usingLockingAndPaging(DomainQueryExecutionContext sqmExecutionContext) {
		return new SqmJdbcExecutionContextAdapter( sqmExecutionContext, sqmExecutionContext.getQueryOptions() );
	}

	private final DomainQueryExecutionContext sqmExecutionContext;
	private final QueryOptions queryOptions;

	private SqmJdbcExecutionContextAdapter(DomainQueryExecutionContext sqmExecutionContext) {
		this( sqmExecutionContext, omitSqlQueryOptions( sqmExecutionContext.getQueryOptions() ) );
	}

	private SqmJdbcExecutionContextAdapter(DomainQueryExecutionContext sqmExecutionContext, QueryOptions queryOptions) {
		super( sqmExecutionContext.getSession() );
		this.sqmExecutionContext = sqmExecutionContext;
		this.queryOptions = queryOptions;
	}

	public SqmJdbcExecutionContextAdapter(
			DomainQueryExecutionContext sqmExecutionContext,
			JdbcSelect jdbcSelect) {
		this( sqmExecutionContext, omitSqlQueryOptions( sqmExecutionContext.getQueryOptions(), jdbcSelect ) );
	}

	@Override
	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return sqmExecutionContext.getQueryParameterBindings();
	}

	@Override
	public Callback getCallback() {
		return sqmExecutionContext.getCallback();
	}

	@Override
	public boolean hasCallbackActions() {
		return sqmExecutionContext.hasCallbackActions();
	}

	@Override
	public boolean hasQueryExecutionToBeAddedToStatistics() {
		return true;
	}

	@Override
	public boolean upgradeLocks() {
		return true;
	}
}
