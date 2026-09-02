/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Selects how an untyped Java `null` is bound through JDBC.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getObjectNullBindingStrategy()
@SPI({ USE, SUPPLY })
public enum ObjectNullBindingStrategy {
	/// Invoke `PreparedStatement.setObject(index, null)`.
	SET_OBJECT,
	/// Invoke `PreparedStatement.setNull(index, Types.NULL)`.
	SET_NULL_WITH_NULL_TYPE,
	/// Resolve the parameter JDBC type and pass it to `setNull`.
	SET_NULL
}
