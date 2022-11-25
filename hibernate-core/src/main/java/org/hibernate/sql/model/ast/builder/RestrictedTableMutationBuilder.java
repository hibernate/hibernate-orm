/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * Specialized TableMutationBuilder implementation for building mutations
 * which define a where-clause
 *
 * @author Steve Ebersole
 */
public interface RestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>> extends TableMutationBuilder<M> {
	/**
	 * Add a restriction as long as the selectable is not a formula and is not nullable
	 */
	default void addKeyRestriction(SelectableMapping selectableMapping) {
		if ( selectableMapping.isNullable() ) {
			return;
		}
		addKeyRestrictionLeniently( selectableMapping );
	}

	/**
	 * Convenience form of {@link #addKeyRestriction(SelectableMapping)} matching the
	 * signature of {@link SelectableConsumer} allowing it to be used as a method reference
	 * in its place.
	 *
	 * @param dummy Ignored; here simply to satisfy the {@link SelectableConsumer} signature
	 */
	default void addKeyRestriction(@SuppressWarnings("unused") int dummy, SelectableMapping selectableMapping) {
		addKeyRestriction( selectableMapping );
	}

	/**
	 * Add a restriction as long as the selectable is not a formula
	 */
	default void addKeyRestrictionLeniently(SelectableMapping selectableMapping) {
		if ( selectableMapping.isFormula() ) {
			return;
		}
		addKeyRestriction(
				selectableMapping.getSelectionExpression(),
				selectableMapping.getWriteExpression(),
				selectableMapping.getJdbcMapping()
		);
	}

	/**
	 * Add restriction based on the column in the table's key
	 */
	void addKeyRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping);

	void addNullOptimisticLockRestriction(SelectableMapping column);

	/**
	 * Add restriction based on non-version optimistically-locked column
	 */
	default void addOptimisticLockRestriction(SelectableMapping selectableMapping) {
		addOptimisticLockRestriction(
				selectableMapping.getSelectionExpression(),
				selectableMapping.getWriteExpression(),
				selectableMapping.getJdbcMapping()
		);
	}

	/**
	 * Add restriction based on non-version optimistically-locked column
	 */
	void addOptimisticLockRestriction(String columnName, String columnWriteFragment, JdbcMapping jdbcMapping);

	void setWhere(String fragment);

	void addWhereFragment(String fragment);
}
