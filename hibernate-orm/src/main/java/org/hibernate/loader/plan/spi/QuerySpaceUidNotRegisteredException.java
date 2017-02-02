/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to lookup a QuerySpace by its uid, when no registration has been made under that uid.
 *
 * @author Steve Ebersole
 */
public class QuerySpaceUidNotRegisteredException extends HibernateException {
	public QuerySpaceUidNotRegisteredException(String uid) {
		super( generateMessage( uid ) );
	}

	private static String generateMessage(String uid) {
		return "Given uid [" + uid + "] could not be resolved to a QuerySpace";
	}

	public QuerySpaceUidNotRegisteredException(String uid, Throwable cause) {
		super( generateMessage( uid ), cause );
	}
}
