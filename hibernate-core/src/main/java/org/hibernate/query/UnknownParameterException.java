/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to find an unknown query parameter or an attempt to
 * bind a value to an unknown query parameter
 *
 * @see Query#getParameter
 * @see Query#setParameter
 * @see Query#setParameterList
 *
 * @author Steve Ebersole
 */
public class UnknownParameterException extends HibernateException {
	public UnknownParameterException(String message) {
		super( message );
	}
}
