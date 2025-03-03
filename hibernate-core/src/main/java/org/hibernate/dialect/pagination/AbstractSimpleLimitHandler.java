/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.query.spi.Limit;

/**
 * Superclass for simple {@link LimitHandler}s that don't
 * support specifying an offset without a limit.
 *
 * @author Gavin King
 */
public abstract class AbstractSimpleLimitHandler extends AbstractLimitHandler {

	protected abstract String limitClause(boolean hasFirstRow);

	protected String offsetOnlyClause() {
		return null;
	}

	@Override
	public String processSql(String sql, Limit limit) {
		if ( !hasMaxRows( limit ) ) {
			final String offsetOnlyClause = offsetOnlyClause();
			if ( offsetOnlyClause != null && hasFirstRow( limit ) ) {
				return insert( offsetOnlyClause, sql );
			}
			return sql;
		}
		return insert( limitClause( hasFirstRow( limit ) ), sql );
	}

	protected String insert(String limitClause, String sql) {
		return insertBeforeForUpdate( limitClause, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return super.supportsOffset();
	}
}
