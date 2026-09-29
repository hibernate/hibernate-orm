/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import org.hibernate.SPI;

/// The kind of SQL type identified by a [NamedSqlTypeNamingInput].
/// An implicit naming strategy may use this distinction when composing a name
/// from a dependency's resolved logical or physical name, for example when
/// naming an array whose element is a named enum, array, or struct type.
///
/// @see ArrayNamingInput#namedElement()
///
/// @since 9.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public enum NamedSqlTypeKind {
	/// An independently named SQL enum type, whether textual or ordinal.
	ENUM,
	/// An independently named SQL array type, such as an Oracle VARRAY or nested table.
	/// Anonymous SQL array syntax does not represent a named dependency.
	ARRAY,
	/// A named SQL structured type used to represent an aggregate embeddable.
	STRUCT
}
