/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

/// Supplies the stable standard aggregate profile.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class AggregateSupports {
	private static final AggregateSupport STANDARD = new StandardAggregateSupport();

	private AggregateSupports() {
	}

	/// Obtain the shared standard aggregate profile.
	///
	/// @return the stable standard profile
	public static AggregateSupport standard() {
		return STANDARD;
	}
}
