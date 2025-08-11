/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.annotations.ColumnTransformer;

/**
 * Mapping of a selectable (column/formula)
 *
 * @author Christian Beikov
 */
@Incubating
public interface SelectableMapping extends SqlTypedMapping {
	/**
	 * The name of the table to which this selectable is mapped
	 */
	String getContainingTableExpression();

	/**
	 * The selection's expression.  This is the column name or formula
	 */
	String getSelectionExpression();
	default String getSelectableName() {
		return getSelectionExpression();
	}
	default SelectablePath getSelectablePath() {
		return new SelectablePath( getSelectableName() );
	}

	/**
	 * The selection's read expression accounting for formula treatment as well
	 * as {@link ColumnTransformer#read()}
	 */
	@Nullable String getCustomReadExpression();

	/**
	 * The selection's write expression accounting {@link ColumnTransformer#write()}
	 *
	 * @apiNote Always null for formula mappings
	 */
	@Nullable String getCustomWriteExpression();

	default String getWriteExpression() {
		final String customWriteExpression = getCustomWriteExpression();
		return customWriteExpression != null
				? customWriteExpression
				: "?";
	}

	/**
	 * Is the mapping a formula instead of a physical column?
	 */
	boolean isFormula();

	/**
	 * Is the mapping considered nullable?
	 */
	boolean isNullable();

	boolean isInsertable();

	boolean isUpdateable();

	boolean isPartitioned();
}
