/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

import org.hibernate.HibernateException;

/**
 * An exception indicating some kind of problem performing bytecode enhancement.
 *
 * @author Steve Ebersole
 */
public class EnhancementException extends HibernateException {

	public EnhancementException(String message) {
		super( message );
	}

	/**
	 * Constructs an EnhancementException
	 *
	 * @param message Message explaining the exception condition
	 * @param cause The underlying cause.
	 */
	public EnhancementException(String message, Throwable cause) {
		super( message, cause );
	}
}
