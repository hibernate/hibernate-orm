/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * An exception that occurs while reading mapping sources, either
 * XML or annotations, usually as a result of something screwy in
 * the O/R mappings.
 *
 * @author Gavin King
 */
public class MappingException extends HibernateException {
	/**
	 * Constructs a {@code MappingException} using the given information.
	 *
	 * @param message A message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public MappingException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a {@code MappingException} using the given information.
	 *
	 * @param cause The underlying cause
	 */
	public MappingException(Throwable cause) {
		super( cause );
	}

	/**
	 * Constructs a {@code MappingException} using the given information.
	 *
	 * @param message A message explaining the exception condition
	 */
	public MappingException(String message) {
		super( message );
	}

}
