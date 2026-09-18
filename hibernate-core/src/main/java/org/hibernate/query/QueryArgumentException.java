/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nonnull
	private final Class<?> parameterType;
	@Nullable
	private final Class<?> argumentType;
	@Nullable
	private final Object argument;

	public QueryArgumentException(@Nonnull String message, @Nonnull Class<?> parameterType, @Nullable Object argument) {
		super( message + " (argument [" + argument + "] is not assignable to " + parameterType.getName() + ")" );
		this.parameterType = parameterType;
		this.argumentType = argument == null ? null : argument.getClass();
		this.argument = argument;
	}

	public QueryArgumentException(@Nonnull String message, @Nonnull Class<?> parameterType, @Nonnull Class<?> argumentType, @Nullable Object argument) {
		super( message + " (" + argumentType.getName() + " is not assignable to " + parameterType.getName() + ")" );
		this.parameterType = parameterType;
		this.argumentType = argumentType;
		this.argument = argument;
	}

	@Nonnull
	public Class<?> getParameterType() {
		return parameterType;
	}

	@Nullable
	public Class<?> getArgumentType() {
		return argumentType;
	}

	@Nullable
	public Object getArgument() {
		return argument;
	}
}
