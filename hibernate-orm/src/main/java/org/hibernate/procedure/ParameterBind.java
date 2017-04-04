/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import javax.persistence.TemporalType;

/**
 * Describes an input value binding for any IN/INOUT parameters.
 */
public interface ParameterBind<T> {
	/**
	 * Retrieves the bound value.
	 *
	 * @return The bound value.
	 */
	T getValue();

	/**
	 * If {@code <T>} represents a DATE/TIME type value, JPA usually allows specifying the particular parts of
	 * the DATE/TIME value to be bound.  This value represents the particular part the user requested to be bound.
	 *
	 * @return The explicitly supplied TemporalType.
	 */
	TemporalType getExplicitTemporalType();
}
