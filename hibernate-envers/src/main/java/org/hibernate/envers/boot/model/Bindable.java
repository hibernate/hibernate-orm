/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

/**
 * Defines a contract for objects that are bindable.
 *
 * @author Chris Cranford
 */
public interface Bindable<T> {
	/**
	 * Builds the specified binded class type.
	 *
	 * @return instance of the bindable class type, never {@code null}
	 */
	T build();
}
