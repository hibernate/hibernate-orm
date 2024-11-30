/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBindingList;
import org.hibernate.sql.model.ast.RestrictedTableMutation;

/**
 * Specialized {@link TableMutationBuilder} implementation for building mutations
 * which have a {@code where} clause.
 *
 * Common operations of {@link TableUpdateBuilder} and {@link TableDeleteBuilder}.
 *
 * @author Steve Ebersole
 */
public interface RestrictedTableMutationBuilder<O extends MutationOperation, M extends RestrictedTableMutation<O>> extends TableMutationBuilder<M> {

	/**
	 * Add a restriction as long as the selectable is not a formula and is not nullable
	 */
	default void addKeyRestrictions(SelectableMappings selectableMappings) {
		final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
		for ( int i = 0; i < jdbcTypeCount; i++ ) {
			addKeyRestriction( selectableMappings.getSelectable( i ) );
		}
	}

	/**
	 * Add a restriction as long as the selectable is not a formula and is not nullable
	 */
	default void addKeyRestrictionsLeniently(SelectableMappings selectableMappings) {
		final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
		for ( int i = 0; i < jdbcTypeCount; i++ ) {
			addKeyRestrictionLeniently( selectableMappings.getSelectable( i ) );
		}
	}

	/**
	 * Add restriction based on non-version optimistically-locked column
	 */
	default void addOptimisticLockRestrictions(SelectableMappings selectableMappings) {
		final int jdbcTypeCount = selectableMappings.getJdbcTypeCount();
		for ( int i = 0; i < jdbcTypeCount; i++ ) {
			addOptimisticLockRestriction( selectableMappings.getSelectable( i ) );
		}
	}

	/**
	 * Add a restriction as long as the selectable is not a formula and is not nullable
	 */
	default void addKeyRestriction(SelectableMapping selectableMapping){
		if ( selectableMapping.isNullable() ) {
			return;
		}
		addKeyRestrictionLeniently( selectableMapping );
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

	void addLiteralRestriction(String columnName, String sqlLiteralText, JdbcMapping jdbcMapping);

	ColumnValueBindingList getKeyRestrictionBindings();

	ColumnValueBindingList getOptimisticLockBindings();

	void setWhere(String fragment);

	void addWhereFragment(String fragment);
}
