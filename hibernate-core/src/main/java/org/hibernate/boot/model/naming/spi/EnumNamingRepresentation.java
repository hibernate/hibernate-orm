/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

/// The representation of a separately named SQL enum.
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public enum EnumNamingRepresentation {
	TEXTUAL,
	ORDINAL
}
