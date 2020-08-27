/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingModelHelper;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslation;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan implements NonSelectQueryPlan {
	private final EntityMappingType entityDescriptor;
	private final SqmDeleteStatement sqmDelete;
	private final DomainParameterXref domainParameterXref;

	private JdbcDelete jdbcDelete;
	private SimpleSqmDeleteTranslation sqmInterpretation;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

	public SimpleDeleteQueryPlan(
			EntityMappingType entityDescriptor,
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref) {
		assert entityDescriptor.getEntityName().equals( sqmDelete.getTarget().getEntityName() );

		this.entityDescriptor = entityDescriptor;
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();

		if ( jdbcDelete == null ) {
			final QueryEngine queryEngine = factory.getQueryEngine();

			final SqmTranslatorFactory translatorFactory = queryEngine.getSqmTranslatorFactory();
			final SimpleSqmDeleteTranslator translator = translatorFactory.createSimpleDeleteTranslator(
					executionContext.getQueryOptions(),
					domainParameterXref,
					executionContext.getQueryParameterBindings(),
					executionContext.getLoadQueryInfluencers(),
					factory
			);

			sqmInterpretation = translator.translate( sqmDelete );

			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

			final SqlAstDeleteTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildDeleteTranslator( factory );

			jdbcDelete = sqlAstTranslator.translate( sqmInterpretation.getSqlAst() );

			this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
					domainParameterXref,
					sqmInterpretation::getJdbcParamsBySqmParam
			);
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getDomainModel(),
				sqmInterpretation.getFromClauseAccess()::findTableGroup,
				session
		);
		jdbcDelete.bindFilterJdbcParameters( jdbcParameterBindings );

		final boolean missingRestriction = sqmDelete.getWhereClause() == null
				|| sqmDelete.getWhereClause().getPredicate() == null;
		if ( missingRestriction ) {
			assert domainParameterXref.getSqmParameterCount() == 0;
			assert jdbcParamsXref.isEmpty();
		}

		SqmMutationStrategyHelper.cleanUpCollectionTables(
				entityDescriptor,
				(tableReference, attributeMapping) -> {
					if ( missingRestriction ) {
						return null;
					}

					final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
					final Expression fkColumnExpression = MappingModelHelper.buildColumnReferenceExpression(
							fkDescriptor,
							null,
							factory
					);

					final QuerySpec matchingIdSubQuery = new QuerySpec( false );

					final Expression fkTargetColumnExpression = MappingModelHelper.buildColumnReferenceExpression(
							fkDescriptor,
							sqmInterpretation.getSqlExpressionResolver(),
							factory
					);
					matchingIdSubQuery.getSelectClause().addSqlSelection( new SqlSelectionImpl( 1, 0, fkTargetColumnExpression ) );

					matchingIdSubQuery.getFromClause().addRoot(
							new MutatingTableReferenceGroupWrapper(
									new NavigablePath( attributeMapping.getRootPathName() ),
									attributeMapping,
									sqmInterpretation.getSqlAst().getTargetTable()
							)
					);

					matchingIdSubQuery.applyPredicate( sqmInterpretation.getSqlAst().getRestriction() );

					return new InSubQueryPredicate( fkColumnExpression, matchingIdSubQuery, false );
				},
				missingRestriction ? JdbcParameterBindings.NO_BINDINGS : jdbcParameterBindings,
				executionContext
		);

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcDelete,
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
