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
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslation;
import org.hibernate.query.sqm.sql.SimpleSqmDeleteTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan implements NonSelectQueryPlan {
	private final SqmDeleteStatement sqmDelete;
	private final DomainParameterXref domainParameterXref;

	private JdbcDelete jdbcDelete;
	private FromClauseAccess tableGroupAccess;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

	public SimpleDeleteQueryPlan(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref) {
		this.sqmDelete = sqmDelete;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
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

			final SimpleSqmDeleteTranslation sqmInterpretation = translator.translate( sqmDelete );

			tableGroupAccess = translator.getFromClauseAccess();

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
				tableGroupAccess::findTableGroup,
				executionContext.getSession()
		);

		return jdbcServices.getJdbcDeleteExecutor().execute(
				jdbcDelete,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}
}
