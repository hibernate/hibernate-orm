/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class UnrestrictedTableBasedDeleteHandler extends TableBasedDeleteHandler  implements DeleteHandler {
	public UnrestrictedTableBasedDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			IdTable idTable,
			TempTableDdlTransactionHandling ddlTransactionHandling,
			DomainParameterXref domainParameterXref,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteStatement,
				idTable,
				() -> null,
				beforeUseAction,
				afterUseAction,
				ddlTransactionHandling,
				domainParameterXref,
				creationContext
		);
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		final AtomicInteger rows = new AtomicInteger();

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnsVisitationSupplier) -> {
					rows.set( deleteFrom( tableExpression, executionContext ) );
				}
		);

		return rows.get();
	}

	private int deleteFrom(
			String tableExpression,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final DeleteStatement deleteStatement = new DeleteStatement(
				new TableReference( tableExpression, null, true, factory ),
				null
		);

		final JdbcServices jdbcServices = factory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final SqlAstDeleteTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildDeleteTranslator( factory );
		final JdbcDelete jdbcDelete = sqlAstTranslator.translate( deleteStatement );

		return jdbcServices.getJdbcDeleteExecutor().execute(
				jdbcDelete,
				JdbcParameterBindings.NO_BINDINGS,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);

	}
}
