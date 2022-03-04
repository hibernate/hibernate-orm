/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models the commonality between a column and a formula (computed value).
 */
public interface Selectable {
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

	String getTemplate(
			Dialect dialect,
			TypeConfiguration typeConfiguration,
			SqmFunctionRegistry functionRegistry);
}
