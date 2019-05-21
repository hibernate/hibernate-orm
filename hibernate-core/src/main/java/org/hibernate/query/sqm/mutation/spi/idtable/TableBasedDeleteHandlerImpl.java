/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedHandler
		implements DeleteHandler {

	private TableBasedDeleteHandlerImpl(
			SqmDeleteStatement sqmDeleteStatement,
			IdTable idTableInfo,
			IdTableSupport idTableSupport,
			SessionUidSupport sessionUidSupport,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			IdTableManagementTransactionality transactionality,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteStatement,
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
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(ExecutionContext executionContext) {
		final QuerySpec idTableSelectSubQuerySpec = createIdTableSubQuery(
				executionContext
		);

		String idTableSelectSubQuery = SqlAstSelectToJdbcSelectConverter.interpret(
				idTableSelectSubQuerySpec,
				executionContext.getSession().getSessionFactory()
		).getSql();

		for ( JoinedTableBinding joinedTable : getEntityDescriptor().getSecondaryTableBindings() ) {
			deleteFrom( joinedTable.getReferringTable(), idTableSelectSubQuery, executionContext );
		}

		deleteFrom( getEntityDescriptor().getPrimaryTable(), idTableSelectSubQuery, executionContext );
	}

	private void deleteFrom(Table table, String idTableSelectSubQuery, ExecutionContext executionContext) {
		final Dialect dialect = executionContext.getSession().getSessionFactory().getJdbcServices().getDialect();
		final JdbcEnvironment jdbcEnvironment = executionContext.getSession().getSessionFactory().getJdbcServices().getJdbcEnvironment();
		final StringBuilder sqlBuffer = new StringBuilder(  );
		sqlBuffer.append( "delete from " )
				.append( table.render( dialect, jdbcEnvironment ) )
				.append( " where " );

		if ( table.getPrimaryKey().getColumns().size() == 1 ) {
			sqlBuffer.append( table.getPrimaryKey().getColumns().get( 0 ).getName().render( dialect ) )
					.append( " in " );
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
			sqlBuffer.append( ") in " );
		}

		sqlBuffer.append( idTableSelectSubQuery );

		final String deleteStatement = sqlBuffer.toString();

		JdbcMutationExecutor.NO_AFTER_STATEMENT_CALL.execute(
				new JdbcDelete() {
					@Override
					public String getSql() {
						return deleteStatement;
					}

					@Override
					public List<JdbcParameterBinder> getParameterBinders() {
						if ( getSessionUidSupport().needsSessionUidColumn() ) {
							return Collections.singletonList(
									(statement, startPosition, jdbcParameterBindings, executionContext1) -> {
										statement.setString(
												startPosition,
												getSessionUidSupport().extractUid( executionContext.getSession() )
										);
										return 1;
									}
							);
						}

						return Collections.emptyList();
					}

					@Override
					public Set<String> getAffectedTableNames() {
						return Collections.singleton( table.getTableExpression() );
					}
				},
				JdbcParameterBindings.NO_BINDINGS,
				executionContext
		);
	}

	public static class Builder {
		private final SqmDeleteStatement sqmStatement;
		private final IdTable idTableInfo;
		private final IdTableSupport idTableSupport;

		private SessionUidSupport sessionUidSupport = SessionUidSupport.NONE;
		private BeforeUseAction beforeUseAction = BeforeUseAction.NONE;
		private AfterUseAction afterUseAction = AfterUseAction.NONE;
		private IdTableManagementTransactionality transactionality = IdTableManagementTransactionality.NONE;

		public Builder(
				SqmDeleteStatement sqmStatement,
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

		public TableBasedDeleteHandlerImpl build(
				HandlerCreationContext creationContext,
				DomainParameterXref domainParameterXref) {
			return new TableBasedDeleteHandlerImpl(
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
