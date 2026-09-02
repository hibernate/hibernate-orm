/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// How a SQL AST translator refers to a select item from a `group by` clause.
///
/// Supply the least database-specific form which preserves the rendered select
/// semantics. The translator defaults to repeating the expression.
///
/// @author Christian Beikov
/// @see AbstractSqlAstTranslator#getGroupBySelectItemReferenceStrategy()
@SPI({ USE, SUPPLY })
public enum SelectItemReferenceStrategy {
	/// Render the selected expression again.
	EXPRESSION,
	/// Refer to the item by its select alias.
	ALIAS,
	/// Refer to the item by its one-based select-list position.
	POSITION
}
