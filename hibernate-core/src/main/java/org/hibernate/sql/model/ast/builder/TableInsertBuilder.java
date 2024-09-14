/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
