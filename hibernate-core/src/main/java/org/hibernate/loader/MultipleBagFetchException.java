/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader;
import java.util.List;

import org.hibernate.HibernateException;

/**
 * Exception used to indicate that a query is attempting to simultaneously fetch multiple
 * {@linkplain org.hibernate.type.BagType bags}
*
* @author Steve Ebersole
*/
public class MultipleBagFetchException extends HibernateException {
	private final List bagRoles;

	public MultipleBagFetchException(List bagRoles) {
		super( "cannot simultaneously fetch multiple bags: " + bagRoles );
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
