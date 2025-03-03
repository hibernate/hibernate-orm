/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Indicates that an element of a path did not resolve to
 * a mapped program element.
 *
 * @apiNote extends {@link IllegalArgumentException} to
 *          satisfy a questionable requirement of the JPA
 *          criteria query API
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class PathElementException extends IllegalArgumentException {
	public PathElementException(String message) {
		super(message);
	}
}
