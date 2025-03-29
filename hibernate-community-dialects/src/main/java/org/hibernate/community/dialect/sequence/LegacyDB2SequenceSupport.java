/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2Dialect}.
 *
 * @author Gavin King
 */
public class LegacyDB2SequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new LegacyDB2SequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "prevval for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "values " + getSelectSequencePreviousValString( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
