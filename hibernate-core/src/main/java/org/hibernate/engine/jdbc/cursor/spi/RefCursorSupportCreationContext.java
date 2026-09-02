/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.cursor.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Creation-only context for selecting and constructing a REF_CURSOR access
/// strategy.
///
/// A factory must not retain this context after it returns. A created support
/// may retain it solely to convert runtime `SQLException`s with
/// [#convert(SQLException, String)].
/// Do not use it to repeat strategy selection after service creation.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public interface RefCursorSupportCreationContext {
	/// Whether the effective JDBC metadata permits the standard REF_CURSOR API.
	boolean supportsStandardRefCursors();

	/// Convert a JDBC failure through Hibernate's configured exception helper.
	///
	/// Return the helper's [JDBCException] unchanged.
	JDBCException convert(SQLException exception, String message);
}
