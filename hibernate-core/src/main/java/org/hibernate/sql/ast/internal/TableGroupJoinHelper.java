/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;

public class TableGroupJoinHelper {

	/**
	 * Determine the {@link TableGroupJoin} to which a custom {@code ON} clause predicate should be applied to.
	 * This is supposed to be called right after construction of a {@link TableGroupJoin}.
	 * This should also be called after a {@link org.hibernate.query.sqm.tree.predicate.SqmPredicate} is translated to a
	 * {@link org.hibernate.sql.ast.tree.predicate.Predicate}, because that translation might cause nested joins to be
	 * added to the table group of the join.
	 */
	public static TableGroupJoin determineJoinForPredicateApply(TableGroupJoin mainTableGroupJoin) {
		final TableGroup mainTableGroup = mainTableGroupJoin.getJoinedGroup();
		if ( !mainTableGroup.getNestedTableGroupJoins().isEmpty() || mainTableGroup.getTableGroupJoins().isEmpty() ) {
			// Always apply a predicate on the main table group join if it has nested table group joins or no joins
			return mainTableGroupJoin;
		}
		else {
			// If the main table group has just regular table group joins,
			// prefer to apply predicates on the last table group join
			final TableGroupJoin lastTableGroupJoin = mainTableGroup.getTableGroupJoins()
					.get( mainTableGroup.getTableGroupJoins().size() - 1 );
			if ( lastTableGroupJoin.getJoinedGroup().getModelPart() instanceof EmbeddedCollectionPart ) {
				// If the table group join refers to an embedded collection part,
				// then the underlying table group *is* the main table group.
				// Applying predicates on the join referring to the virtual table group would be a problem though,
				// because these predicates will never be rendered. So use the main table group join in that case
				assert lastTableGroupJoin.getJoinedGroup() instanceof VirtualTableGroup
						&& ( (VirtualTableGroup) lastTableGroupJoin.getJoinedGroup() ).getUnderlyingTableGroup() == mainTableGroup;
				return mainTableGroupJoin;
			}
			return lastTableGroupJoin;
		}
	}
}
