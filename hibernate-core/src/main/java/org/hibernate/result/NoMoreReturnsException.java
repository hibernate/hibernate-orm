/*
 * SPDX-License-Identifier: Apache-2.0
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
