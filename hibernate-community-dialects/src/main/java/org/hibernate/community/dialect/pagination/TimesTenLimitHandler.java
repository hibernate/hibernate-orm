/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.AbstractLimitHandler;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code ROWS n},
 * but at the start of the query instead of at the end.
 */
public class TimesTenLimitHandler extends AbstractLimitHandler {

	public static final TimesTenLimitHandler INSTANCE = new TimesTenLimitHandler();

	public TimesTenLimitHandler(){
	}

	/*
	 * Copyright (c) 2025, Oracle and/or its affiliates.
	 * Licensed under the Universal Permissive License v 1.0 as shown
	 * at http://oss.oracle.com/licenses/upl
	 *
	 *   - The class now extends 'AbstractLimitHandler'
	 *   - Removed a custom definition for 'insert()'
	 *   - Added a custom definition for 'supportsLimit()'
	 *   - Added a custom definition for 'supportsOffset()'
	 *   - Added a custom definition for 'supportsLimitOffset()'
	 *   - Added a custom definition for 'supportsVariableLimit()'
	 *   - Added a custom definition for 'convertToFirstRowValue(int zeroBasedFirstResult)'
	 *   - Added a custom definition for 'useMaxForLimit()'
	 *   - Added a custom definition for 'limitClause(boolean hasFirstRow)'
	 *
  */
	@Override
	public boolean supportsLimit() {
		return true;
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
	public boolean supportsVariableLimit() {
 		// a limit string using literals instead of parameters is
		// required to translate from Hibernate's 0 based row numbers
		// to TimesTen 1 based row numbers
 		return false;
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

	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " rows ? to ?" : " first ?";
	}
}
