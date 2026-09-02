/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;


import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;

/// Provider-owned strategy implementation used to verify an independently
/// implemented Dialect strategy at the assembled-artifact boundary.
///
/// @author Steve Ebersole
public final class ExampleMultiKeyLoadSizingStrategy implements MultiKeyLoadSizingStrategy {
	public static final ExampleMultiKeyLoadSizingStrategy INSTANCE = new ExampleMultiKeyLoadSizingStrategy();

	private ExampleMultiKeyLoadSizingStrategy() {
	}

	@Override
	public int determineOptimalBatchLoadSize(
			int numberOfKeyColumns,
			int numberOfKeys,
			boolean inClauseParameterPaddingEnabled) {
		final int parameterLimitedSize = Math.max( 1, 1000 / Math.max( 1, numberOfKeyColumns ) );
		return Math.min( numberOfKeys, parameterLimitedSize );
	}
}
