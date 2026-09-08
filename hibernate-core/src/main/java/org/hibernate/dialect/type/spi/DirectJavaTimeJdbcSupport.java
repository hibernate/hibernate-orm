/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes direct JDBC access support for Java Time classes.
///
/// A supported class may be passed directly to JDBC through
/// `PreparedStatement.setObject()` and requested from JDBC through
/// `ResultSet.getObject()` and the corresponding callable-statement methods.
/// Support therefore covers both binding and extraction.
///
/// [java.time.OffsetTime] and [java.time.OffsetDateTime] are a matched pair:
/// an implementation must return the same answer for both classes.
///
/// Implementations must be stable and thread-safe.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getDirectJavaTimeJdbcSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface DirectJavaTimeJdbcSupport {
	/// Whether the exact Java Time class may be used directly at the JDBC boundary.
	/// Unknown classes are unsupported.
	boolean supports(Class<?> javaTimeType);
}
