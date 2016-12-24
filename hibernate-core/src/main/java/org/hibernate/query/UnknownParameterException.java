/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Generally indicates an attempt to bind a parameter value for an unknown parameter.
 *
 * @author Steve Ebersole
 */
public class UnknownParameterException extends HibernateException {
	public UnknownParameterException(String message) {
		super( message );
	}
}
