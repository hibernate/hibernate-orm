/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Indicates an attempt to dereference a terminal path
 * (usually a path referring to something of basic type)
 *
 * @apiNote extends {@link IllegalStateException} to
 *          satisfy a questionable requirement of the JPA
 *          criteria query API
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class TerminalPathException extends IllegalStateException {
	public TerminalPathException(String message) {
		super(message);
	}
}
