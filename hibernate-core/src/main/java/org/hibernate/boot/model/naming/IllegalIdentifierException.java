/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.HibernateException;

/**
 * Indicates an attempted use of a name that was deemed illegal
 *
 * @author Steve Ebersole
 */
public class IllegalIdentifierException extends HibernateException {
	public IllegalIdentifierException(String s) {
		super( s );
	}
}
