/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.tree.CteStatement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Bulk-id delete handler that uses CTE and VALUES lists.
 *
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CteDeleteHandlerImpl extends AbstractCteMutationHandler implements DeleteHandler {
	protected CteDeleteHandlerImpl(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			CteBasedMutationStrategy strategy,
			HandlerCreationContext creationContext) {
		super( sqmDeleteStatement, domainParameterXref, strategy, creationContext );
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		final List<Object> ids = selectMatchingIds( executionContext );

		final QuerySpec cteQuerySpec = getStrategy().getCteTable().createCteSubQuery( executionContext );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		final QuerySpec cteDefinitionQuerySpec = getStrategy().getCteTable().createCteDefinition(
				ids,
				jdbcParameterBindings,
				executionContext
		);

		// for every table to be deleted, create the CteStatement and execute it


		getEntityDescriptor().visitAttributes(
				attribute -> {
					final PluralPersistentAttribute pluralAttribute = (PluralPersistentAttribute) attribute;
					final PersistentCollectionDescriptor collectionDescriptor = pluralAttribute.getCollectionDescriptor();

					if ( collectionDescriptor.getSeparateCollectionTable() != null ) {
						// this collection has a separate collection table, meaning it is one of:
						//		1) element-collection
						//		2) many-to-many
						//		3) one-to many using a dedicated join-table
						//
						// in all of these cases, we should clean up the matching rows in the
						// collection table

						executeDelete(
								cteDefinitionQuerySpec,
								collectionDescriptor.getSeparateCollectionTable(),
								collectionDescriptor.getCollectionKeyDescriptor().getJoinForeignKey().getColumnMappings().getReferringColumns(),
								cteQuerySpec,
								jdbcParameterBindings,
								executionContext
						);
					}
				},
				attribute -> attribute instanceof PluralPersistentAttribute
		);

		getEntityDescriptor().getHierarchy().visitConstraintOrderedTables(
				(table,columns) -> executeDelete(
						cteDefinitionQuerySpec,
						table,
						columns,
						cteQuerySpec,
						jdbcParameterBindings,
						executionContext
				)
		);

		return ids.size();
	}

	private List<Object> selectMatchingIds(ExecutionContext executionContext) {
		return SqmMutationStrategyHelper.selectMatchingIds(
				getDomainParameterXref(),
				getSqmDeleteOrUpdateStatement(),
				executionContext
		);
	}

	protected void executeDelete(
			QuerySpec cteDefinition,
			Table targetTable,
			List<Column> columnsToMatch,
			QuerySpec cteSubQuery,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final CteStatement cteStatement = generateCteStatement(
				cteDefinition,
				targetTable,
				columnsToMatch,
				cteSubQuery,
				executionContext
		);
		final JdbcDelete jdbcDelete = SqlDeleteToJdbcDeleteConverter.interpret(
				cteStatement,
				executionContext.getSession().getSessionFactory()
		);

		final JdbcMutationExecutor executor = JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL;
		executor.execute(
				jdbcDelete,
				jdbcParameterBindings,
				executionContext,
				(rows, preparedStatement) -> {}
		);
	}

	protected CteStatement generateCteStatement(
			QuerySpec cteDefinition,
			Table targetTable,
			List<Column> columnsToMatch,
			QuerySpec cteSubQuery,
			@SuppressWarnings("unused") ExecutionContext executionContext) {
		return new CteStatement(
				cteDefinition,
				CteBasedMutationStrategy.ID_CTE,
				getStrategy().getCteTable(),
				generateCteConsumer( targetTable, columnsToMatch, cteSubQuery )
		);
	}

	private DeleteStatement generateCteConsumer(Table targetTable, List<Column> columnsToMatch, QuerySpec cteSubQuery) {
		final TableReference targetTableReference = new TableReference(
				targetTable,
				null,
				false
		);

		final Expression columnsToMatchExpression;

		if ( columnsToMatch.size() == 1 ) {
			columnsToMatchExpression = targetTableReference.resolveColumnReference( columnsToMatch.get( 0 ) );
		}
		else {
			final List<Expression> columnsToMatchExpressions = new ArrayList<>();
			for ( Column toMatch : columnsToMatch ) {
				columnsToMatchExpressions.add( targetTableReference.resolveColumnReference( toMatch ) );
			}
			columnsToMatchExpression = new SqlTuple( columnsToMatchExpressions, getEntityDescriptor().getIdType() );
		}

		return new DeleteStatement(
				targetTableReference,
				new InSubQueryPredicate(
						columnsToMatchExpression,
						cteSubQuery,
						false
				)
		);
	}
}
