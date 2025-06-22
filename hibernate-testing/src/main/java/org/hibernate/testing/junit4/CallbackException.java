/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.junit4;

import java.lang.reflect.Method;

/**
 * Indicates an exception while performing a callback on the test
 *
 * @author Steve Ebersole
 */
public class CallbackException extends RuntimeException {
	public CallbackException(Method method) {
		this( Helper.extractMethodName( method ) );
	}

	public CallbackException(String message) {
		super( message );
	}

	public CallbackException(Method method, Throwable cause) {
		this( Helper.extractMethodName( method ), cause );
	}

	public CallbackException(String message, Throwable cause) {
		super( message, cause );
	}
}
