/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandler
		extends AbstractTableBasedHandler
		implements DeleteHandler {

	private static final Logger log = Logger.getLogger( TableBasedDeleteHandler.class );


	public TableBasedDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			IdTable idTable,
			Supplier<IdTableExporter> exporterSupplier,
			BeforeUseAction beforeUseAction,
			AfterUseAction afterUseAction,
			TempTableDdlTransactionHandling transactionality,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super(
				sqmDeleteStatement,
				idTable,
				transactionality,
				domainParameterXref,
				beforeUseAction,
				afterUseAction,
				session -> session.getSessionIdentifier().toString(),
				exporterSupplier,
				creationContext
		);
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(ExecutionContext executionContext) {
		log.trace( "performMutations - " + getEntityDescriptor().getEntityName() );

		// create the selection of "matching ids" from the id-table.  this is used as the subquery in
		// used to restrict the deletions from each table
		final QuerySpec idTableSelectSubQuerySpec = createIdTableSubQuery( executionContext );

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnsVisitationSupplier) -> {
					deleteFrom( tableExpression, tableKeyColumnsVisitationSupplier, idTableSelectSubQuerySpec, executionContext );
				}
		);
	}

	private static class TableKeyExpressionCollector {
		private final EntityMappingType entityMappingType;

		public TableKeyExpressionCollector(EntityMappingType entityMappingType) {
			this.entityMappingType = entityMappingType;
		}

		Expression firstColumnExpression;
		List<Expression> collectedColumnExpressions;

		void apply(ColumnReference columnReference) {
			if ( firstColumnExpression == null ) {
				firstColumnExpression = columnReference;
			}
			else if ( collectedColumnExpressions == null ) {
				collectedColumnExpressions = new ArrayList<>();
				collectedColumnExpressions.add( firstColumnExpression );
				collectedColumnExpressions.add( columnReference );
			}
			else {
				collectedColumnExpressions.add( columnReference );
			}
		}

		Expression buildKeyExpression() {
			if ( collectedColumnExpressions == null ) {
				return firstColumnExpression;
			}

			return new SqlTuple( collectedColumnExpressions, entityMappingType.getIdentifierMapping() );
		}
	}

	private void deleteFrom(
			String tableExpression,
			Supplier<Consumer<ColumnConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSelectSubQuery,
			ExecutionContext executionContext) {
		log.trace( "deleteFrom - " + tableExpression );

		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( getEntityDescriptor() );

		tableKeyColumnVisitationSupplier.get().accept(
				(columnExpression, containingTableExpression, jdbcMapping) -> {
					assert containingTableExpression.equals( tableExpression );
					keyColumnCollector.apply(
							new ColumnReference(
									(String) null,
									columnExpression,
									jdbcMapping,
									factory
							)
					);
				}
		);

		final InSubQueryPredicate predicate = new InSubQueryPredicate(
				keyColumnCollector.buildKeyExpression(),
				idTableSelectSubQuery,
				false
		);

		final DeleteStatement deleteStatement = new DeleteStatement(
				new TableReference( tableExpression, null, true, factory ),
				predicate
		);

		final JdbcServices jdbcServices = factory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();

		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final SqlAstDeleteTranslator sqlAstTranslator = sqlAstTranslatorFactory.buildDeleteTranslator( factory );
		final JdbcDelete jdbcDelete = sqlAstTranslator.translate( deleteStatement );

		final int rows = jdbcServices.getJdbcDeleteExecutor().execute(
				jdbcDelete,
				JdbcParameterBindings.NO_BINDINGS,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
				executionContext
		);

		log.debugf( "delete-from `%s` : %s rows", tableExpression, rows );
	}
}
