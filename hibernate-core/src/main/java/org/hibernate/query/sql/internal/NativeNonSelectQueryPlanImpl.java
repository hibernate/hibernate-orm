/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.StandardJdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.NativeJdbcMutation;

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
		executionContext.getSession().autoFlushIfRequired( affectedTableNames );
		BulkOperationCleanupAction.schedule( executionContext.getSession(), affectedTableNames );
		final List<JdbcParameterBinder> jdbcParameterBinders;
		final JdbcParameterBindings jdbcParameterBindings;

		final QueryParameterBindings queryParameterBindings = executionContext.getQueryParameterBindings();
		if ( parameterList == null || parameterList.isEmpty() ) {
			jdbcParameterBinders = Collections.emptyList();
			jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
		}
		else {
			jdbcParameterBinders = new ArrayList<>( parameterList.size() );
			jdbcParameterBindings = new JdbcParameterBindingsImpl( parameterList.size() );

			jdbcParameterBindings.registerNativeQueryParameters(
					queryParameterBindings,
					parameterList,
					jdbcParameterBinders,
					executionContext.getSession().getFactory()
			);
		}

		final JdbcMutation jdbcMutation = new NativeJdbcMutation(
				sql,
				jdbcParameterBinders,
				affectedTableNames
		);

		final JdbcMutationExecutor executor = StandardJdbcMutationExecutor.INSTANCE;

		final SharedSessionContractImplementor session = executionContext.getSession();
		// todo (6.0): use configurable executor instead?
//		final SessionFactoryImplementor factory = session.getFactory();
//		final JdbcServices jdbcServices = factory.getJdbcServices();
//		return jdbcServices.getJdbcMutationExecutor().execute(
		return executor.execute(
				jdbcMutation,
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext )
		);
	}
}
