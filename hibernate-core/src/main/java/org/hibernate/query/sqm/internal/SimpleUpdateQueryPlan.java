/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.action.internal.BulkOperationCleanupAction;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.SqlOmittingQueryOptions;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class SimpleUpdateQueryPlan implements NonSelectQueryPlan {
	private final SqmUpdateStatement sqmUpdate;
	private final DomainParameterXref domainParameterXref;

	private JdbcUpdate jdbcUpdate;
	private FromClauseAccess tableGroupAccess;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> jdbcParamsXref;
	private Map<SqmParameter,MappingModelExpressable> sqmParamMappingTypeResolutions;

	public SimpleUpdateQueryPlan(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref) {
		this.sqmUpdate = sqmUpdate;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext, sqmUpdate );
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		SqlAstTranslator<JdbcUpdate> updateTranslator = null;
		if ( jdbcUpdate == null ) {
			updateTranslator = createUpdateTranslator( executionContext );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getDomainModel(),
				tableGroupAccess::findTableGroup,
				sqmParamMappingTypeResolutions::get,
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
		else if ( jdbcUpdate != null ) {
			jdbcUpdate.bindFilterJdbcParameters( jdbcParameterBindings );
		}

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcUpdate,
				jdbcParameterBindings,
				sql -> session
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				SqlOmittingQueryOptions.omitSqlQueryOptions( executionContext )
		);
	}

	private SqlAstTranslator<JdbcUpdate> createUpdateTranslator(ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();

		final SqmTranslatorFactory translatorFactory = queryEngine.getSqmTranslatorFactory();
		final SqmTranslator<UpdateStatement> translator = translatorFactory.createSimpleUpdateTranslator(
				sqmUpdate,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				factory
		);

		final SqmTranslation<UpdateStatement> sqmInterpretation = translator.translate();

		tableGroupAccess = sqmInterpretation.getFromClauseAccess();

		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				sqmInterpretation::getJdbcParamsBySqmParam
		);

		this.sqmParamMappingTypeResolutions = sqmInterpretation.getSqmParameterMappingModelTypeResolutions();

		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildUpdateTranslator( factory, sqmInterpretation.getSqlAst() );
	}
}
