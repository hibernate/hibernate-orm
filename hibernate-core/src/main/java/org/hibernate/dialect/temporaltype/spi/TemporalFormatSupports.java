/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.temporaltype.internal.StandardTemporalFormatSupport;

import static org.hibernate.SPI.Role.USE;

/// Access to Hibernate's standard temporal-format translation.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class TemporalFormatSupports {
	private TemporalFormatSupports() {}

	/// Return the shared standard support.
	public static TemporalFormatSupport standard() {
		return StandardTemporalFormatSupport.instance();
	}
}
