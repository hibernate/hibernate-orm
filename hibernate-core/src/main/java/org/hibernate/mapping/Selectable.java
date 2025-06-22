/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.Incubating;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models the commonality between a {@link Column} and a {@link Formula} (computed value).
 */
public sealed interface Selectable permits Column, Formula {
	/**
	 * The selectable's "canonical" text representation
	 */
	String getText();

	/**
	 * The selectable's text representation accounting for the Dialect's
	 * quoting, if quoted
	 */
	String getText(Dialect dialect);

	/**
	 * Does this selectable represent a formula?  {@code true} indicates
	 * it is a formula; {@code false} indicates it is a physical column
	 */
	boolean isFormula();

	/**
	 * Any custom read expression for this selectable.  Only pertinent
	 * for physical columns (not formulas)
	 *
	 * @see org.hibernate.annotations.ColumnTransformer
	 */
	String getCustomReadExpression();

	/**
	 * Any custom write expression for this selectable.  Only pertinent
	 * for physical columns (not formulas)
	 *
	 * @see org.hibernate.annotations.ColumnTransformer
	 */
	String getCustomWriteExpression();

	/**
	 * @deprecated new read-by-position paradigm means that these generated
	 * aliases are no longer needed
	 */
	@Deprecated(since = "6.0")
	String getAlias(Dialect dialect);

	/**
	 * @deprecated new read-by-position paradigm means that these generated
	 * aliases are no longer needed
	 */
	@Deprecated(since = "6.0")
	String getAlias(Dialect dialect, Table table);

	String getTemplate(Dialect dialect, TypeConfiguration typeConfiguration);

	@Incubating
	default String getWriteExpr() {
		final String customWriteExpression = getCustomWriteExpression();
		return customWriteExpression == null || customWriteExpression.isEmpty()
				? "?"
				: customWriteExpression;
	}

	@Incubating
	default String getWriteExpr(JdbcMapping jdbcMapping, Dialect dialect) {
		return jdbcMapping.getJdbcType().wrapWriteExpression( getWriteExpr(), dialect );
	}
}
