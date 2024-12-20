/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.AltibaseDialect}.
 *
 * @author Geoffrey park
 */
public class AltibaseSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new AltibaseSequenceSupport();

	@Override
	public String getFromDual() {
		return " from dual";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
