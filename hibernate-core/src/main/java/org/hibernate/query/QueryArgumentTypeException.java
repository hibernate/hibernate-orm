/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * Thrown when an argument bound to a query parameter is of an incompatible type.
 *
 * @since 7.2
 *
 * @author Gavin King
 */
public class QueryArgumentTypeException extends IllegalArgumentException {
	private final Class<?> expectedType;
	private final Class<?> actualType;

	public QueryArgumentTypeException(String message, Class<?> expectedType, Class<?> actualType) {
		super( message + " (" + actualType.getName() + " is not assignable to " + expectedType.getName() + ")" );
		this.expectedType = expectedType;
		this.actualType = actualType;
	}

	public Class<?> getExpectedType() {
		return expectedType;
	}

	public Class<?> getActualType() {
		return actualType;
	}
}
