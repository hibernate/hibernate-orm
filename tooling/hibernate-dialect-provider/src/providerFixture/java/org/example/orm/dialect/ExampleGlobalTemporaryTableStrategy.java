/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;

/// Example provider specialization of the supported global temporary-table
/// family strategy.
///
/// @author Steve Ebersole
public final class ExampleGlobalTemporaryTableStrategy extends StandardGlobalTemporaryTableStrategy {
	public static final ExampleGlobalTemporaryTableStrategy INSTANCE = new ExampleGlobalTemporaryTableStrategy();

	private ExampleGlobalTemporaryTableStrategy() {
	}

	@Override
	public boolean supportsTemporaryTablePrimaryKey() {
		return false;
	}
}
