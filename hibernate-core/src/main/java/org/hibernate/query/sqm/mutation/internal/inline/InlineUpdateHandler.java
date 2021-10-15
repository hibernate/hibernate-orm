/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;

/**
 * @author Steve Ebersole
 */
public class InlineUpdateHandler implements UpdateHandler {
	private final SqmUpdateStatement sqmUpdate;
	private final DomainParameterXref domainParameterXref;
	private final MatchingIdRestrictionProducer matchingIdsPredicateProducer;

	private final DomainQueryExecutionContext executionContext;

	private final SessionFactoryImplementor sessionFactory;
	private final SqlAstTranslatorFactory sqlAstTranslatorFactory;
	private final JdbcMutationExecutor jdbcMutationExecutor;

	public InlineUpdateHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmUpdateStatement sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		this.matchingIdsPredicateProducer = matchingIdsPredicateProducer;
		this.domainParameterXref = domainParameterXref;
		this.sqmUpdate = sqmUpdate;

		this.executionContext = context;

		this.sessionFactory = executionContext.getSession().getFactory();
		this.sqlAstTranslatorFactory = sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
		this.jdbcMutationExecutor = sessionFactory.getJdbcServices().getJdbcMutationExecutor();
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
