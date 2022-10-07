/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import org.hibernate.HibernateException;

public class BagFetchException extends HibernateException {
	public BagFetchException(String bagName) {
		super( "cannot simultaneously fetch a bag " + bagName + " and fetch another collection or join one of its collection " );
	}
}
