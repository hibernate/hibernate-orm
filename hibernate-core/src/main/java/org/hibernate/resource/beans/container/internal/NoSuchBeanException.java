/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
