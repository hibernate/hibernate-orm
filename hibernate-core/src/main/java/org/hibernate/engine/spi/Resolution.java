/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * Used to put natural id values into collections.  Useful mainly to
 * apply equals/hashCode implementations.
 */
public interface Resolution {
	Object getNaturalIdValue();
	boolean isSame(Object otherValue);
}
