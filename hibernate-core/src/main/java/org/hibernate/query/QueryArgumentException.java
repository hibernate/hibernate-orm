/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * An error that occurs binding an argument to a query parameter.
 * Usually indicates that the argument is of a type not assignable
 * to the type of the parameter.
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class QueryArgumentException extends IllegalArgumentException {
	private final Class<?> parameterType;
	private final Object argument;

	public QueryArgumentException(String message, Class<?> parameterType, Object argument) {
		super(message);
		this.parameterType = parameterType;
		this.argument = argument;
	}

	public Class<?> getParameterType() {
		return parameterType;
	}

	public Object getArgument() {
		return argument;
	}
}
