/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates a request against an unknown filter name.
 *
 * @author Gavin King
 *
 * @see org.hibernate.annotations.FilterDef
 * @see Session#enableFilter(String)
 */
public class UnknownFilterException extends HibernateException {
	private final String name;

	/**
	 * Constructs an {@code UnknownFilterException} for the given name.
	 *
	 * @param name The filter that was unknown.
	 */
	public UnknownFilterException(String name) {
		super( "No filter named '" + name + "'" );
		this.name = name;
	}

	/**
	 * The unknown filter name.
	 *
	 * @return The unknown filter name.
	 */
	public String getName() {
		return name;
	}
}
