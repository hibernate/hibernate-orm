/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2iDialect}.
 *
 * @author Christian Beikov
 */
public class DB2iSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new DB2iSequenceSupport();

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
