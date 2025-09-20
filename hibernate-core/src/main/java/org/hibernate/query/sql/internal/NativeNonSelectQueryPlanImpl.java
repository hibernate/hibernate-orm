/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQueryMutationNative;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class NativeNonSelectQueryPlanImpl implements NonSelectQueryPlan {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<ParameterOccurrence> parameterList;

	public NativeNonSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<ParameterOccurrence> parameterList) {
		this.sql = sql;
		this.affectedTableNames = affectedTableNames;
		this.parameterList = parameterList;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		session.autoFlushIfRequired( affectedTableNames );
		BulkOperationCleanupAction.schedule( session, affectedTableNames );
		final List<JdbcParameterBinder> jdbcParameterBinders;
		final JdbcParameterBindings jdbcParameterBindings;
		if ( parameterList == null || parameterList.isEmpty() ) {
			jdbcParameterBinders = emptyList();
			jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
		}
		else {
			jdbcParameterBinders = new ArrayList<>( parameterList.size() );
			jdbcParameterBindings = new JdbcParameterBindingsImpl(
					executionContext.getQueryParameterBindings(),
					parameterList,
					jdbcParameterBinders,
					session.getFactory()
			);
		}

		final String processedSql = new SQLQueryParser( sql, null, session.getSessionFactory() ).process();
		return session.getJdbcServices().getJdbcMutationExecutor().execute(
				new JdbcOperationQueryMutationNative( processedSql, jdbcParameterBinders, affectedTableNames ),
				jdbcParameterBindings,
				sql -> session.getJdbcCoordinator().getStatementPreparer().prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext )
		);
	}
}
