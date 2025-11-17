/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.NonTransientException;

/**
 * Indicated a problem with a mapping.  Usually this is a problem with a combination
 * of mapping constructs.
 */
public class UnsupportedMappingException extends HibernateException implements NonTransientException {
	public UnsupportedMappingException(String message) {
		super( message );
	}
}
