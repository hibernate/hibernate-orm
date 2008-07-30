/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
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
