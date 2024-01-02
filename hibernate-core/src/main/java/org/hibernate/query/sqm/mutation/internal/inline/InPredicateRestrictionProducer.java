/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.exec.spi.ExecutionContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * MatchingIdRestrictionProducer producing a restriction based on an in-values-list predicate.  E.g.:
 *
 * ````
 * delete
 * from
 *     entity-table
 * where
 *     ( id ) in (
 *         ( 1 ),
 *         ( 2 ),
 *         ( 3 ),
 *         ( 4 )
 *     )
 * ````
 *
 * @author Steve Ebersole
 */
public class InPredicateRestrictionProducer implements MatchingIdRestrictionProducer {

	@Override
	public List<Expression> produceIdExpressionList(List<Object> idsAndFks, EntityMappingType entityDescriptor) {
		final List<Expression> inListExpressions = new ArrayList<>( idsAndFks.size() );
		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		if ( identifierMapping instanceof BasicValuedModelPart ) {
			final BasicValuedModelPart basicValuedModelPart = (BasicValuedModelPart) identifierMapping;
			for ( int i = 0; i < idsAndFks.size(); i++ ) {
				inListExpressions.add( new QueryLiteral<>( idsAndFks.get( i ), basicValuedModelPart ) );
			}
		}
		else {
			final int jdbcTypeCount = identifierMapping.getJdbcTypeCount();
			for ( int i = 0; i < idsAndFks.size(); i++ ) {
				final Object[] id = (Object[]) idsAndFks.get( i );
				final List<Expression> tupleElements = new ArrayList<>( jdbcTypeCount );
				inListExpressions.add( new SqlTuple( tupleElements, identifierMapping ) );
				identifierMapping.forEachJdbcType( (index, jdbcMapping) -> {
					tupleElements.add( new QueryLiteral<>( id[index], (BasicValuedMapping) jdbcMapping ) );
				} );
			}
		}
		return inListExpressions;
	}

	@Override
	public InListPredicate produceRestriction(
			List<Expression> matchingIdValueExpressions,
			EntityMappingType entityDescriptor,
			int valueIndex,
			ModelPart valueModelPart,
			TableReference mutatingTableReference,
			Supplier<Consumer<SelectableConsumer>> columnsToMatchVisitationSupplier,
			ExecutionContext executionContext) {
		assert matchingIdValueExpressions != null;
		assert ! matchingIdValueExpressions.isEmpty();

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final int idColumnCount = identifierMapping.getJdbcTypeCount();
		assert idColumnCount > 0;

		final InListPredicate predicate;

		if ( idColumnCount == 1 ) {
			final BasicValuedModelPart basicIdMapping = castNonNull( identifierMapping.asBasicValuedModelPart() );
			final String idColumn = basicIdMapping.getSelectionExpression();
			final Expression inFixture = new ColumnReference(
					mutatingTableReference,
					idColumn,
					// id columns cannot be formulas and cannot have custom read and write expressions
					false,
					null,
					basicIdMapping.getJdbcMapping()
			);
			predicate = new InListPredicate( inFixture, matchingIdValueExpressions );
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( idColumnCount );
			final SelectableConsumer selectableConsumer = (columnIndex, selection) -> {
				columnReferences.add(
						new ColumnReference(
								mutatingTableReference,
								selection
						)
				);
			};
			if ( columnsToMatchVisitationSupplier == null ) {
				identifierMapping.forEachSelectable( selectableConsumer );
			}
			else {
				columnsToMatchVisitationSupplier.get().accept( selectableConsumer );
			}

			final Expression inFixture = new SqlTuple( columnReferences, identifierMapping );
			predicate = new InListPredicate( inFixture, matchingIdValueExpressions );
		}

		return predicate;
	}
}
