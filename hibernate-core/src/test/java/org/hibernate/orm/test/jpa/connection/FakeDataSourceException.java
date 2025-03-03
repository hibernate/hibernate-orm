/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.connection;


/**
 * @author Emmanuel Bernard
 */
public class FakeDataSourceException extends RuntimeException {
	public FakeDataSourceException(String message) {
		super( message );
	}
}
