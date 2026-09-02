/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;

/// Families of built-in entity-locking strategies available to a provider
/// factory.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public enum EntityLockingStrategyKind {
	/// Hibernate's default selection for the requested lock mode.
	STANDARD,
	/// Pessimistic locking expressed through Hibernate's SQL AST.
	SQL_AST,
	/// Locking based on a selecting statement.
	SELECT,
	/// Pessimistic locking based on an updating statement.
	UPDATE
}
