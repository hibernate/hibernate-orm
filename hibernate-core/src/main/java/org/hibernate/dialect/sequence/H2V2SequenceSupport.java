/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.H2Dialect}.
 *
 * @author Gavin King
 */
public final class H2V2SequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new H2V2SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}
}
