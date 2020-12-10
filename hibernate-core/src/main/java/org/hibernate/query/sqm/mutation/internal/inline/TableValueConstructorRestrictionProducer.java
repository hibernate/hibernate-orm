/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * MatchingIdRestrictionProducer producing a restriction based on a SQL table-value-constructor.  E.g.:
 *
 * ````
 * delete
 * from
 *     entity-table
 * where
 *     ( id ) in (
 *         select
 *             id
 *         from (
 *             values
 *                 ( 1 ),
 *                 ( 2 ),
 *                 ( 3 ),
 *                 ( 4 )
 *             ) as HT (id)
 *     )
 * ````
 *
 * @author Vlad Mihalcea
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class TableValueConstructorRestrictionProducer implements MatchingIdRestrictionProducer {
	@Override
	public InSubQueryPredicate produceRestriction(
			List<?> matchingIdValues,
			EntityMappingType entityDescriptor,
			TableReference mutatingTableReference,
			Supplier<Consumer<SelectionConsumer>> columnsToMatchVisitationSupplier,
			ExecutionContext executionContext) {
		// Not "yet" implemented.  Not sure we will.  This requires the ability to define
		// "in-line views" with a table-ctor which the SQL AST does not yet define support for
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
