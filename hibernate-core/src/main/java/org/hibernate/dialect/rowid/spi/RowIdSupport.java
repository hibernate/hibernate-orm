/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowid.spi;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines row-locator expression, JDBC type, and optional physical-column
/// declaration behavior.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getRowIdSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface RowIdSupport {
	/// Whether row-locator mapping is supported.
	boolean isSupported();

	/// Resolve the SQL row-locator expression from the optional mapping name.
	@Nullable String resolveExpression(@Nullable String requestedName);

	/// Return the JDBC type code of the row-locator expression.
	int sqlTypeCode();

	/// Resolve the physical column definition, or `null` for an implicit
	/// pseudo-column or an unresolved requested name.
	@Nullable String columnDefinition(@Nullable String requestedName);
}
