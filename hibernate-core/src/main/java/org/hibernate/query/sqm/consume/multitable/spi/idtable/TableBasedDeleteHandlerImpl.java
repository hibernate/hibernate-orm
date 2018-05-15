/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.consume.multitable.spi.DeleteHandler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedHandler
		implements DeleteHandler {

	private TableBasedDeleteHandlerImpl(
			SqmDeleteStatement sqmDeleteStatement,
			EntityTypeDescriptor entityDescriptor,
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

		final QuerySpec idTableSelectSubQuerySpec = generateIdTableSelect( executionContext );

		String idTableSelectSubQuery = SqlAstSelectToJdbcSelectConverter.interpret(
				idTableSelectSubQuerySpec,
				executionContext.getSessionFactory()
		).getSql();

		for ( JoinedTableBinding joinedTable : getEntityDescriptor().getSecondaryTableBindings() ) {
			deleteFrom( joinedTable.getReferringTable(), idTableSelectSubQuery, executionContext );
		}

		deleteFrom( getEntityDescriptor().getPrimaryTable(), idTableSelectSubQuery, executionContext );
	}

	private void deleteFrom(Table table, String idTableSelectSubQuery, HandlerExecutionContext executionContext) {
		final Dialect dialect = executionContext.getSessionFactory().getJdbcServices().getDialect();
		final StringBuilder sqlBuffer = new StringBuilder(  );
		sqlBuffer.append( "delete from " )
				.append( table.render( dialect ) )
				.append( " where " );

		if ( table.getPrimaryKey().getColumns().size() == 1 ) {
			sqlBuffer.append( table.getPrimaryKey().getColumns().get( 0 ).getName().render( dialect ) )
					.append( "= " )
					.append( idTableSelectSubQuery );
		}
		else {
			sqlBuffer.append( "(" );
			boolean firstPass = true;
			for ( PhysicalColumn physicalColumn : table.getPrimaryKey().getColumns() ) {
				if ( firstPass ) {
					firstPass = false;
				}
				else {
					sqlBuffer.append( "," );
				}
				sqlBuffer.append( physicalColumn.getName().render( dialect ) );
			}
			sqlBuffer.append( ") = " )
					.append( idTableSelectSubQuery );
		}

		final String deleteStatement = sqlBuffer.toString();

		JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
				new JdbcDelete() {
					@Override
					public String getSql() {
						return deleteStatement;
					}

					@Override
					public List<JdbcParameterBinder> getParameterBinders() {
						return Collections.emptyList();
					}

					@Override
					public Set<String> getAffectedTableNames() {
						return Collections.singleton( table.getTableExpression() );
					}
				},
				executionContext,
				Connection::prepareStatement
		);
	}

	public static class Builder {
		private final SqmDeleteStatement sqmStatement;
		private final EntityTypeDescriptor entityDescriptor;
		private final IdTable idTableInfo;
		private final IdTableSupport idTableSupport;

		private SessionUidSupport sessionUidSupport = SessionUidSupport.NONE;
		private BeforeUseAction beforeUseAction = BeforeUseAction.NONE;
		private AfterUseAction afterUseAction = AfterUseAction.NONE;
		private IdTableManagementTransactionality transactionality = IdTableManagementTransactionality.NONE;

		public Builder(
				SqmDeleteStatement sqmStatement,
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
