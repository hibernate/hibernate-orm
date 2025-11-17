/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.dialect.H2Dialect}.
 *
 * @author Gavin King
 */
public final class H2V1SequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new H2V1SequenceSupport();

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return sequenceName + ".currval";
	}
}
