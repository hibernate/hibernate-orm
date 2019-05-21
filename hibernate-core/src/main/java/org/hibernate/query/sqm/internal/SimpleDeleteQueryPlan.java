/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmDeleteInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmDeleteToSqlAstConverterSimple;
import org.hibernate.sql.exec.internal.JdbcMutationExecutorImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;

import static org.hibernate.query.internal.QueryHelper.buildJdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteQueryPlan implements NonSelectQueryPlan {
	private final SqmDeleteStatement sqmStatement;
	private final DomainParameterXref domainParameterXref;

	public SimpleDeleteQueryPlan(SqmDeleteStatement sqmStatement) {
		this.sqmStatement = sqmStatement;
		domainParameterXref = DomainParameterXref.from( sqmStatement );

		// todo (6.0) : here is where we need to perform the conversion into SQL AST
	}

	@Override
	public int executeUpdate(ExecutionContext executionContext) {
		final SqmDeleteInterpretation sqmInterpretation = SqmDeleteToSqlAstConverterSimple.interpret(
				sqmStatement,
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getSession()
		);

		// the converter should enforce this, simple assertion here
		assert sqmInterpretation.getSqlDeletes().size() == 1;

		final JdbcMutation jdbcDelete = SqlDeleteToJdbcDeleteConverter.interpret(
				sqmInterpretation.getSqlDeletes().get( 0 ),
				executionContext.getSession().getSessionFactory()
		);


		return JdbcMutationExecutorImpl.WITH_AFTER_STATEMENT_CALL.execute(
				jdbcDelete,
				buildJdbcParameterBindings( sqmStatement, sqmInterpretation, executionContext ),
				executionContext
		);
	}
}
