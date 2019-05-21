/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

/**
 * @author Steve Ebersole
 */
public class Limit {
	/**
	 * Singleton access for "no limit"
	 */
	public static final Limit NONE = new Limit();

	private Integer firstRow;
	private Integer maxRows;

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
		if ( maxRows <= 0 ) {
			// treat zero and negatives specially as meaning no limit...
			this.maxRows = null;
		}
		else {
			this.maxRows = maxRows;
		}
	}

	public void setMaxRows(Integer maxRows) {
		if ( maxRows != null && maxRows <= 0 ) {
			// treat zero and negatives specially as meaning no limit...
			this.maxRows = null;
		}
		else {
			this.maxRows = maxRows;
		}
	}
}
