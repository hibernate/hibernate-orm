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
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.engine.Cascade;

/**
 * Represents directionality of the foreign key constraint
 * @author Gavin King
 */
public abstract class ForeignKeyDirection implements Serializable {
	protected ForeignKeyDirection() {}
	/**
	 * Should we cascade at this cascade point?
	 * @see org.hibernate.engine.Cascade
	 */
	public abstract boolean cascadeNow(int cascadePoint);

	/**
	 * A foreign key from child to parent
	 */
	public static final ForeignKeyDirection FOREIGN_KEY_TO_PARENT = new ForeignKeyDirection() {
		public boolean cascadeNow(int cascadePoint) {
			return cascadePoint!=Cascade.BEFORE_INSERT_AFTER_DELETE;
		}

		public String toString() {
			return "toParent";
		}
		
		Object readResolve() {
			return FOREIGN_KEY_TO_PARENT;
		}
	};
	/**
	 * A foreign key from parent to child
	 */
	public static final ForeignKeyDirection FOREIGN_KEY_FROM_PARENT = new ForeignKeyDirection() {
		public boolean cascadeNow(int cascadePoint) {
			return cascadePoint!=Cascade.AFTER_INSERT_BEFORE_DELETE;
		}

		public String toString() {
			return "fromParent";
		}
		
		Object readResolve() {
			return FOREIGN_KEY_FROM_PARENT;
		}
	};
}