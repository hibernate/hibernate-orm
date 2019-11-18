/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
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
public class UnrestrictedDeleteExecutionDelegate implements TableBasedDeleteHandler.ExecutionDelegate {
	private final EntityMappingType entityDescriptor;

	public UnrestrictedDeleteExecutionDelegate(EntityMappingType entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		// NOTE : we want the number of rows returned from this method to be the number of rows deleted
		// 		from the root table of the entity hierarchy,  which happens to be the last table we
		// 		will visit
		final AtomicInteger result = new AtomicInteger();

		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnsVisitationSupplier) -> {
					final int rows = deleteFrom( tableExpression, executionContext );
					result.set( rows );
				}
		);

		return result.get();
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

		return jdbcServices.getJdbcMutationExecutor().execute(
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
