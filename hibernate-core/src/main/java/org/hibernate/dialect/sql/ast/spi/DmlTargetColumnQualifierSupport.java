/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// The qualifier form a database accepts for references to target columns in
/// update and delete statements.
///
/// A Dialect supplies this value through
/// [org.hibernate.dialect.Dialect#getDmlTargetColumnQualifierSupport]. The
/// translator uses it for native statement rendering; providers should report
/// the least permissive form required by the database.
///
/// @author Marco Belladelli
/// @see org.hibernate.dialect.Dialect#getDmlTargetColumnQualifierSupport()
@SPI({ USE, SUPPLY })
public enum DmlTargetColumnQualifierSupport {
	/// Qualify the column using the table expression, ignoring a possible table
	/// alias.
	TABLE_EXPRESSION,

	/// Qualify the column using the table alias when available, falling back to
	/// the table expression.
	TABLE_ALIAS,

	/// Render the target column without a qualifier.
	NONE
}
