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

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * Strategy (pattern) for producing the restriction used when mutating a
 * particular table in the group of tables
 *
 * @author Steve Ebersole
 */
public interface MatchingIdRestrictionProducer {
	/**
	 * Produce the restriction predicate
	 *
	 * @param matchingIdValues The matching id values.
	 * @param mutatingTableReference The TableReference for the table being mutated
	 * @param columnsToMatchVisitationSupplier The columns against which to restrict the mutations
	 */
	Predicate produceRestriction(
			List<?> matchingIdValues,
			EntityMappingType entityDescriptor,
			int valueIndex,
			ModelPart valueModelPart,
			TableReference mutatingTableReference,
			Supplier<Consumer<SelectableConsumer>> columnsToMatchVisitationSupplier,
			ExecutionContext executionContext);
}
