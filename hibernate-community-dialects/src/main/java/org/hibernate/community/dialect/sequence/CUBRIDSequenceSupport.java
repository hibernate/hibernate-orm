/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.CUBRIDDialect}.
 *
 * @author Gavin King
 */
public final class CUBRIDSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new CUBRIDSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".next_value";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) {
		return sequenceName + ".current_value";
	}

	@Override
	public String getFromDual() {
		//TODO: is this really needed?
		//TODO: would " from db_root" be better?
		return " from table({1}) as T(X)";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create serial " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop serial " + sequenceName;
	}
}
