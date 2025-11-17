/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for dialects which support the common
 * Oracle-style syntax {@code seqname.nextval}.
 */
public class NextvalSequenceSupport implements SequenceSupport {

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public final String getSelectSequencePreviousValString(String sequenceName) {
		return sequenceName + ".currval";
	}

}
