/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a condition where an instrumented/enhanced class was expected, but the class was not
 * instrumented/enhanced.
 *
 * @author Steve Ebersole
 */
public class NotInstrumentedException extends HibernateException {
	/**
	 * Constructs a NotInstrumentedException
	 *
	 * @param message Message explaining the exception condition
	 */
	public NotInstrumentedException(String message) {
		super( message );
	}
}
