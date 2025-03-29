/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;

/**
 * A {@link LimitHandler} for Informix which supports the syntax
 * {@code SKIP m FIRST n}.
 */
public class SkipFirstLimitHandler extends AbstractLimitHandler {

	public static final SkipFirstLimitHandler INSTANCE = new SkipFirstLimitHandler(true);

	private final boolean variableLimit;

	public SkipFirstLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public String processSql(String sql, Limit limit) {

		boolean hasFirstRow = hasFirstRow( limit );
		boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( supportsVariableLimit() ) {
			if ( hasFirstRow ) {
				skipFirst.append( " skip ?" );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first ?" );
			}
		}
		else {
			if ( hasFirstRow ) {
				skipFirst.append( " skip " )
						.append( limit.getFirstRow() );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first " )
						.append( getMaxOrLimit( limit ) );
			}
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
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}
}
