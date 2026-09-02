/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.community.dialect.MaxDBDialect;
import org.hibernate.dialect.sequence.spi.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;

/**
 * Sequence support for {@link MaxDBDialect}.
 *
 * @author Gavin King
 */
public final class MaxDBSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new MaxDBSequenceSupport();

	@Override
	public String getFromDual() {
		return " from dual";
	}

}
