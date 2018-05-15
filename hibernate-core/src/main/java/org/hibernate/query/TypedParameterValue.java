/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;

/**
 * Can be used to bind query parameter values.  Allows to provide additional details about the
 * parameter value/binding.
 *
 * @author Steve Ebersole
 */
public class TypedParameterValue {
	private final AllowableParameterType type;
	private final Object value;

	public TypedParameterValue(AllowableParameterType type, Object value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * The value to bind
	 *
	 * @return The value to be bound
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * The specific Hibernate type to use to bind the value.
	 *
	 * @return The Hibernate type to use.
	 */
	public AllowableParameterType getType() {
		return type;
	}
}
