/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
