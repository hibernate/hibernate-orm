/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.literal.spi;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Selects how a zero UTC offset is rendered in a SQL datetime literal.
///
/// Choose the form accepted by the database when composing an offset-bearing
/// literal with [StandardDateTimeLiteralRendering]. This choice does not affect
/// nonzero offsets or literals rendered without an offset.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public enum ZeroOffsetLiteralStyle {
	/// Render a zero offset using the UTC designator `Z`.
	UTC_DESIGNATOR,

	/// Render a zero offset using the numeric form `+00:00`.
	NUMERIC_OFFSET
}
