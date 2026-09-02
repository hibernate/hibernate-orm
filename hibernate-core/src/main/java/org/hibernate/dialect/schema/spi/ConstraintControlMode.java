/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies how table cleaning controls referential constraints.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum ConstraintControlMode {
	NONE,
	GLOBAL,
	PER_CONSTRAINT
}
