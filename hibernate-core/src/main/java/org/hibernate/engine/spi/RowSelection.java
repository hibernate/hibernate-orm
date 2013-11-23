/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.spi;

/**
 * Represents a selection criteria for rows in a JDBC {@link java.sql.ResultSet}
 *
 * @author Gavin King
 */
public final class RowSelection {
	private Integer firstRow;
	private Integer maxRows;
	private Integer timeout;
	private Integer fetchSize;

	public void setFirstRow(Integer firstRow) {
		this.firstRow = firstRow;
	}

	public Integer getFirstRow() {
		return firstRow;
	}

	public void setMaxRows(Integer maxRows) {
		this.maxRows = maxRows;
	}

	public Integer getMaxRows() {
		return maxRows;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public void setFetchSize(Integer fetchSize) {
		this.fetchSize = fetchSize;
	}

	public boolean definesLimits() {
		return maxRows != null || (firstRow != null && firstRow <= 0);
	}
}
