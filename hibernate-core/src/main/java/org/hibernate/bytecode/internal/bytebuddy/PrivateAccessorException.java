/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

public class PrivateAccessorException extends InvalidPropertyAccessorException {

	public PrivateAccessorException(String message) {
		super( message );
	}
}
