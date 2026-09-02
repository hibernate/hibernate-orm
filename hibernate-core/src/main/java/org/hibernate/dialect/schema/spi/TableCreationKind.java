/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the table-creation command selected by schema export.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum TableCreationKind {
	STANDARD,
	MULTISET
}
