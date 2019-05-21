/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.inline;

import java.util.List;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.mapping.spi.Navigable;
import org.hibernate.metamodel.model.mapping.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.mapping.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcDelete;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * @author Steve Ebersole
 */
public class InlineDeleteHandler extends AbstractInlineHandler implements DeleteHandler {
	public InlineDeleteHandler(
			SqmDeleteStatement sqmDeleteStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		super( sqmDeleteStatement, domainParameterXref, creationContext );
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		final List<Object> ids = SqmMutationStrategyHelper.selectMatchingIds( getDomainParameterXref(), getSqmStatement(), executionContext );

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
								collectionDescriptor.getSeparateCollectionTable(),
								ids,
								collectionDescriptor.getCollectionKeyDescriptor().getJoinForeignKey().getColumnMappings().getTargetColumns(),
								collectionDescriptor.getCollectionKeyDescriptor(),
								executionContext
						);
					}
				},
				attribute -> attribute instanceof PluralPersistentAttribute
		);

		getEntityDescriptor().getHierarchy().visitConstraintOrderedTables(
				(table,columns) -> executeDelete(
						table,
						ids,
						columns,
						getEntityDescriptor().getIdentifierDescriptor(),
						executionContext
				)
		);


		return ids.size();
	}

	private void executeDelete(
			Table table,
			List<Object> ids,
			List<Column> columns,
			Navigable<?> navigable,
			ExecutionContext executionContext) {
		final TableReference tableReference = new TableReference( table, null, false );

		final Predicate idRestriction = generateRestrictionValuesList( tableReference, ids, columns , navigable, executionContext );

		final DeleteStatement deleteStatement = new DeleteStatement( tableReference, idRestriction );

		final JdbcDelete jdbcDelete = SqlDeleteToJdbcDeleteConverter.interpret(
				deleteStatement,
				executionContext.getSession().getSessionFactory()
		);

		final JdbcMutationExecutor executor = JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL;
		executor.execute(
				jdbcDelete,
				JdbcParameterBindings.NO_BINDINGS,
				executionContext,
				(rows, preparedStatement) -> {}
		);
	}

	private Predicate generateRestrictionValuesList(
			TableReference tableReference,
			List<Object> ids,
			List<Column> columns,
			Navigable navigable,
			ExecutionContext executionContext) {
		if ( ids.isEmpty() ) {
			return null;
		}

		final InListPredicate restriction;
		if ( columns.size() > 1 ) {
			final List<Expression> keyColumnReferences = CollectionHelper.arrayList( columns.size() );

			for ( Column column : columns ) {
				keyColumnReferences.add(
						tableReference.resolveColumnReference( column ) )
				;
			}

			restriction = new InListPredicate( new SqlTuple( keyColumnReferences, navigable ) );
		}
		else {
			restriction = new InListPredicate(
					tableReference.resolveColumnReference( columns.get( 0 ) )
			);
		}

		final List<Expression> valueListElementExpressions = CollectionHelper.arrayList( columns.size() );

		for ( Object id : ids ) {
			valueListElementExpressions.clear();

			navigable.dehydrate(
					navigable.unresolve( id, executionContext.getSession() ),
					(jdbcValue, type, boundColumn) -> {
						valueListElementExpressions.add( new QueryLiteral( jdbcValue, type, Clause.WHERE ) );
					},
					Clause.WHERE,
					executionContext.getSession()
			);

			if ( columns.size() > 1 ) {
				restriction.addExpression( new SqlTuple( valueListElementExpressions, navigable ) );
			}
			else {
				restriction.addExpression( valueListElementExpressions.get( 0 ) );
			}
		}

		return restriction;
	}
}
