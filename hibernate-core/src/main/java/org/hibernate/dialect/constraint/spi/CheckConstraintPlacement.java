/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies where a check constraint is declared.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum CheckConstraintPlacement {
	ANONYMOUS_COLUMN,
	NAMED_COLUMN,
	TABLE
}
