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
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * @author Gavin King
 */
public class SimpleInsertQueryPlan implements NonSelectQueryPlan {
	private final SqmInsertStatement<?> sqmInsert;
	private final DomainParameterXref domainParameterXref;
	private Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions;

	private JdbcOperationQueryMutation jdbcInsert;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;

	public SimpleInsertQueryPlan(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref) {
		this.sqmInsert = sqmInsert;
		this.domainParameterXref = domainParameterXref;
	}

	private SqlAstTranslator<? extends JdbcOperationQueryMutation> createInsertTranslator(DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final SqmTranslation<? extends MutationStatement> sqmInterpretation =
				factory.getQueryEngine().getSqmTranslatorFactory()
						.createMutationTranslator(
								sqmInsert,
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

		this.paramTypeResolutions = sqmInterpretation.getSqmParameterMappingModelTypeResolutions();

		return factory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, sqmInterpretation.getSqlAst() );
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmInsert );
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		SqlAstTranslator<? extends JdbcOperationQueryMutation> insertTranslator = null;
		if ( jdbcInsert == null ) {
			insertTranslator = createInsertTranslator( executionContext );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) paramTypeResolutions.get(parameter);
					}
				},
				session
		);

		if ( jdbcInsert != null && !jdbcInsert.isCompatibleWith(
				jdbcParameterBindings,
				executionContext.getQueryOptions()
		) ) {
			insertTranslator = createInsertTranslator( executionContext );
		}

		if ( insertTranslator != null ) {
			jdbcInsert = insertTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
		}

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext )
		);
	}
}
