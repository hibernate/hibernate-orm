/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Renders the SQL fragment used with a table-reference fragment to request a
/// pessimistic lock. The translator owns placement of the fragment. For an
/// ordinary named table reference, it is appended after the identification
/// variable (table alias); union-table references may require the fragment to
/// be pushed into each physical union member.
///
/// The returned fragment should include any leading whitespace required to
/// separate it from the preceding SQL. An empty fragment indicates that no
/// table lock hint is required.
///
/// @see LockingSupport#getTableLockHintRenderer()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface TableLockHintRenderer {
	/// A renderer for databases which do not use table lock hints.
	TableLockHintRenderer NONE = request -> "";

	/// Render the table lock hint for the given request.
	String render(TableLockHintRequest request);
}
