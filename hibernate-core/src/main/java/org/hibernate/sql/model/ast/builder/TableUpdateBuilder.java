/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
		extends RestrictedTableMutationBuilder<O, RestrictedTableMutation<O>>, ColumnValuesTableMutationBuilder {

	/**
	 * Convenience form of {@link #addValueColumn(SelectableMapping)} matching the
	 * signature of {@link SelectableConsumer} allowing it to be used as a method reference
	 * in its place.
	 *
	 * @param dummy Ignored; here simply to satisfy the {@link SelectableConsumer} signature
	 *
	 * @see RestrictedTableMutationBuilder#addKeyRestriction(int, SelectableMapping)
	 */
	default void addValueColumn(@SuppressWarnings("unused") int dummy, SelectableMapping selectableMapping) {
		addValueColumn( selectableMapping );
	}

	void setWhere(String fragment);
}
