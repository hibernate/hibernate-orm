/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;

/**
 * Defines transformation of a raw row in the domain query result row.
 *
 * E.g. a query might select multiple
 *
 * @see org.hibernate.query.TupleTransformer
 *
 * @author Steve Ebersole
 */
@Incubating
public interface RowTransformer<T> {
	/**
	 * Transform the "raw" row values into the ultimate query result (for a row)
	 */
	T transformRow(Object[] row);

	/**
	 * How many result elements will this transformation produce?
	 */
	default int determineNumberOfResultElements(int rawElementCount) {
		return rawElementCount;
	}
}
