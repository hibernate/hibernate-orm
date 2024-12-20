/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.TableInsert;

/**
 * {@link TableMutationBuilder} implementation for {@code insert} statements.
 *
 * @author Steve Ebersole
 */
public interface TableInsertBuilder extends TableMutationBuilder<TableInsert>, ColumnValuesTableMutationBuilder,
		SelectableConsumer {


	/**
	 * Allows using the insert builder as selectable consumer.
	 * @see org.hibernate.metamodel.mapping.ValuedModelPart#forEachInsertable(SelectableConsumer)
	 */
	@Override
	default void accept(int selectionIndex, SelectableMapping selectableMapping) {
		addValueColumn( selectableMapping );
	}
}
