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
	private final Class<?> argumentType;
	private final Object argument;

	public QueryArgumentException(String message, Class<?> parameterType, Object argument) {
		super( message + " (argument [" + argument + "] is not assignable to " + parameterType.getName() + ")" );
		this.parameterType = parameterType;
		this.argumentType = argument == null ? null : argument.getClass();
		this.argument = argument;
	}

	public QueryArgumentException(String message, Class<?> parameterType, Class<?> argumentType, Object argument) {
		super( message + " (" + argumentType.getName() + " is not assignable to " + parameterType.getName() + ")" );
		this.parameterType = parameterType;
		this.argumentType = argumentType;
		this.argument = argument;
	}

	public Class<?> getParameterType() {
		return parameterType;
	}

	public Class<?> getArgumentType() {
		return argumentType;
	}

	public Object getArgument() {
		return argument;
	}
}
