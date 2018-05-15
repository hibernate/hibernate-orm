/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.consume.multitable.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.spi.SqlAstUpdateDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.SqmUpdateToSqlAstConverterMultiTable;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandlerImpl
		extends AbstractTableBasedHandler
		implements UpdateHandler {
	private TableBasedUpdateHandlerImpl(
			SqmDeleteOrUpdateStatement sqmDeleteOrUpdateStatement,
			EntityTypeDescriptor entityDescriptor,
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableManagementTransactionality transactionality,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteOrUpdateStatement,
				entityDescriptor,
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
				creationContext
		);
	}

	@Override
	public SqmUpdateStatement getSqmDeleteOrUpdateStatement() {
		return (SqmUpdateStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(HandlerExecutionContext executionContext) {
		boolean hasNoSecondaryTables = getSqmDeleteOrUpdateStatement().getEntityFromElement()
				.getNavigableReference()
				.getEntityDescriptor()
				.getSecondaryTableBindings()
				.isEmpty();

		final List<SqlAstUpdateDescriptor> updateDescriptors = SqmUpdateToSqlAstConverterMultiTable.interpret(
				getSqmDeleteOrUpdateStatement(),
				generateIdTableSelect( executionContext ),
				executionContext.getQueryOptions(),
				executionContext
		);

		for ( SqlAstUpdateDescriptor updateDescriptor : updateDescriptors ) {
			// convert each SQL AST UpdateStatement into a JdbcUpdate operation
			// 		and execute it

			final JdbcUpdate jdbcUpdate = UpdateToJdbcUpdateConverter.createJdbcUpdate(
					updateDescriptor.getSqlAstStatement(),
					executionContext.getSessionFactory()
			);

			JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
					jdbcUpdate,
					executionContext,
					Connection::prepareStatement
			);

		}
	}

	public static class Builder {
		private final SqmDeleteOrUpdateStatement sqmStatement;
		private final EntityTypeDescriptor entityDescriptor;
		private final IdTable idTableInfo;
		private final IdTableSupport idTableSupport;

		private SessionUidSupport sessionUidSupport = SessionUidSupport.NONE;
		private BeforeUseAction beforeUseAction = BeforeUseAction.NONE;
		private AfterUseAction afterUseAction = AfterUseAction.NONE;
		private IdTableManagementTransactionality transactionality = IdTableManagementTransactionality.NONE;

		public Builder(
				SqmUpdateStatement sqmStatement,
				EntityTypeDescriptor entityDescriptor,
				IdTable idTableInfo,
				IdTableSupport idTableSupport) {
			this.sqmStatement = sqmStatement;
			this.entityDescriptor = entityDescriptor;
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

		public TableBasedUpdateHandlerImpl build(HandlerCreationContext creationContext) {
			return new TableBasedUpdateHandlerImpl(
					sqmStatement,
					entityDescriptor,
					idTableInfo,
					idTableSupport,
					sessionUidSupport,
					beforeUseAction,
					afterUseAction,
					transactionality,
					creationContext
			);
		}
	}
}
