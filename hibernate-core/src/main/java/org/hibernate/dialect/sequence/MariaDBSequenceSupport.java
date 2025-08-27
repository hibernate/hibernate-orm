/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.MariaDBDialect}.
 *
 * @author Christian Beikov
 */
public final class MariaDBSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new MariaDBSequenceSupport();

	@Override
	public String getCreateSequenceString(String sequenceName) throws MappingException {
		return "create sequence " + sequenceName + " nocache";
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		return "create sequence " + sequenceName
				+ startingValue( initialValue, incrementSize )
				+ " start with " + initialValue
				+ " increment by " + incrementSize
				+ " nocache";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "previous value for " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}
}
