/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class NoMoreReturnsException extends HibernateException {
	public NoMoreReturnsException(String message) {
		super( message );
	}

	public NoMoreReturnsException() {
		super( "Results have been exhausted" );
	}
}
