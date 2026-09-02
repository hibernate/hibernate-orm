/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// The ordering of null values used by a Dialect's
/// [NullOrderingSupport#getDefaultOrdering] profile value.
///
/// @since 8.0
/// @author Christian Beikov
@SPI(USE)
public enum NullOrdering {
	/// Null is treated as the smallest value.
	SMALLEST,

	/// Null is treated as the greatest value.
	GREATEST,

	/// Null is always ordered first.
	FIRST,

	/// Null is always ordered last.
	LAST
}
