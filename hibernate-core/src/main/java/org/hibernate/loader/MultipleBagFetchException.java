/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;
import java.util.List;

import org.hibernate.HibernateException;

/**
 * Exception used to indicate that a query is attempting to simultaneously fetch multiple
 * {@link org.hibernate.type.BagType bags}
*
* @author Steve Ebersole
*/
public class MultipleBagFetchException extends HibernateException {
	private final List bagRoles;

	public MultipleBagFetchException(List bagRoles) {
		super( "cannot simultaneously fetch multiple bags" );
		this.bagRoles = bagRoles;
	}

	/**
	 * Retrieves the collection-roles for the bags encountered.
	 *
	 * @return The bag collection roles.
	 */
	public List getBagRoles() {
		return bagRoles;
	}
}
