/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import org.hibernate.engine.internal.Cascade;

/**
 * Represents directionality of the foreign key constraint
 *
 * @author Gavin King
 */
public enum ForeignKeyDirection {
	/**
	 * A foreign key from child to parent
	 */
	TO_PARENT {
		@Override
		public boolean cascadeNow(int cascadePoint) {
			return cascadePoint != Cascade.BEFORE_INSERT_AFTER_DELETE;
		}

	},
	/**
	 * A foreign key from parent to child
	 */
	FROM_PARENT {
		@Override
		public boolean cascadeNow(int cascadePoint) {
			return cascadePoint != Cascade.AFTER_INSERT_BEFORE_DELETE;
		}
	};

	/**
	 * Should we cascade at this cascade point?
	 *
	 * @see org.hibernate.engine.internal.Cascade
	 */
	public abstract boolean cascadeNow(int cascadePoint);

}