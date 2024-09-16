/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

/**
 * Paging limits
 *
 * @author Steve Ebersole
 */
public class Limit {
	/**
	 * Singleton access for "no limit"
	 */
	public static final Limit NONE = new Limit();

	private Integer firstRow;
	private Integer maxRows;

	public Limit() {
	}

	public Limit(Integer firstRow, Integer maxRows) {
		this.firstRow = firstRow;
		this.maxRows = maxRows;
	}

	public boolean isEmpty() {
		return firstRow == null && maxRows == null;
	}

	public Limit makeCopy() {
		return new Limit( firstRow, maxRows );
	}

	public Integer getFirstRow() {
		return firstRow;
	}

	public int getFirstRowJpa() {
		// JPA defines this return as a primitive with magic values...
		//  	- specifically the "magic number" 0 (ZERO) as defined by the spec.
		return firstRow == null ? 0 : firstRow;
	}

	public void setFirstRow(Integer firstRow) {
		this.firstRow = firstRow;
	}

	public Integer getMaxRows() {
		return maxRows;
	}

	public int getMaxRowsJpa() {
		// JPA defines this return as a primitive with magic values...
		//  	- specifically the "magic number" Integer.MAX_VALUE as defined by the spec.
		return maxRows == null ? Integer.MAX_VALUE : maxRows;
	}

	public void setMaxRows(int maxRows) {
		if ( maxRows < 0 ) {
			// treat negatives specially as meaning no limit...
			this.maxRows = null;
		}
		else {
			this.maxRows = maxRows;
		}
	}

	public void setMaxRows(Integer maxRows) {
		if ( maxRows != null && maxRows < 0 ) {
			// treat negatives specially as meaning no limit...
			this.maxRows = null;
		}
		else {
			this.maxRows = maxRows;
		}
	}

	public boolean isCompatible(Limit limit) {
		if ( limit == null ) {
			return isEmpty();
		}
		else if ( this == limit ) {
			return true;
		}

		if ( firstRow != null ? !firstRow.equals( limit.firstRow ) : limit.firstRow != null ) {
			return false;
		}
		return maxRows != null ? maxRows.equals( limit.maxRows ) : limit.maxRows == null;
	}

}
