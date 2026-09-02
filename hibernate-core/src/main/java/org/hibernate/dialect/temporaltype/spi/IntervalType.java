/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Identifies the native SQL interval family supplied to temporal arithmetic.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public enum IntervalType {
	/// A day-to-second interval.
	SECOND
}
