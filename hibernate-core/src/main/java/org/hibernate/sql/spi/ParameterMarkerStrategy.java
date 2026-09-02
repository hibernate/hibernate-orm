/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi;

import org.hibernate.SPI;
import org.hibernate.service.Service;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

/// Generates parameter markers for prepared SQL statements.
///
/// A Dialect may supply a stable native-marker strategy when its database and
/// selected driver support or require syntax such as `$n` or `?n`. Return a
/// complete marker for the one-based position and do not retain the supplied
/// JDBC type. Hibernate uses the JDBC-standard `?` strategy when the Dialect
/// supply point returns `null`.
///
/// This contract was originally introduced for reactive PostgreSQL drivers
/// which require native `$n` markers.
///
/// @see org.hibernate.dialect.Dialect#getNativeParameterMarkerStrategy()
/// @see org.hibernate.cfg.AvailableSettings#DIALECT_NATIVE_PARAM_MARKERS
/// @author Steve Ebersole
@SPI({ IMPLEMENT, SUPPLY })
public interface ParameterMarkerStrategy extends Service {
	/// Create the complete marker for a one-based parameter position.
	///
	/// @param position the one-based parameter position
	/// @param jdbcType the known JDBC type, or `null`
	String createMarker(int position, JdbcType jdbcType);
}
