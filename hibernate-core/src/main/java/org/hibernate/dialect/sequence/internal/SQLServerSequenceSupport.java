/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sequence.internal;

import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sequence.spi.ANSISequenceSupport;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.SQLServerDialect}.
 *
 * @author Christian Beikov
 */
public class SQLServerSequenceSupport extends ANSISequenceSupport {

	private static final SequenceSupport INSTANCE = new SQLServerSequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "select convert(varchar(200),current_value) from sys.sequences where name='" + sequenceName + "'";
	}
}
