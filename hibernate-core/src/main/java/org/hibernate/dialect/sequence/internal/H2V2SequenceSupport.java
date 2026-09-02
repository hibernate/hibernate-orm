/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.internal;

import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.ANSISequenceSupport;

/**
 * Sequence support for {@link org.hibernate.dialect.H2Dialect}.
 *
 * @author Gavin King
 */
public final class H2V2SequenceSupport extends ANSISequenceSupport {

	private static final SequenceSupport INSTANCE = new H2V2SequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}
}
