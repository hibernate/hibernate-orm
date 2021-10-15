/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;

import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;

/**
 * @author Steve Ebersole
 */
public class SqmJdbcExecutionContextAdapter implements ExecutionContext {
	private final DomainQueryExecutionContext sqmExecutionContext;
	private final QueryOptions queryOptions;

	public SqmJdbcExecutionContextAdapter(DomainQueryExecutionContext sqmExecutionContext) {
		this( sqmExecutionContext, omitSqlQueryOptions( sqmExecutionContext.getQueryOptions() ) );
	}

	public SqmJdbcExecutionContextAdapter(DomainQueryExecutionContext sqmExecutionContext, JdbcSelect jdbcSelect) {
		this( sqmExecutionContext, omitSqlQueryOptions( sqmExecutionContext.getQueryOptions(), jdbcSelect ) );
	}

	public SqmJdbcExecutionContextAdapter(DomainQueryExecutionContext sqmExecutionContext, QueryOptions queryOptions) {
		this.sqmExecutionContext = sqmExecutionContext;
		this.queryOptions = queryOptions;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return sqmExecutionContext.getSession();
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
}
