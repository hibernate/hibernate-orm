/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class NoMoreOutputsException extends HibernateException {
	public NoMoreOutputsException(String message) {
		super( message );
	}

	public NoMoreOutputsException() {
		super( "Outputs have been exhausted" );
	}
}
