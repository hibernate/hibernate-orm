/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

public class InvalidNamedEntityGraphParameterException extends HibernateException {
	private static final long serialVersionUID = 1L;

	public InvalidNamedEntityGraphParameterException(String message) {
		super( message );
	}
}
