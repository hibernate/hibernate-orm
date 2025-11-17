/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2Dialect}.
 *
 * @author Gavin King
 */
public class DB2SequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new DB2SequenceSupport();

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "previous value for " + sequenceName;
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) {
		return "values " + getSelectSequencePreviousValString( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
