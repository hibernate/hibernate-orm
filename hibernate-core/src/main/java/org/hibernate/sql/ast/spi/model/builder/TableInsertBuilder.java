/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model.builder;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.model.TableInsert;

import static org.hibernate.SPI.Role.IMPLEMENT;

/**
 * {@link TableMutationBuilder} implementation for {@code insert} statements.
 *
 * @author Steve Ebersole
 */
@SPI( IMPLEMENT )
public interface TableInsertBuilder
		extends TableMutationBuilder<TableInsert>,
		AssigningTableMutationBuilder<TableInsert>,
		SelectableConsumer {

	boolean hasColumnAssignment(SelectableMapping selectableMapping);

	/**
	 * Allows using the insert builder as selectable consumer.
	 * @see org.hibernate.metamodel.mapping.ValuedModelPart#forEachInsertable(SelectableConsumer)
	 */
	@Override
	default void accept(int selectionIndex, SelectableMapping selectableMapping) {
		addValueColumn( selectableMapping );
	}
}
