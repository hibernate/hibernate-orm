/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code ROWS n},
 * but at the start of the query instead of at the end.
 */
public class TimesTenLimitHandler extends AbstractSimpleLimitHandler {

	public static final TimesTenLimitHandler INSTANCE = new TimesTenLimitHandler();

	public TimesTenLimitHandler(){
	}

	@Override
	public boolean supportsOffset() {
		return false;
	}
  
	@Override
	public boolean supportsLimitOffset() { 
		return true;
	}

	@Override
	// TimesTen is 1 based
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult + 1;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " rows ? to ?" : " first ?";
	}
}
