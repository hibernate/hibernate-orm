/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.internal;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

/// Built-in standard current-temporal support.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardCurrentTemporalSupport implements CurrentTemporalSupport {
	private static final CurrentTemporalSupport INSTANCE = new StandardCurrentTemporalSupport();

	private StandardCurrentTemporalSupport() {
	}

	public static CurrentTemporalSupport instance() {
		return INSTANCE;
	}
}
