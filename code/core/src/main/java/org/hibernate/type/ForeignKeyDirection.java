//$Id: ForeignKeyDirection.java 7019 2005-06-05 05:09:58Z oneovthafew $
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