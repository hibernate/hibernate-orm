/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;


/**
 * Specifies how Hibernate should handle the case of an orphaned foreign
 * key with no associated row in the referenced table. This is a situation
 * that can only occur in a database with missing foreign key constraints.
 * <p>
 * When a database lacks foreign key constraints, it's normal for it to
 * accumulate data with referential integrity violations over time.
 * Alternatively, this situation is also sometimes encountered in legacy
 * foreign key schemes where some "magic value", instead of {@code NULL},
 * indicates a missing reference.
 *
 * @see NotFound
 *
 * @author Emmanuel Bernard
 */
public enum NotFoundAction {
	/**
	 * Raise an exception when a foreign key value has no corresponding
	 * primary key value in the referenced table.
	 */
	EXCEPTION,
	/**
	 * Treat a foreign key value with no corresponding primary key value
	 * in the referenced table as a null reference.
	 */
	IGNORE
}
