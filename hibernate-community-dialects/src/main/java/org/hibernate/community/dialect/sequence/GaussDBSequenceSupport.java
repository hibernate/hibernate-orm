/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link GaussDBDialect}.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLAggregateSupport.
 */
public class GaussDBSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new GaussDBSequenceSupport();

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
