/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.simple;

import java.util.Collections;

import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.sqm.consume.internal.SqmConsumeHelper;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.produce.internal.SqlAstDeleteDescriptorImpl;
import org.hibernate.sql.ast.produce.sqm.spi.SqmDeleteToSqlAstConverterSimple;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.exec.internal.JdbcMutationExecutorImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class SimpleDeleteHandler extends AbstractMutationHandler implements DeleteHandler {
	private final DomainParameterXref domainParameterXref;

	@SuppressWarnings("WeakerAccess")
	protected SimpleDeleteHandler(
			SqmDeleteStatement sqmStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super( sqmStatement, creationContext );
		this.domainParameterXref = domainParameterXref;
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		final SqmDeleteToSqlAstConverterSimple sqmConverter = new SqmDeleteToSqlAstConverterSimple(
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getSession()
		);

		final DeleteStatement deleteStatement = sqmConverter.visitDeleteStatement( getSqmDeleteOrUpdateStatement() );

		//noinspection unchecked
		final JdbcParameterBindings jdbcParameterBindings = QueryHelper.createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				SqmConsumeHelper.generateJdbcParamsXref( domainParameterXref, sqmConverter ),
				executionContext.getSession()
		);

		final JdbcMutation jdbcDelete = SqlDeleteToJdbcDeleteConverter.interpret(
				new SqlAstDeleteDescriptorImpl(
						deleteStatement,
						Collections.singleton(
								deleteStatement.getTargetTable().getTable().getTableExpression()
						)
				),
				executionContext.getSession().getSessionFactory()
		);

		return JdbcMutationExecutorImpl.WITH_AFTER_STATEMENT_CALL.execute(
				jdbcDelete,
				jdbcParameterBindings,
				executionContext
		);
	}
}
