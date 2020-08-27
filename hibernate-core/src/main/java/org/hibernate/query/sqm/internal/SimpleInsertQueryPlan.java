/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.sql.SqmInsertTranslation;
import org.hibernate.query.sqm.sql.SqmInsertTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.SqlAstInsertTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class SimpleInsertQueryPlan implements NonSelectQueryPlan {
	private final SqmInsertStatement sqmInsert;
	private final DomainParameterXref domainParameterXref;

	private JdbcInsert jdbcInsert;
	private FromClauseAccess tableGroupAccess;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

	public SimpleInsertQueryPlan(
			SqmInsertStatement sqmInsert,
			DomainParameterXref domainParameterXref) {
		this.sqmInsert = sqmInsert;
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();

		if ( jdbcInsert == null ) {
			final QueryEngine queryEngine = factory.getQueryEngine();

			final SqmTranslatorFactory translatorFactory = queryEngine.getSqmTranslatorFactory();
			final SqmInsertTranslator translator = translatorFactory.createInsertTranslator(
					executionContext.getQueryOptions(),
					domainParameterXref,
					executionContext.getQueryParameterBindings(),
					executionContext.getLoadQueryInfluencers(),
					factory
			);

			final SqmInsertTranslation sqmInterpretation = translator.translate(sqmInsert);

			tableGroupAccess = translator.getFromClauseAccess();

			this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref(
					domainParameterXref,
					sqmInterpretation::getJdbcParamsBySqmParam
			);

			final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
			final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

			final SqlAstInsertTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildInsertTranslator( factory );

			jdbcInsert = sqlAstTranslator.translate( sqmInterpretation.getSqlAst() );
		}


		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				factory.getDomainModel(),
				tableGroupAccess::findTableGroup,
				session
		);

		jdbcInsert.bindFilterJdbcParameters( jdbcParameterBindings );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcInsert,
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
