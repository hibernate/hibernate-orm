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
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.sql.SimpleSqmUpdateTranslation;
import org.hibernate.query.sqm.sql.SimpleSqmUpdateTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.SqlAstUpdateTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
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
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

	public SimpleUpdateQueryPlan(
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref) {
		this.sqmUpdate = sqmUpdate;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();

		if ( jdbcUpdate == null ) {
			final QueryEngine queryEngine = factory.getQueryEngine();

			final SqmTranslatorFactory translatorFactory = queryEngine.getSqmTranslatorFactory();
			final SimpleSqmUpdateTranslator translator = translatorFactory.createSimpleUpdateTranslator(
					executionContext.getQueryOptions(),
					domainParameterXref,
					executionContext.getQueryParameterBindings(),
					executionContext.getLoadQueryInfluencers(),
					factory
			);

			final SimpleSqmUpdateTranslation sqmInterpretation = translator.translate( sqmUpdate );

			tableGroupAccess = translator.getFromClauseAccess();

			this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
					domainParameterXref,
					sqmInterpretation::getJdbcParamsBySqmParam
			);

			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

			final SqlAstUpdateTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildUpdateTranslator( factory );

			jdbcUpdate = sqlAstTranslator.translate( sqmInterpretation.getSqlAst() );
		}


		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getDomainModel(),
				tableGroupAccess::findTableGroup,
				session
		);
		jdbcUpdate.bindFilterJdbcParameters( jdbcParameterBindings );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcUpdate,
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
