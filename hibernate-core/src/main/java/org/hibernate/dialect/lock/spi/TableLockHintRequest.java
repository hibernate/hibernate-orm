/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import jakarta.persistence.Timeout;

import org.hibernate.SPI;

/// Read-only context for rendering a lock hint for a table-expression
/// fragment. The expression identifies the physical table being locked; it
/// does not determine where the rendered hint is placed relative to an alias.
///
/// @see TableLockHintRenderer
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public interface TableLockHintRequest {
	/// The focused pessimistic lock kind.
	PessimisticLockKind lockKind();

	/// The effective lock timeout.
	Timeout timeout();

	/// The physical table-expression fragment for which the hint is rendered.
	String tableExpression();
}
