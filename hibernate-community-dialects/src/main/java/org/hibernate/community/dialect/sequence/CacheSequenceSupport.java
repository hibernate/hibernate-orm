/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.CacheDialect}.
 *
 * Use of sequences on Cache is not recommended.
 *
 * @author Gavin King
 */
public final class CacheSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new CacheSequenceSupport();

	public String getSequenceNextValString(String sequenceName) {
		return "select InterSystems.Sequences_GetNext('" + sequenceName + "')" + getFromDual( sequenceName );
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		//TODO: is this really correct? Why can't we just call InterSystems.Sequences_GetNext() without the select?
		return "(select InterSystems.Sequences_GetNext('" + sequenceName + "')" + getFromDual( sequenceName ) + ")";
	}

	private String getFromDual(String sequenceName) {
		return " from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "insert into InterSystems.Sequences(Name) values (ucase('" + sequenceName + "'))";
	}

	public String getDropSequenceString(String sequenceName) {
		return "delete from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}
}
