/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.dialect.PostgreSQLDialect}.
 *
 * @author Gavin King
 */
public class PostgreSQLLegacySequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new PostgreSQLLegacySequenceSupport();

	public static final SequenceSupport LEGACY_INSTANCE = new PostgreSQLLegacySequenceSupport() {
		@Override
		public String getDropSequenceString(String sequenceName) {
			return "drop sequence " + sequenceName;
		}
	};

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval('" + sequenceName + "')";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "currval('" + sequenceName + "')";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

}
