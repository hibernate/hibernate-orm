/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * ANSI SQL compliant sequence support, for dialects which
 * support the ANSI SQL syntax {@code next value for seqname}.
 *
 * @author Gavin King
 */
public class ANSISequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new ANSISequenceSupport();

	@Override
	public final String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "current value for " + sequenceName;
	}
}
