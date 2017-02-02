/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
