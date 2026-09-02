/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies where an existence check occurs relative to a DDL target name.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum ExistenceCheckPlacement {
	/// The DDL command has no existence check.
	NONE,
	/// The existence check immediately precedes the target name.
	BEFORE_NAME,
	/// The existence check immediately follows the target name.
	AFTER_NAME
}
