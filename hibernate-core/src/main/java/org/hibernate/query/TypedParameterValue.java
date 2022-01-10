/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;


import org.hibernate.type.BasicTypeReference;

/**
 * Can be used to bind query parameter values.  Allows providing additional details about the
 * parameter value/binding.
 *
 * @author Steve Ebersole
 */
public final class TypedParameterValue<J> {

	private final BindableType<J> type;
	private final J value;

	public TypedParameterValue(BindableType<J> type, J value) {
		this.type = type;
		this.value = value;
	}

	public TypedParameterValue(BasicTypeReference<J> type, J value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * The value to bind
	 *
	 * @return The value to be bound
	 */
	public J getValue() {
		return value;
	}

	/**
	 * The specific Hibernate type to use to bind the value.
	 *
	 * @return The Hibernate type to use.
	 */
	public BindableType<J> getType() {
		return type;
	}

	/**
	 * The specific Hibernate type reference to use to bind the value.
	 *
	 * @return The Hibernate type reference to use.
	 */
	public BasicTypeReference<J> getTypeReference() {
		return type instanceof BasicTypeReference ? (BasicTypeReference<J>) type : null;
	}
}
