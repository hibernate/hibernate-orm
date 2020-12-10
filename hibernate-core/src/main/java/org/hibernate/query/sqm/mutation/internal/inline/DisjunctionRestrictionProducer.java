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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * MatchingIdRestrictionProducer producing a restriction based on a disjunction (OR) predicate.  E.g.:
 *
 * ````
 * delete
 * from
 *     entity-table
 * where
 *     ( id = 1 )
 *     or ( id = 2 )
 *     or ( id = 3 )
 *     or ( id = 4 )
 * ````
 *
 * @author Steve Ebersole
 */
public class DisjunctionRestrictionProducer implements MatchingIdRestrictionProducer {
	@Override
	public Junction produceRestriction(
			List<?> matchingIdValues,
			EntityMappingType entityDescriptor,
			TableReference mutatingTableReference,
			Supplier<Consumer<SelectionConsumer>> columnsToMatchVisitationSupplier,
			ExecutionContext executionContext) {
		assert matchingIdValues != null;
		assert ! matchingIdValues.isEmpty();

		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final int idColumnCount = identifierMapping.getJdbcTypeCount();
		assert idColumnCount > 0;

		final Junction predicate = new Junction( Junction.Nature.DISJUNCTION );

		if ( idColumnCount == 1 ) {
			final BasicValuedModelPart basicIdMapping = (BasicValuedModelPart) identifierMapping;
			final String idColumn = basicIdMapping.getSelectionExpression();
			final ColumnReference idColumnReference = new ColumnReference(
					mutatingTableReference,
					idColumn,
					// id columns cannot be formulas and cannot have custom read and write expressions
					false,
					null,
					null,
					basicIdMapping.getJdbcMapping(),
					sessionFactory
			);

			matchingIdValues.forEach(
					matchingId -> predicate.add(
							new ComparisonPredicate(
									idColumnReference,
									ComparisonOperator.EQUAL,
									new JdbcLiteral<>( matchingId, basicIdMapping.getJdbcMapping() )
							)
					)
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( idColumnCount );
			final List<JdbcMapping> jdbcMappings = new ArrayList<>( idColumnCount );
			identifierMapping.forEachSelection(
					(columnIndex, selection) -> {
						columnReferences.add(
								new ColumnReference(
										mutatingTableReference,
										selection,
										sessionFactory
								)
						);
						jdbcMappings.add( selection.getJdbcMapping() );
					}
			);

			matchingIdValues.forEach(
					matchingId -> {
						final Junction idMatch = new Junction( Junction.Nature.CONJUNCTION );

						assert matchingId instanceof Object[];

						final Object[] matchingIdParts = (Object[]) matchingId;

						for ( int p = 0; p < matchingIdParts.length; p++ ) {
							idMatch.add(
									new ComparisonPredicate(
											columnReferences.get( p ),
											ComparisonOperator.EQUAL,
											new JdbcLiteral<>( matchingIdParts[ p ], jdbcMappings.get( p ) )
									)
							);
						}

						predicate.add( idMatch );
					}
			);
		}

		return predicate;
	}
}
