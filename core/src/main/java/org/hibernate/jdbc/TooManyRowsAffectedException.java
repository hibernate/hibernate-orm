package org.hibernate.jdbc;

import org.hibernate.HibernateException;

/**
 * Indicates that more rows were affected then we were expecting to be.
 * Typically indicates presence of duplicate "PK" values in the
 * given table.
 *
 * @author Steve Ebersole
 */
public class TooManyRowsAffectedException extends HibernateException {
	private final int expectedRowCount;
	private final int actualRowCount;

	public TooManyRowsAffectedException(String message, int expectedRowCount, int actualRowCount) {
		super( message );
		this.expectedRowCount = expectedRowCount;
		this.actualRowCount = actualRowCount;
	}

	public int getExpectedRowCount() {
		return expectedRowCount;
	}

	public int getActualRowCount() {
		return actualRowCount;
	}
}
