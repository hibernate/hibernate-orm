/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.container.internal;

import org.hibernate.HibernateException;

/**
 * Exception indicating that the given class is not known as a CDI bean - triggers
 * fallback handling
 *
 * @author Steve Ebersole
 */
public class NoSuchBeanException extends HibernateException {
	public NoSuchBeanException(Throwable cause) {
		super( cause );
	}

	public NoSuchBeanException(String message, Throwable cause) {
		super( message, cause );
	}
}
