/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.TimesTenDialect}.
 *
 * @author Gavin King
 */
public final class TimesTenSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new TimesTenSequenceSupport();

	@Override
	public String getFromDual() {
		return " from sys.dual";
	}

}
