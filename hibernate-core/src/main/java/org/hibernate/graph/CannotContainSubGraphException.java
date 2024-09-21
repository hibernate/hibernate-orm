/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt was made to add a (key)? sub-graph to an
 * attribute type that does not support (key)? sub-graphs.
 *
 * @author Steve Ebersole
 */
public class CannotContainSubGraphException extends HibernateException {
	public CannotContainSubGraphException(String message) {
		super( message );
	}
}
