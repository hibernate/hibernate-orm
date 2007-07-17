package org.hibernate.jdbc;

/**
 * Much like {@link TooManyRowsAffectedException}, indicates that more
 * rows than what we were expcecting were affected.  Additionally, this form
 * occurs from a batch and carries along the batch positon that failed.
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
