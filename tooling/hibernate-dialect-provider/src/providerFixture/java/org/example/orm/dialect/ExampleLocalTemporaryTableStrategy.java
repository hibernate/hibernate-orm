/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.temptable.spi.StandardLocalTemporaryTableStrategy;

/// Example provider specialization of the supported local temporary-table
/// family strategy.
///
/// @author Steve Ebersole
public final class ExampleLocalTemporaryTableStrategy extends StandardLocalTemporaryTableStrategy {
	public static final ExampleLocalTemporaryTableStrategy INSTANCE = new ExampleLocalTemporaryTableStrategy();

	private ExampleLocalTemporaryTableStrategy() {
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit preserve rows";
	}
}
