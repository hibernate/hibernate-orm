/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * {@link TableMutationBuilder} implementation for {@code update} statements.
 *
 * @author Steve Ebersole
 */
public interface TableUpdateBuilder<O extends MutationOperation>
		extends RestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>, ColumnValuesTableMutationBuilder, SelectableConsumer {

	/**
	 * Allows using the update builder as selectable consumer.
	 * @see org.hibernate.metamodel.mapping.ValuedModelPart#forEachUpdatable(SelectableConsumer)
	 */
	@Override
	default void accept(int selectionIndex, SelectableMapping selectableMapping) {
		addValueColumn( selectableMapping );
	}

	void setWhere(String fragment);
}
