/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

/**
 * Thrown by {@link GraphParser} to indicate textual entity graph representation parsing errors.
 *
 * @author asusnjar
 *
 */
public class InvalidGraphException extends HibernateException {
	private static final long serialVersionUID = 1L;

	public InvalidGraphException(String message) {
		super( message );
	}
}
