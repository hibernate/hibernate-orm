/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;

/**
 * An OptionalTableUpdate that uses Expectation.OptionalRowCount.
 */
public class OptionalTableUpdateWithOptionalRowCount extends OptionalTableUpdate {

	public OptionalTableUpdateWithOptionalRowCount(OptionalTableUpdate original) {
		super(
				original.getMutatingTable(),
				original.getMutationTarget(),
				original.getValueBindings(),
				original.getKeyBindings(),
				original.getOptimisticLockBindings()
		);
	}

	@Override
	public Expectation getExpectation() {
		return new Expectation.OptionalRowCount();
	}
}
