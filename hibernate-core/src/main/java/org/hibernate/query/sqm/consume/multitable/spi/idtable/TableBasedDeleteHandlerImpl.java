/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.multitable.spi.DeleteHandler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedHandler
		implements DeleteHandler {

	private TableBasedDeleteHandlerImpl(
			SqmDeleteStatement sqmDeleteStatement,
			EntityDescriptor entityDescriptor,
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableManagementTransactionality transactionality,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteStatement,
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
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(HandlerExecutionContext executionContext) {
		// todo (6.0) : see TableBasedUpdateHandlerImpl#performMutations for general guideline

		// todo (6.0) : who is responsible for injecting any strategy-specific restrictions (i.e., session-uid)?
	}

	public static class Builder {
		private final SqmDeleteStatement sqmStatement;
		private final EntityDescriptor entityDescriptor;
		private final IdTable idTableInfo;
		private final IdTableSupport idTableSupport;

		private SessionUidSupport sessionUidSupport = SessionUidSupport.NONE;
		private BeforeUseAction beforeUseAction = BeforeUseAction.NONE;
		private AfterUseAction afterUseAction = AfterUseAction.NONE;
		private IdTableManagementTransactionality transactionality = IdTableManagementTransactionality.NONE;

		public Builder(
				SqmDeleteStatement sqmStatement,
				EntityDescriptor entityDescriptor,
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

		public TableBasedDeleteHandlerImpl build(HandlerCreationContext creationContext) {
			return new TableBasedDeleteHandlerImpl(
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
