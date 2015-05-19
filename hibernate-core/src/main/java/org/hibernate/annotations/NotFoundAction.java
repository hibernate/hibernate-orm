/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;


/**
 * Possible actions when an associated entity is not found in the database.  Often seen with "legacy" foreign-key
 * schemes which do not use {@code NULL} to indicate a missing reference, instead using a "magic value".
 *
 * @author Emmanuel Bernard
 */
public enum NotFoundAction {
	/**
	 * Raise an exception when an element is not found (default and recommended).
	 */
	EXCEPTION,
	/**
	 * Ignore the element when not found in database.
	 */
	IGNORE
}
