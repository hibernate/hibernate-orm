/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.unique.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the database object used for a logical unique declaration.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum UniqueKeyRepresentation {
	CONSTRAINT,
	INDEX
}
