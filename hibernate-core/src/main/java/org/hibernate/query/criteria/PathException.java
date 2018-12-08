/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to use a path in an unsupported way
 *
 * @author Steve Ebersole
 */
public class PathException extends HibernateException {
	public PathException(String message) {
		super( message );
	}

	public PathException(String message, Throwable cause) {
		super( message, cause );
	}
}
