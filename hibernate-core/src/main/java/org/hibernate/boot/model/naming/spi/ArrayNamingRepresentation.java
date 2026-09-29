/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

/// The representation of an independently named SQL array object.
/// Exposed through [ArrayNamingInput] so custom implicit naming strategies may
/// distinguish array representations. The default naming convention is the same
/// for both representations. Anonymous SQL array syntax is not represented here.
///
/// @see ArrayNamingInput#representation()
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public enum ArrayNamingRepresentation {
	/// An Oracle varying array (`VARRAY`) type, declared with a maximum capacity.
	VARRAY,
	/// An Oracle nested-table (`TABLE OF`) type, declared without a maximum capacity.
	NESTED_TABLE
}
