/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;


/**
 * Much like {@link TooManyRowsAffectedException}, indicates that more
 * rows than what we were expecting were affected.  Additionally, this form
 * occurs from a batch and carries along the batch position that failed.
 *
 * @author Steve Ebersole
 */
public class BatchedTooManyRowsAffectedException extends TooManyRowsAffectedException {
	private final int batchPosition;

	public BatchedTooManyRowsAffectedException(String message, int expectedRowCount, int actualRowCount, int batchPosition) {
		super( message, expectedRowCount, actualRowCount );
		this.batchPosition = batchPosition;
	}

	public int getBatchPosition() {
		return batchPosition;
	}
}
