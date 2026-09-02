/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.RestrictedTableMutation;

import static org.hibernate.SPI.Role.IMPLEMENT;

/**
 * {@link TableMutationBuilder} implementation for {@code update} statements.
 *
 * @author Steve Ebersole
 */
@SPI( IMPLEMENT )
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
