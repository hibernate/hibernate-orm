/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
