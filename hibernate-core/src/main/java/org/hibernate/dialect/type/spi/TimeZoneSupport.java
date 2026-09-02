/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Describes how a database stores SQL types declared `with time zone`.
///
/// Providers select one of these immutable profiles from
/// [org.hibernate.dialect.Dialect#getTimeZoneSupport()]. The profile controls
/// mapping and JDBC type selection; it does not describe temporal-literal
/// syntax or alter the configured storage strategy.
///
/// @author Christian Beikov
/// @author Steve Ebersole
/// @since 8.0
/// @see org.hibernate.dialect.Dialect#getTimeZoneSupport()
@Incubating
@SPI(USE)
public enum TimeZoneSupport {
	/// The `with time zone` types retain the original zone. A round trip
	/// preserves both the represented instant and the zone.
	NATIVE,
	/// The `with time zone` types normalize to UTC. A round trip preserves the
	/// represented instant but returns it in the UTC zone.
	NORMALIZE,
	/// The database has no SQL `with time zone` type support.
	NONE
}
