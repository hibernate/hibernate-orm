/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jndi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem with a given JNDI name being deemed as not valid.
 *
 * @author Steve Ebersole
 */
public class JndiNameException extends HibernateException {
	/**
	 * Constructs a JndiNameException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause.
	 */
	public JndiNameException(String message, Throwable cause) {
		super( message, cause );
	}
}
