/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.util.ast;

import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQueryDelete;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.internal.JdbcOperationQueryUpdate;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/// Utilities for helping test HQL translation
///
/// @author Steve Ebersole
public class HqlHelper {

	/// Translation details about a particular HQL
	///
	/// @param hql The translated HQL
	/// @param sqm The corresponding SQM AST
	/// @param sql The corresponding SQL
	/// @param sqlAst The corresponding SQL AST
	/// @param parameterMetadata Details about any query parameters
	public record HqlTranslation(
			String hql,
			SqmStatement<?> sqm,
			String sql,
			Statement sqlAst,
			ParameterMetadata parameterMetadata) {
	}

	/// Performs the translation, returning the details.  Delegates to {@linkplain #translateHql(String, Class, SessionFactoryImplementor)}
	/// passing {@code Object[]} as the expected result type.
	public static HqlTranslation translateHql(String hql, SessionFactoryImplementor sessionFactory) {
		return translateHql( hql, Object[].class,  sessionFactory );
	}

	/// Performs the translation, returning the details.
	@SuppressWarnings("rawtypes")
	public static HqlTranslation translateHql(String hql, Class<?> resultType, SessionFactoryImplementor sessionFactory) {
		final HqlTranslator hqlTranslator = sessionFactory.getQueryEngine().getHqlTranslator();
		final SqmStatement<?> sqmAst = hqlTranslator.translate( hql, resultType );

		if ( sqmAst instanceof SqmSelectStatement sqmSelect ) {
			//noinspection unchecked
			return new SqmSelectInterpreter<>( hql, sessionFactory ).interpret( sqmSelect, sessionFactory );
		}
		else if ( sqmAst instanceof SqmDeleteStatement sqmDelete ) {
			//noinspection unchecked
			return new SqmDeleteInterpreter<>( hql, sessionFactory ).interpret( sqmDelete, sessionFactory );
		}
		else if ( sqmAst instanceof SqmUpdateStatement sqmUpdate ) {
			//noinspection unchecked
			return new SqmUpdateInterpreter<>( hql, sessionFactory ).interpret( sqmUpdate, sessionFactory );
		}
		else if ( sqmAst instanceof SqmInsertStatement sqmInsert ) {
			//noinspection unchecked
			return new SqmInsertInterpreter<>( hql, sessionFactory ).interpret( sqmInsert, sessionFactory );
		}

		throw new UnsupportedOperationException( "Unexpected SQM type from HQL - " + sqmAst.getClass().getName() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// internals

	private static abstract class SqmInterpreter<R, T extends SqmStatement<R>, S extends Statement, J extends JdbcOperation> {
		protected final String hql;
		protected final SessionFactoryImplementor sessionFactory;

		public SqmInterpreter(String hql, SessionFactoryImplementor sessionFactory) {
			this.hql = hql;
			this.sessionFactory = sessionFactory;
		}

		public HqlTranslation interpret(T sqmAst, SessionFactoryImplementor sessionFactory) {
			final HqlInterpretation<R> hqlInterpretation = createHqlInterpretation( sqmAst );

			final QueryParameterBindingsImpl domainParameterBindings = QueryParameterBindingsImpl.from(
					hqlInterpretation.getParameterMetadata(),
					sessionFactory
			);

			final SqmTranslator<S> sqmTranslator = createSqmTranslator( hqlInterpretation, domainParameterBindings );
			final SqmTranslation<S> sqmTranslation = sqmTranslator.translate();

			final SqlAstTranslator<J> sqlAstTranslator = createSqlAstTranslator( sqmTranslation );

			final J jdbcOperation = sessionFactory.fromSession( (session) -> {
				final JdbcParameterBindings jdbcParameterBindings = createJdbcParameterBindings(
						sqmTranslation,
						hqlInterpretation.getDomainParameterXref(),
						domainParameterBindings,
						session
				);
				return sqlAstTranslator.translate( jdbcParameterBindings, QueryOptions.NONE );
			} );

			return new HqlTranslation(
					hql,
					hqlInterpretation.getSqmStatement(),
					jdbcOperation.getSqlString(),
					sqmTranslation.getSqlAst(),
					hqlInterpretation.getParameterMetadata()
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

			return new NonCopyingHqlInterpretationImpl<>( sqmAst, parameterMetadata, domainParameterXref );
		}

		protected abstract SqmTranslator<S> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings);

		protected abstract SqlAstTranslator<J> createSqlAstTranslator(
				SqmTranslation<S> sqmTranslation);

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
						public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
							final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( parameter );
							final QueryParameterBinding<?> binding = parameterBindings.getBinding( domainParam );
							//noinspection unchecked
							return (MappingModelExpressible<T>) binding.getType();
						}
					},
					session.unwrap( SharedSessionContractImplementor.class )
			);
		}
	}

	private static class SqmSelectInterpreter<R> extends SqmInterpreter<R, SqmSelectStatement<R>, SelectStatement, JdbcOperationQuerySelect> {
		public SqmSelectInterpreter(
				String hql,
				SessionFactoryImplementor sessionFactory) {
			super( hql, sessionFactory );
		}

		@Override
		protected SqmTranslator<SelectStatement> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			final SqmTranslatorFactory sqmTranslatorFactory = sessionFactory.getQueryEngine().getSqmTranslatorFactory();
			return sqmTranslatorFactory.createSelectTranslator(
					(SqmSelectStatement<R>) hqlInterpretation.getSqmStatement(),
					QueryOptions.NONE,
					hqlInterpretation.getDomainParameterXref(),
					parameterBindings,
					new LoadQueryInfluencers( sessionFactory),
					sessionFactory.getSqlTranslationEngine(),
					true
			);
		}

		@Override
		protected SqlAstTranslator<JdbcOperationQuerySelect> createSqlAstTranslator(
				SqmTranslation<SelectStatement> sqmTranslation) {
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory();
			return sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqmTranslation.getSqlAst() );
		}
	}

	private static class SqmDeleteInterpreter<R> extends SqmInterpreter<R, SqmDeleteStatement<R>, DeleteStatement, JdbcOperationQueryDelete> {
		public SqmDeleteInterpreter(String hql, SessionFactoryImplementor sessionFactory) {
			super( hql, sessionFactory );
		}

		@Override
		protected SqmTranslator<DeleteStatement> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			final SqmTranslatorFactory sqmTranslatorFactory = sessionFactory.getQueryEngine().getSqmTranslatorFactory();
			//noinspection unchecked
			return (SqmTranslator<DeleteStatement>) sqmTranslatorFactory.createMutationTranslator(
					(SqmDeleteStatement<?>) hqlInterpretation.getSqmStatement(),
					QueryOptions.NONE,
					hqlInterpretation.getDomainParameterXref(),
					parameterBindings,
					new LoadQueryInfluencers(sessionFactory),
					sessionFactory.getSqlTranslationEngine()
			);
		}

		@Override
		protected SqlAstTranslator<JdbcOperationQueryDelete> createSqlAstTranslator(SqmTranslation<DeleteStatement> sqmTranslation) {
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory();
			//noinspection unchecked
			return (SqlAstTranslator<JdbcOperationQueryDelete>) sqlAstTranslatorFactory.buildMutationTranslator(
					sessionFactory,
					sqmTranslation.getSqlAst()
			);
		}
	}

	private static class SqmUpdateInterpreter<R> extends SqmInterpreter<R, SqmUpdateStatement<R>, UpdateStatement, JdbcOperationQueryUpdate> {
		public SqmUpdateInterpreter(String hql, SessionFactoryImplementor sessionFactory) {
			super( hql, sessionFactory );
		}

		@Override
		protected SqmTranslator<UpdateStatement> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			final SqmTranslatorFactory sqmTranslatorFactory = sessionFactory.getQueryEngine().getSqmTranslatorFactory();
			//noinspection unchecked
			return (SqmTranslator<UpdateStatement>) sqmTranslatorFactory.createMutationTranslator(
					(SqmUpdateStatement<?>) hqlInterpretation.getSqmStatement(),
					QueryOptions.NONE,
					hqlInterpretation.getDomainParameterXref(),
					parameterBindings,
					new LoadQueryInfluencers(sessionFactory),
					sessionFactory.getSqlTranslationEngine()
			);
		}

		@Override
		protected SqlAstTranslator<JdbcOperationQueryUpdate> createSqlAstTranslator(SqmTranslation<UpdateStatement> sqmTranslation) {
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory();
			//noinspection unchecked
			return (SqlAstTranslator<JdbcOperationQueryUpdate>) sqlAstTranslatorFactory.buildMutationTranslator(
					sessionFactory,
					sqmTranslation.getSqlAst()
			);
		}
	}

	private static class SqmInsertInterpreter<R> extends SqmInterpreter<R, SqmInsertStatement<R>, InsertStatement, JdbcOperationQueryInsert> {
		public SqmInsertInterpreter(String hql, SessionFactoryImplementor sessionFactory) {
			super( hql, sessionFactory );
		}

		@Override
		protected SqmTranslator<InsertStatement> createSqmTranslator(
				HqlInterpretation<R> hqlInterpretation,
				QueryParameterBindingsImpl parameterBindings) {
			final SqmTranslatorFactory sqmTranslatorFactory = sessionFactory.getQueryEngine().getSqmTranslatorFactory();
			//noinspection unchecked
			return (SqmTranslator<InsertStatement>) sqmTranslatorFactory.createMutationTranslator(
					(SqmInsertStatement<?>) hqlInterpretation.getSqmStatement(),
					QueryOptions.NONE,
					hqlInterpretation.getDomainParameterXref(),
					parameterBindings,
					new LoadQueryInfluencers(sessionFactory),
					sessionFactory.getSqlTranslationEngine()
			);
		}

		@Override
		protected SqlAstTranslator<JdbcOperationQueryInsert> createSqlAstTranslator(SqmTranslation<InsertStatement> sqmTranslation) {
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = sessionFactory
					.getJdbcServices()
					.getJdbcEnvironment()
					.getSqlAstTranslatorFactory();
			//noinspection unchecked
			return (SqlAstTranslator<JdbcOperationQueryInsert>) sqlAstTranslatorFactory.buildMutationTranslator(
					sessionFactory,
					sqmTranslation.getSqlAst()
			);
		}
	}

	private record NonCopyingHqlInterpretationImpl<R>(
			SqmStatement<R> sqmAst,
			ParameterMetadataImplementor parameterMetadata,
			DomainParameterXref domainParameterXref) implements HqlInterpretation<R> {
		@Override
		public SqmStatement<R> getSqmStatement() {
			return sqmAst();
		}

		@Override
		public ParameterMetadataImplementor getParameterMetadata() {
			return parameterMetadata();
		}

		@Override
		public DomainParameterXref getDomainParameterXref() {
			return domainParameterXref();
		}

		@Override
		public void validateResultType(Class<?> resultType) {
			// irrelevant here
		}
	}

}
