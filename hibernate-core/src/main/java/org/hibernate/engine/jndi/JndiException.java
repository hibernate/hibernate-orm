/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jndi;
import org.hibernate.HibernateException;

/**
 * An exception indicating trouble accessing JNDI
 *
 * @author Steve Ebersole
 */
public class JndiException extends HibernateException {
	/**
	 * Constructs a JndiException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause
	 */
	public JndiException(String message, Throwable cause) {
		super( message, cause );
	}
}
