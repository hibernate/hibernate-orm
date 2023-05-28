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
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * @author Gavin King
 */
public class SimpleInsertQueryPlan implements NonSelectQueryPlan {
	private final SqmInsertStatement<?> sqmInsert;
	private final DomainParameterXref domainParameterXref;
	private Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions;

	private JdbcOperationQueryInsert jdbcInsert;
	private FromClauseAccess tableGroupAccess;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;

	public SimpleInsertQueryPlan(
			SqmInsertStatement<?> sqmInsert,
			DomainParameterXref domainParameterXref) {
		this.sqmInsert = sqmInsert;
		this.domainParameterXref = domainParameterXref;
	}

	private SqlAstTranslator<JdbcOperationQueryInsert> createInsertTranslator(DomainQueryExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();

		final SqmTranslatorFactory translatorFactory = queryEngine.getSqmTranslatorFactory();
		final SqmTranslator<InsertStatement> translator = translatorFactory.createInsertTranslator(
				sqmInsert,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getQueryParameterBindings(),
				executionContext.getSession().getLoadQueryInfluencers(),
				factory
		);

		final SqmTranslation<InsertStatement> sqmInterpretation = translator.translate();

		tableGroupAccess = sqmInterpretation.getFromClauseAccess();

		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
				domainParameterXref,
				sqmInterpretation::getJdbcParamsBySqmParam
		);

		this.paramTypeResolutions = sqmInterpretation.getSqmParameterMappingModelTypeResolutions();

		return factory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildInsertTranslator( factory, sqmInterpretation.getSqlAst() );
	}

	@Override
	public int executeUpdate(DomainQueryExecutionContext executionContext) {
		BulkOperationCleanupAction.schedule( executionContext.getSession(), sqmInsert );
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		SqlAstTranslator<JdbcOperationQueryInsert> insertTranslator = null;
		if ( jdbcInsert == null ) {
			insertTranslator = createInsertTranslator( executionContext );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getRuntimeMetamodels().getMappingMetamodel(),
				tableGroupAccess::findTableGroup,
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
