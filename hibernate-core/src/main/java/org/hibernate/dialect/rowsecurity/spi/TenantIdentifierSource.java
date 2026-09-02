/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the database-side source consulted by a row-level-security
/// policy for the current tenant identifier.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum TenantIdentifierSource {
	/// The strategy stores the tenant identifier in database session state by
	/// way of [RowLevelSecurity#setTenantIdentifier].
	SESSION,

	/// The policy obtains the tenant identifier from the database's current-user
	/// expression.
	DATABASE_USER
}
