/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc.leak;

/**
 * @author Vlad Mihalcea
 */
public class ConnectionLeakException extends RuntimeException {

	public ConnectionLeakException() {
	}

	public ConnectionLeakException(String message) {
		super( message );
	}

	public ConnectionLeakException(String message, Throwable cause) {
		super( message, cause );
	}

	public ConnectionLeakException(Throwable cause) {
		super( cause );
	}

	public ConnectionLeakException(
			String message,
			Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super( message, cause, enableSuppression, writableStackTrace );
	}
}
