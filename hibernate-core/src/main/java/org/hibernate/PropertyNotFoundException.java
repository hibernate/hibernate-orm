/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates that an expected getter or setter method could not be
 * found on a class.
 *
 * @author Gavin King
 */
public class PropertyNotFoundException extends MappingException {
	/**
	 * Constructs a {@code PropertyNotFoundException} given the specified message.
	 *
	 * @param message A message explaining the exception condition
	 */
	public PropertyNotFoundException(String message) {
		super( message );
	}
}
