/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.sequence.spi.SequenceSupport;

/// Provider-owned sequence grammar used by the standalone Dialect fixture.
///
/// @author Steve Ebersole
public final class ExampleSequenceSupport implements SequenceSupport {
	static final ExampleSequenceSupport INSTANCE = new ExampleSequenceSupport();

	private ExampleSequenceSupport() {
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "fixture_next('" + sequenceName + "')";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) {
		return "fixture_current('" + sequenceName + "')";
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		return "create fixture sequence " + sequenceName
				+ " start " + initialValue
				+ " step " + incrementSize;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop fixture sequence " + sequenceName;
	}
}
