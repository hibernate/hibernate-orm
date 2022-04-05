/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.Incubating;

/**
 * A basic plural type. Represents a type, that is mapped to a single column instead of multiple rows.
 * This is used for array or collection types, that are backed by e.g. SQL array or JSON/XML DDL types.
 *
 * @see BasicCollectionType
 * @see BasicArrayType
 */
@Incubating
public interface BasicPluralType<C, E> extends BasicType<C> {
	/**
	 * Get element type
	 */
	BasicType<E> getElementType();

}
