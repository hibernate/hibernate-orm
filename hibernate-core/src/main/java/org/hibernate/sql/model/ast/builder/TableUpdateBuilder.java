/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * {@link TableMutationBuilder} implementation for {@code update} statements.
 *
 * @author Steve Ebersole
 */
public interface TableUpdateBuilder<O extends MutationOperation>
		extends RestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>,
		AssigningTableMutationBuilder<RestrictedTableMutation<O>>,
		SelectableConsumer {

	/**
	 * Allows using the update builder as selectable consumer.
	 * @see org.hibernate.metamodel.mapping.ValuedModelPart#forEachUpdatable(SelectableConsumer)
	 */
	@Override
	default void accept(int selectionIndex, SelectableMapping selectableMapping) {
		addColumnAssignment( selectableMapping );
	}

	void setWhere(String fragment);

	@Override
	LogicalTableUpdate<O> buildMutation();
}
