/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

public class PrivateAccessorException extends InvalidPropertyAccessorException {

	public PrivateAccessorException(String message) {
		super( message );
	}
}
