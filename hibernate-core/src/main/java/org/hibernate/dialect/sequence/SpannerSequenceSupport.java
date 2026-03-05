/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence;


import org.hibernate.MappingException;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

import org.hibernate.dialect.SpannerDialect;

/**
 * Sequence support for Spanner.
 */
public class SpannerSequenceSupport implements SequenceSupport {

	private final SpannerDialect dialect;

	public SpannerSequenceSupport(SpannerDialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) throws MappingException {
		return getCreateSequenceString(sequenceName, 1, 1);
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		if (incrementSize == 1) {
			return getCreateSequenceString(sequenceName, initialValue, "");
		}
		throw new MappingException("Cloud Spanner does not support sequences with an increment size != 1");
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize, String options)
			throws MappingException {
		if (incrementSize == 1) {
			return new String[] { getCreateSequenceString(sequenceName, initialValue, options) };
		}
		throw new MappingException("Cloud Spanner does not support sequences with an increment size != 1");
	}

	protected String getCreateSequenceString(String sequenceName, int initialValue, String additionalOptions) {
		final var builder = new StringBuilder("create sequence if not exists ");
		builder.append(sequenceName).append(" options(sequence_kind=\"bit_reversed_positive\"");
		if (initialValue != 1) {
			builder.append(", start_with_counter=").append(initialValue);
		}
		if (isNotEmpty(additionalOptions)) {
			builder.append(", ").append(additionalOptions);
		}
		builder.append(")");
		return builder.toString();
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getRestartSequenceString(String sequenceName, long startWith) {
		return "alter sequence " + sequenceName + " set options (start_with_counter = " + startWith + ")";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString(sequenceName);
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		var nextValString = "get_next_sequence_value(sequence " + sequenceName + ")";
		if ( dialect != null && dialect.useIntegerForPrimaryKey() ) {
			return "bit_reverse(" + nextValString + ", true)";
		}
		return nextValString;
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}
}
