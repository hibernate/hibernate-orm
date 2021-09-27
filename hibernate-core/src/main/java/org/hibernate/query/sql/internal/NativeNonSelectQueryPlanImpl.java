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

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.SqlOmittingQueryOptions;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.StandardJdbcMutationExecutor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.NativeJdbcMutation;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class NativeNonSelectQueryPlanImpl implements NonSelectQueryPlan {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<QueryParameterImplementor<?>> parameterList;

	public NativeNonSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<QueryParameterImplementor<?>> parameterList) {
		this.sql = sql;
		this.affectedTableNames = affectedTableNames;
		this.parameterList = parameterList;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		executionContext.getSession().autoFlushIfRequired( affectedTableNames );
		BulkOperationCleanupAction.schedule( executionContext, affectedTableNames );
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

			for ( QueryParameterImplementor<?> param : parameterList ) {
				QueryParameterBinding<?> binding = queryParameterBindings.getBinding( param );
				AllowableParameterType<?> type = binding.getBindType();
				if ( type == null ) {
					type = param.getHibernateType();
				}
				if ( type == null ) {
					type = StandardBasicTypes.OBJECT_TYPE;
				}

				final JdbcMapping jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
				final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );

				jdbcParameterBinders.add( jdbcParameter );

				jdbcParameterBindings.addBinding(
						jdbcParameter,
						new JdbcParameterBindingImpl( jdbcMapping, binding.getBindValue() )
				);
			}
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
				executionContext
		);
	}
}
