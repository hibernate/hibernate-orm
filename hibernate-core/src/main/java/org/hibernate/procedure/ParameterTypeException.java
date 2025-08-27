/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Indicates Hibernate is unable to determine the type details for a parameter.
 *
 * @author Steve Ebersole
 */
public class ParameterTypeException extends HibernateException {
	public ParameterTypeException(String message) {
		super( message );
	}
}
