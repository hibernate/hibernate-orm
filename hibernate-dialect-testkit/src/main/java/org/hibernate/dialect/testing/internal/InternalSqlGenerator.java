/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.internal;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.testing.GeneratedParameter;
import org.hibernate.dialect.testing.GeneratedStatement;
import org.hibernate.dialect.testing.Pagination;
import org.hibernate.dialect.testing.SqlGenerationRequest;
import org.hibernate.dialect.testing.SqlGenerationResult;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.hql.spi.HqlTranslator;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.spi.SqmTranslation;
import org.hibernate.query.sqm.sql.spi.SqmTranslator;
import org.hibernate.query.sqm.sql.spi.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.spi.SqmTranslationRequest;
import org.hibernate.query.sqm.tree.spi.SqmDmlStatement;
import org.hibernate.query.sqm.tree.spi.SqmStatement;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.delete.DeleteStatement;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.insert.InsertStatement;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.update.UpdateStatement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.internal.JdbcOperationQueryDelete;
import org.hibernate.sql.exec.internal.JdbcOperationQueryUpdate;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

/// Drives Hibernate's normal HQL-to-SQL pipeline without executing the result.
///
/// @author Steve Ebersole
final class InternalSqlGenerator {
	private InternalSqlGenerator() {
	}

	@SuppressWarnings("rawtypes")
	static SqlGenerationResult translate(
			SqlGenerationRequest request,
			SessionFactoryImplementor sessionFactory) {
		if ( request.hql().matches( ".*\\?[0-9]+.*" ) ) {
			throw new IllegalArgumentException( "The Dialect test kit supports named HQL parameters only" );
		}

		final HqlTranslator hqlTranslator = sessionFactory.getQueryEngine().getHqlTranslator();
		final Class<?> resultType = request.expectedResultType() == null
				? Object[].class
				: request.expectedResultType();
		final SqmStatement<?> sqmAst = hqlTranslator.translate( request.hql(), resultType );

		if ( sqmAst instanceof SqmSelectStatement sqmSelect ) {
			return new SqmSelectInterpreter<>( request, sessionFactory ).interpret( sqmSelect );
		}
		if ( sqmAst instanceof SqmDeleteStatement sqmDelete ) {
			return new SqmDeleteInterpreter<>( request, sessionFactory ).interpret( sqmDelete );
		}
		if ( sqmAst instanceof SqmUpdateStatement sqmUpdate ) {
			return new SqmUpdateInterpreter<>( request, sessionFactory ).interpret( sqmUpdate );
		}
		if ( sqmAst instanceof SqmInsertStatement sqmInsert ) {
			return new SqmInsertInterpreter<>( request, sessionFactory ).interpret( sqmInsert );
		}

		throw new UnsupportedOperationException( "Unexpected SQM statement: " + sqmAst.getClass().getName() );
	}

	private abstract static class SqmInterpreter<
			R,
			T extends SqmStatement<R>,
			S extends Statement,
			J extends JdbcOperation> {
		protected final SqlGenerationRequest request;
		protected final SessionFactoryImplementor sessionFactory;
		protected final QueryOptions queryOptions;

		SqmInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			this.request = request;
			this.sessionFactory = sessionFactory;
			queryOptions = new TestQueryOptions( request.pagination(), request.lockMode() );
		}

		SqlGenerationResult interpret(T sqmAst) {
			final HqlInterpretation<R> hqlInterpretation = createHqlInterpretation( sqmAst );
			final QueryParameterBindingsImpl parameterBindings = QueryParameterBindingsImpl.from(
					hqlInterpretation.getParameterMetadata(),
					sessionFactory
			);
			for ( var entry : request.parameterValues().entrySet() ) {
				parameterBindings.getBinding( entry.getKey() ).setBindValue( entry.getValue() );
			}
			parameterBindings.validate();

			final SqmTranslator<S> sqmTranslator = createSqmTranslator( hqlInterpretation, parameterBindings );
			final SqmTranslation<S> sqmTranslation = sqmTranslator.translate();
			final SqlAstTranslator<J> sqlAstTranslator = createSqlAstTranslator( sqmTranslation );

			final J jdbcOperation = sessionFactory.fromSession( session -> {
				final JdbcParameterBindings jdbcParameterBindings = createJdbcParameterBindings(
						sqmTranslation,
						hqlInterpretation.getDomainParameterXref(),
						parameterBindings,
						session
				);
				return sqlAstTranslator.translate( jdbcParameterBindings, queryOptions );
			} );

			return new SqlGenerationResult(
					request.hql(),
					List.of( new GeneratedStatement(
							jdbcOperation.getSqlString(),
							generatedParameters(
									jdbcOperation,
									sqmTranslation,
									hqlInterpretation.getDomainParameterXref()
							)
					) )
			);
		}

		private HqlInterpretation<R> createHqlInterpretation(T sqmAst) {
			final ParameterMetadataImplementor parameterMetadata;
			final DomainParameterXref domainParameterXref;
			if ( sqmAst.getSqmParameters().isEmpty() ) {
				domainParameterXref = DomainParameterXref.EMPTY;
				parameterMetadata = ParameterMetadataImpl.EMPTY;
			}
			else {
				domainParameterXref = DomainParameterXref.from( sqmAst );
				parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
			}
			return new NonCopyingHqlInterpretation<>( sqmAst, parameterMetadata, domainParameterXref );
		}

		protected abstract SqmTranslator<S> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings);

		protected abstract SqlAstTranslator<J> createSqlAstTranslator(SqmTranslation<S> sqmTranslation);

		private JdbcParameterBindings createJdbcParameterBindings(
				SqmTranslation<S> sqmTranslation,
				DomainParameterXref domainParameterXref,
				QueryParameterBindingsImpl parameterBindings,
				Session session) {
			return SqmUtil.createJdbcParameterBindings(
					parameterBindings,
					domainParameterXref,
					SqmUtil.generateJdbcParamsXref(
							domainParameterXref,
							sqmTranslation::getJdbcParamsBySqmParam
					),
					new SqmParameterMappingModelResolutionAccess() {
						@Override
						public <X> MappingModelExpressible<X> getResolvedMappingModelType(SqmParameter<X> parameter) {
							final QueryParameterImplementor<?> domainParameter =
									domainParameterXref.getQueryParameter( parameter );
							final QueryParameterBinding<?> binding = parameterBindings.getBinding( domainParameter );
							@SuppressWarnings("unchecked")
							final MappingModelExpressible<X> type =
									(MappingModelExpressible<X>) binding.getType();
							return type;
						}
					},
					session.unwrap( SharedSessionContractImplementor.class )
			);
		}
	}

	private static final class SqmSelectInterpreter<R>
			extends SqmInterpreter<R, SqmSelectStatement<R>, SelectStatement, JdbcSelect> {
		SqmSelectInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			super( request, sessionFactory );
		}

		@Override
		protected SqmTranslator<SelectStatement> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			final SqmTranslatorFactory factory = sessionFactory.getQueryEngine().getSqmTranslatorFactory();
			return factory.createSelectTranslator( new SqmTranslationRequest.Select(
					(SqmSelectStatement<R>) hqlInterpretation.getSqmStatement(),
					queryOptions,
					hqlInterpretation.getDomainParameterXref(),
					parameterBindings,
					new LoadQueryInfluencers( sessionFactory ),
					sessionFactory.getSqlTranslationEngine(),
					true
			) );
		}

		@Override
		protected SqlAstTranslator<JdbcSelect> createSqlAstTranslator(SqmTranslation<SelectStatement> translation) {
			return translatorFactory().buildTranslator(
					new SqlAstTranslationRequest.Select( sessionFactory, translation.getSqlAst() )
			);
		}

		private SqlAstTranslatorFactory translatorFactory() {
			return sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		}
	}

	private abstract static class SqmMutationInterpreter<
			R,
			T extends SqmStatement<R>,
			S extends Statement,
			J extends JdbcOperation>
			extends SqmInterpreter<R, T, S, J> {
		SqmMutationInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			super( request, sessionFactory );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected SqmTranslator<S> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			return (SqmTranslator<S>) sessionFactory.getQueryEngine().getSqmTranslatorFactory()
					.createMutationTranslator( new SqmTranslationRequest.Mutation(
							(SqmDmlStatement<?>) hqlInterpretation.getSqmStatement(),
							queryOptions,
							hqlInterpretation.getDomainParameterXref(),
							parameterBindings,
							new LoadQueryInfluencers( sessionFactory ),
							sessionFactory.getSqlTranslationEngine()
					) );
		}

		SqlAstTranslatorFactory translatorFactory() {
			return sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		}
	}

	private static final class SqmDeleteInterpreter<R>
			extends SqmMutationInterpreter<R, SqmDeleteStatement<R>, DeleteStatement, JdbcOperationQueryDelete> {
		SqmDeleteInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			super( request, sessionFactory );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected SqlAstTranslator<JdbcOperationQueryDelete> createSqlAstTranslator(
				SqmTranslation<DeleteStatement> translation) {
			return (SqlAstTranslator<JdbcOperationQueryDelete>) (SqlAstTranslator<?>) translatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.QueryMutation(
							sessionFactory,
							translation.getSqlAst()
					) );
		}
	}

	private static final class SqmUpdateInterpreter<R>
			extends SqmMutationInterpreter<R, SqmUpdateStatement<R>, UpdateStatement, JdbcOperationQueryUpdate> {
		SqmUpdateInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			super( request, sessionFactory );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected SqlAstTranslator<JdbcOperationQueryUpdate> createSqlAstTranslator(
				SqmTranslation<UpdateStatement> translation) {
			return (SqlAstTranslator<JdbcOperationQueryUpdate>) (SqlAstTranslator<?>) translatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.QueryMutation(
							sessionFactory,
							translation.getSqlAst()
					) );
		}
	}

	private static final class SqmInsertInterpreter<R>
			extends SqmMutationInterpreter<R, SqmInsertStatement<R>, InsertStatement, JdbcOperationQueryInsert> {
		SqmInsertInterpreter(SqlGenerationRequest request, SessionFactoryImplementor sessionFactory) {
			super( request, sessionFactory );
		}

		@Override
		@SuppressWarnings("unchecked")
		protected SqlAstTranslator<JdbcOperationQueryInsert> createSqlAstTranslator(
				SqmTranslation<InsertStatement> translation) {
			return (SqlAstTranslator<JdbcOperationQueryInsert>) (SqlAstTranslator<?>) translatorFactory()
					.buildTranslator( new SqlAstTranslationRequest.QueryMutation(
							sessionFactory,
							translation.getSqlAst()
					) );
		}
	}

	private record NonCopyingHqlInterpretation<R>(
			SqmStatement<R> sqmStatement,
			ParameterMetadataImplementor parameterMetadata,
			DomainParameterXref domainParameterXref) implements HqlInterpretation<R> {
		@Override
		public SqmStatement<R> getSqmStatement() {
			return sqmStatement;
		}

		@Override
		public ParameterMetadataImplementor getParameterMetadata() {
			return parameterMetadata;
		}

		@Override
		public DomainParameterXref getDomainParameterXref() {
			return domainParameterXref;
		}

		@Override
		public void validateResultType(Class<?> resultType) {
		}
	}

	private static final class TestQueryOptions extends QueryOptionsAdapter {
		private final Limit limit;
		private final LockOptions lockOptions;

		TestQueryOptions(Pagination pagination, LockMode lockMode) {
			limit = pagination == null
					? Limit.NONE
					: new Limit( pagination.firstResult(), pagination.maxResults() );
			lockOptions = new LockOptions( lockMode == null ? LockMode.NONE : lockMode );
		}

		@Override
		public Limit getLimit() {
			return limit;
		}

		@Override
		public LockOptions getLockOptions() {
			return lockOptions;
		}
	}

	private static List<GeneratedParameter> generatedParameters(
			JdbcOperation operation,
			SqmTranslation<?> translation,
			DomainParameterXref domainParameterXref) {
		final Map<JdbcParameterBinder, String> names = new IdentityHashMap<>();
		for ( var entry : translation.getJdbcParamsBySqmParam().entrySet() ) {
			final String name = domainParameterXref.getQueryParameter( entry.getKey() ).getName();
			for ( List<JdbcParameter> expansion : entry.getValue() ) {
				for ( JdbcParameter parameter : expansion ) {
					names.put( parameter.getParameterBinder(), name );
				}
			}
		}

		final List<GeneratedParameter> result = new ArrayList<>();
		int position = 1;
		for ( JdbcParameterBinder binder : operation.getParameterBinders() ) {
			result.add( new GeneratedParameter( position++, names.get( binder ) ) );
		}
		return result;
	}
}
