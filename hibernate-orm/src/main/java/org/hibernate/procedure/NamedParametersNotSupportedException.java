/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Thrown to indicate that an attempt was made to register a stored procedure named parameter, but the underlying
 * database reports to not support named parameters.
 *
 * @author Steve Ebersole
 */
public class NamedParametersNotSupportedException extends HibernateException {
	public NamedParametersNotSupportedException(String message) {
		super( message );
	}
}
