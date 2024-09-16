/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.TiDBDialect}.
 *
 * @author Cong Wang
 */
public class TiDBSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new TiDBSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval(" + sequenceName + ")";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "lastval(" + sequenceName + ")";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		return getCreateSequenceString( sequenceName )
				+ " start with " + initialValue
				+ " increment by " + incrementSize
				+ startingValue( initialValue, incrementSize );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

}
