/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

/**
 * A basic container type.
 *
 * The type also has to implement {@link BasicType} but that can't be enforced
 * because the container type can not be parameterized.
 *
 * @author Christian Beikov
 */
public interface BasicContainerType<T> {
	/**
	 * Get element type
	 */
	BasicType<T> getElementType();

}
