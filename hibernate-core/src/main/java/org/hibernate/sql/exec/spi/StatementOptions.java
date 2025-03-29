/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

/**
 * Options for the creation of a JDBC statement
 *
 * @author Steve Ebersole
 */
public class StatementOptions {
	public static final StatementOptions NONE = new StatementOptions( -1, -1, -1, -1 );

	private final Integer firstRow;
	private final Integer maxRows;
	private final Integer timeoutInMilliseconds;
	private final Integer fetchSize;

	public StatementOptions(
			Integer firstRow,
			Integer maxRows,
			Integer timeoutInMilliseconds,
			Integer fetchSize) {
		this.firstRow = firstRow;
		this.maxRows = maxRows;
		this.timeoutInMilliseconds = timeoutInMilliseconds;
		this.fetchSize = fetchSize;
	}

	public boolean hasLimit() {
		return ( firstRow != null && firstRow > 0 )
				|| ( maxRows != null && maxRows > 0 );
	}

	public Integer getFirstRow() {
		return firstRow;
	}

	public Integer getMaxRows() {
		return maxRows;
	}

	public boolean hasTimeout() {
		return timeoutInMilliseconds != null && timeoutInMilliseconds > 0;
	}

	public Integer getTimeoutInMilliseconds() {
		return timeoutInMilliseconds;
	}

	public boolean hasFetchSize() {
		return fetchSize != null && fetchSize > 0;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}
}
