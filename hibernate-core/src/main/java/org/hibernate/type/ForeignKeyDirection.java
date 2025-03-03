/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.engine.internal.CascadePoint;

/**
 * Represents directionality of the foreign key constraint
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public enum ForeignKeyDirection {
	/**
	 * A foreign key from child to parent
	 */
	TO_PARENT {
		@Override
		public boolean cascadeNow(CascadePoint cascadePoint) {
			return cascadePoint != CascadePoint.BEFORE_INSERT_AFTER_DELETE;
		}

	},
	/**
	 * A foreign key from parent to child
	 */
	FROM_PARENT {
		@Override
		public boolean cascadeNow(CascadePoint cascadePoint) {
			return cascadePoint != CascadePoint.AFTER_INSERT_BEFORE_DELETE;
		}
	};

	/**
	 * Should we cascade at this cascade point?
	 *
	 * @param cascadePoint The point at which the cascade is being initiated.
	 *
	 * @return {@code true} if cascading should be performed now.
	 *
	 * @see org.hibernate.engine.internal.Cascade
	 */
	public abstract boolean cascadeNow(CascadePoint cascadePoint);
}
