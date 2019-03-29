/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import java.util.List;

import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandlerImpl
		extends AbstractTableBasedHandler
		implements UpdateHandler {
	private TableBasedUpdateHandlerImpl(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableManagementTransactionality transactionality,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteOrUpdateStatement,
				idTableInfo,
				sessionUidSupport,
				beforeUseAction,
				afterUseAction,
				new IdTableHelper(
						idTableInfo,
						idTableSupport,
						transactionality,
						creationContext.getSessionFactory().getJdbcServices()
				),
				domainParameterXref,
				creationContext
		);
	}

	@Override
	public SqmUpdateStatement getSqmDeleteOrUpdateStatement() {
		return (SqmUpdateStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(ExecutionContext executionContext) {
		boolean hasNoSecondaryTables = getSqmDeleteOrUpdateStatement().getTarget()
				.getReferencedNavigable()
				.getEntityDescriptor()
				.getSecondaryTableBindings()
				.isEmpty();

		final QuerySpec idTableSelectSubQuerySpec = createIdTableSubQuery( executionContext );

		final List<SqlAstUpdateDescriptor> updateDescriptors = SqmUpdateToSqlAstConverterMultiTable.interpret(
				getSqmDeleteOrUpdateStatement(),
				idTableSelectSubQuerySpec,
				executionContext.getQueryOptions(),
				DomainParameterXref.empty(),
				QueryParameterBindings.NO_PARAM_BINDINGS,
				executionContext.getSession().getFactory()
		);

		for ( SqlAstUpdateDescriptor updateDescriptor : updateDescriptors ) {
			// convert each SQL AST UpdateStatement into a JdbcUpdate operation
			// 		and execute it

			final JdbcUpdate jdbcUpdate = UpdateToJdbcUpdateConverter.createJdbcUpdate(
					updateDescriptor.getSqlAstStatement(),
					executionContext.getSession().getSessionFactory()
			);

			JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
					jdbcUpdate,
					JdbcParameterBindings.NO_BINDINGS,
					executionContext
			);
		}
	}

	public static class Builder {
		private final SqmDeleteOrUpdateStatement sqmStatement;
		private final IdTable idTableInfo;
		private final IdTableSupport idTableSupport;

		private SessionUidSupport sessionUidSupport = SessionUidSupport.NONE;
		private BeforeUseAction beforeUseAction = BeforeUseAction.NONE;
		private AfterUseAction afterUseAction = AfterUseAction.NONE;
		private IdTableManagementTransactionality transactionality = IdTableManagementTransactionality.NONE;

		public Builder(
				SqmUpdateStatement sqmStatement,
				IdTable idTableInfo,
				IdTableSupport idTableSupport) {
			this.sqmStatement = sqmStatement;
			this.idTableInfo = idTableInfo;
			this.idTableSupport = idTableSupport;
		}

		public void setSessionUidSupport(SessionUidSupport sessionUidSupport) {
			this.sessionUidSupport = sessionUidSupport;
		}

		public void setBeforeUseAction(BeforeUseAction beforeUseAction) {
			this.beforeUseAction = beforeUseAction;
		}

		public void setAfterUseAction(AfterUseAction afterUseAction) {
			this.afterUseAction = afterUseAction;
		}

		public void setTransactionality(IdTableManagementTransactionality transactionality) {
			this.transactionality = transactionality;
		}

		public TableBasedUpdateHandlerImpl build(
				DomainParameterXref domainParameterXref,
				HandlerCreationContext creationContext) {
			return new TableBasedUpdateHandlerImpl(
					sqmStatement,
					idTableInfo,
					idTableSupport,
					sessionUidSupport,
					beforeUseAction,
					afterUseAction,
					transactionality,
					domainParameterXref,
					creationContext
			);
		}
	}
}
