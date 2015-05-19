/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class UnknownUnwrapTypeException extends HibernateException {
	public UnknownUnwrapTypeException(Class unwrapType) {
		super( "Cannot unwrap to requested type [" + unwrapType.getName() + "]" );
	}

	public UnknownUnwrapTypeException(Class unwrapType, Throwable root) {
		this( unwrapType );
		super.initCause( root );
	}
}
