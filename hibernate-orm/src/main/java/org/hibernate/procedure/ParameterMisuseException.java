/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

import org.hibernate.HibernateException;

/**
 * Thrown to indicate a misuse of a {@link ParameterRegistration}
 *
 * @author Steve Ebersole
 */
public class ParameterMisuseException extends HibernateException {
	public ParameterMisuseException(String message) {
		super( message );
	}
}
