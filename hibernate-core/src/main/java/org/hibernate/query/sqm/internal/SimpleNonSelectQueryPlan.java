/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import java.util.List;
import java.util.Map;

import static org.hibernate.query.sqm.internal.SqmUtil.generateJdbcParamsXref;


/**
 * @since 7.1
 */
public class SimpleNonSelectQueryPlan implements NonSelectQueryPlan {
	private final SqmDmlStatement<?> statement;
	private final DomainParameterXref domainParameterXref;

	private volatile CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> interpretation;

	public SimpleNonSelectQueryPlan(SqmDmlStatement<?> statement, DomainParameterXref domainParameterXref) {
		this.statement = statement;
		this.domainParameterXref = domainParameterXref;
	}

	// For Hibernate Reactive
	protected SqmDmlStatement<?> getStatement() {
		return statement;
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext context) {
		BulkOperationCleanupAction.schedule( context.getSession(), statement );
		final Interpretation interpretation = getInterpretation( context );
		final ExecutionContext executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		return execute( interpretation.interpretation, interpretation.jdbcParameterBindings, executionContext );
	}

	protected int execute(CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> sqmInterpretation, JdbcParameterBindings jdbcParameterBindings, ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		return session.getFactory().getJdbcServices().getJdbcMutationExecutor().execute(
				sqmInterpretation.jdbcOperation(),
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	private JdbcParameterBindings createJdbcParameterBindings(CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> sqmInterpretation, DomainQueryExecutionContext executionContext) {
		return SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				sqmInterpretation.jdbcParamsXref(),
				new SqmParameterMappingModelResolutionAccess() {
					//this is pretty ugly!
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmInterpretation.sqmParameterMappingModelTypes().get(parameter);
					}
				},
				executionContext.getSession()
		);
	}

	protected SqmTranslation<? extends MutationStatement> buildTranslation(
			SqmDmlStatement<?> sqm,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		return sessionFactory.getQueryEngine().getSqmTranslatorFactory()
						.createMutationTranslator(
								sqm,
								executionContext.getQueryOptions(),
								domainParameterXref,
								executionContext.getQueryParameterBindings(),
								executionContext.getSession().getLoadQueryInfluencers(),
								sessionFactory.getSqlTranslationEngine()
						)
						.translate();
	}

	// For Hibernate Reactive
	protected Interpretation getInterpretation(DomainQueryExecutionContext context) {
		Interpretation builtInterpretation = null;
		CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> localCopy = interpretation;

		if ( localCopy == null ) {
			synchronized ( this ) {
				localCopy = interpretation;
				if ( localCopy == null ) {
					builtInterpretation = buildInterpretation( statement, domainParameterXref, context );
					localCopy = builtInterpretation.interpretation;
					interpretation = builtInterpretation.interpretation;
				}
				else {
					builtInterpretation = updateInterpretation( localCopy, context );
				}
			}
		}
		else {
			builtInterpretation = updateInterpretation( localCopy, context );
		}

		return builtInterpretation != null ? builtInterpretation
				: new Interpretation( localCopy, createJdbcParameterBindings( localCopy, context ) );
	}

	private @Nullable Interpretation updateInterpretation(
			CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> localCopy,
			DomainQueryExecutionContext context) {
		Interpretation builtInterpretation = null;
		if ( localCopy.jdbcOperation().dependsOnParameterBindings() ) {
			final JdbcParameterBindings jdbcParameterBindings = createJdbcParameterBindings( localCopy, context );
			// If the translation depends on the limit or lock options, we have to rebuild the JdbcSelect
			// We could avoid this by putting the lock options into the cache key
			if ( !localCopy.jdbcOperation().isCompatibleWith( jdbcParameterBindings, context.getQueryOptions() ) ) {
				builtInterpretation = buildInterpretation(
						localCopy.statement(),
						localCopy.jdbcParamsXref(),
						localCopy.sqmParameterMappingModelTypes(),
						jdbcParameterBindings,
						context
				);
				interpretation = builtInterpretation.interpretation;
			}
			else {
				builtInterpretation = new Interpretation( localCopy, jdbcParameterBindings );
			}
		}
		return builtInterpretation;
	}

	// For Hibernate Reactive
	protected record Interpretation(
			CacheableSqmInterpretation<MutationStatement, JdbcOperationQueryMutation> interpretation,
			JdbcParameterBindings jdbcParameterBindings
	) {}

	protected Interpretation buildInterpretation(
			SqmDmlStatement<?> sqm,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();

		final SqmTranslation<? extends MutationStatement> sqmInterpretation =
				buildTranslation( sqm, domainParameterXref, executionContext );
		final var jdbcParamsXref =
				generateJdbcParamsXref( domainParameterXref, sqmInterpretation::getJdbcParamsBySqmParam );
		final Map<SqmParameter<?>, MappingModelExpressible<?>> parameterModelTypeResolutions =
				sqmInterpretation.getSqmParameterMappingModelTypeResolutions();

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>)
								parameterModelTypeResolutions.get( parameter );
					}
				},
				session
		);
		return buildInterpretation(
				sqmInterpretation.getSqlAst(),
				jdbcParamsXref,
				parameterModelTypeResolutions,
				jdbcParameterBindings,
				executionContext
		);
	}

	protected Interpretation buildInterpretation(
			MutationStatement mutationStatement,
			Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref,
			Map<SqmParameter<?>, MappingModelExpressible<?>> parameterModelTypeResolutions,
			JdbcParameterBindings jdbcParameterBindings,
			DomainQueryExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final SqlAstTranslator<? extends JdbcOperationQueryMutation> mutationTranslator =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildMutationTranslator( sessionFactory, mutationStatement );
		return new Interpretation(
				new CacheableSqmInterpretation<>(
						mutationStatement,
						mutationTranslator.translate( jdbcParameterBindings, executionContext.getQueryOptions() ),
						jdbcParamsXref,
						parameterModelTypeResolutions
				),
				jdbcParameterBindings
		);
	}

}
