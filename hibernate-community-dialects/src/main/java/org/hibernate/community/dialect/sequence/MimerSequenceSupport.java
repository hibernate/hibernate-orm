/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.community.dialect.MimerSQLDialect;
import org.hibernate.dialect.sequence.ANSISequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link MimerSQLDialect}.
 *
 * @author Gavin King
 */
public class MimerSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new MimerSequenceSupport();

	@Override
	public String getFromDual() {
		return " from system.onerow";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
