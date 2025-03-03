/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.ANSISequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.IngresDialect}.
 */
public final class IngresLegacySequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new IngresLegacySequenceSupport();

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}
}
