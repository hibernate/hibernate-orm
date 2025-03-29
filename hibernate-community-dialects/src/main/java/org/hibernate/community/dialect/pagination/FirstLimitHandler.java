/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractNoOffsetLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * A {@link LimitHandler} for older versions of Informix, Ingres,
 * and TimesTen, which supported the syntax {@code SELECT FIRST n}.
 * Note that this syntax does not allow specification of an offset.
 *
 * @author Chris Cranford
 */
public class FirstLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FirstLimitHandler INSTANCE = new FirstLimitHandler(false);

	public FirstLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " first ?";
	}

	@Override
	protected String insert(String first, String sql) {
		return insertAfterSelect( first, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}
}
