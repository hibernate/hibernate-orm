/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * @author Steve Ebersole
 */
public class SimpleUpdateQueryPlan implements NonSelectQueryPlan {
	private final SqmUpdateStatement<?> sqmUpdate;
	private final DomainParameterXref domainParameterXref;

	private JdbcOperationQueryMutation jdbcUpdate;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParamMappingTypeResolutions;

	public SimpleUpdateQueryPlan(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref) {
		this.sqmUpdate = sqmUpdate;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmUpdate );
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		SqlAstTranslator<? extends JdbcOperationQueryMutation> updateTranslator = null;
		if ( jdbcUpdate == null ) {
			updateTranslator = createUpdateTranslator( executionContext );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmParamMappingTypeResolutions.get(parameter);
					}
				},
				session
		);

		if ( jdbcUpdate != null && !jdbcUpdate.isCompatibleWith(
				jdbcParameterBindings,
				executionContext.getQueryOptions()
		) ) {
			updateTranslator = createUpdateTranslator( executionContext );
		}

		if ( updateTranslator != null ) {
			jdbcUpdate = updateTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
		}

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcUpdate,
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext )
		);
	}

	private SqlAstTranslator<? extends JdbcOperationQueryMutation> createUpdateTranslator(DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final SqmTranslation<? extends MutationStatement> sqmInterpretation =
				factory.getQueryEngine().getSqmTranslatorFactory()
						.createMutationTranslator(
								sqmUpdate,
								executionContext.getQueryOptions(),
								domainParameterXref,
								executionContext.getQueryParameterBindings(),
								executionContext.getSession().getLoadQueryInfluencers(),
								factory.getSqlTranslationEngine()
						)
						.translate();

		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				sqmInterpretation::getJdbcParamsBySqmParam
		);

		this.sqmParamMappingTypeResolutions = sqmInterpretation.getSqmParameterMappingModelTypeResolutions();

		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, sqmInterpretation.getSqlAst() );
	}
}
