/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temporaltype.internal;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

/// Built-in standard temporal-operation support.
///
/// @author Steve Ebersole
/// @since 8.0
public final class StandardTemporalOperationSupport implements TemporalOperationSupport {
	private static final TemporalOperationSupport INSTANCE = new StandardTemporalOperationSupport();

	private StandardTemporalOperationSupport() {
	}

	public static TemporalOperationSupport instance() {
		return INSTANCE;
	}
}
