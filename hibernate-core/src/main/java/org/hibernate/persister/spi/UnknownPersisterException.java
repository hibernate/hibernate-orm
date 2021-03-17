/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.spi;

import org.hibernate.HibernateException;

/**
 * Indicates that the persister to use is not known and could not be determined.
 *
 * @author Steve Ebersole
 */
public class UnknownPersisterException extends HibernateException {
	public UnknownPersisterException(String s) {
		super( s );
	}

	public UnknownPersisterException(String string, Throwable root) {
		super( string, root );
	}
}
