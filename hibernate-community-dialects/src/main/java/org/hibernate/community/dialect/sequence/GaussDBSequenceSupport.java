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

	/**
	 * GaussDB's {@code ALTER SEQUENCE} only supports {@code MAXVALUE}, {@code CACHE} and {@code OWNER},
	 * so the {@code RESTART WITH} syntax is not supported.
	 * Use {@code setval()} function instead to reset the sequence value.
	 * <p>
	 * The three-argument form with {@code is_called=false} is used so that the next {@code nextval()}
	 * returns {@code startWith} itself, matching PostgreSQL's {@code ALTER SEQUENCE ... RESTART WITH}
	 * semantics. The two-argument form defaults to {@code is_called=true}, which makes the next
	 * {@code nextval()} return {@code startWith + increment} (off by one), breaking tests that reset
	 * a sequence and then expect {@code nextval} to yield the restart value (e.g. truncate/resync).
	 */
	@Override
	public String getRestartSequenceString(String sequenceName, long startWith) {
		return "select setval('" + sequenceName + "', " + startWith + ", false)";
	}


}
