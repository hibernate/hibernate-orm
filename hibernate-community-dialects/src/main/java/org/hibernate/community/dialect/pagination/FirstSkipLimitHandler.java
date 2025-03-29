/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;

/**
 * A {@link LimitHandler} for Firebird 2.5 and older which supports the syntax
 * {@code FIRST n SKIP m}.
 */
public class FirstSkipLimitHandler extends AbstractLimitHandler {

	public static final FirstSkipLimitHandler INSTANCE = new FirstSkipLimitHandler();

	@Override
	public String processSql(String sql, Limit limit) {
		boolean hasFirstRow = hasFirstRow( limit );
		boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( hasMaxRows ) {
			skipFirst.append( " first ?" );
		}
		if ( hasFirstRow ) {
			skipFirst.append( " skip ?" );
		}

		return insertAfterSelect( skipFirst.toString(), sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

}
