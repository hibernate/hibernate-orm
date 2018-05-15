/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.HibernateException;

/**
 * Indicates access to something that is not yet resolved/ready-to-use.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class NotYetResolvedException extends HibernateException {
	public NotYetResolvedException(String message) {
		super( message );
	}

	public NotYetResolvedException(String message, Throwable cause) {
		super( message, cause );
	}
}
