/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.DB2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.DerbyDialect}.
 *
 * @author Christian Beikov
 */
public final class DerbySequenceSupport extends DB2SequenceSupport {

	public static final SequenceSupport INSTANCE = new DerbySequenceSupport();

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "SYSCS_UTIL.SYSCS_PEEK_AT_SEQUENCE('HIBERNATE_ORM_TEST','" + sequenceName.toUpperCase() + "')";
	}
}
