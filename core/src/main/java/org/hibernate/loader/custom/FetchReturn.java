/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.loader.custom;

import org.hibernate.LockMode;

/**
 * Represents a return which names a fetched association.
 *
 * @author Steve Ebersole
 */
public abstract class FetchReturn extends NonScalarReturn {
	private final NonScalarReturn owner;
	private final String ownerProperty;

	/**
	 * Creates a return descriptor for an association fetch.
	 *
	 * @param owner The return descriptor for the owner of the fetch
	 * @param ownerProperty The name of the property represernting the association being fetched
	 * @param alias The alias for the fetch
	 * @param lockMode The lock mode to apply to the fetched association.
	 */
	public FetchReturn(
			NonScalarReturn owner,
			String ownerProperty,
			String alias,
			LockMode lockMode) {
		super( alias, lockMode );
		this.owner = owner;
		this.ownerProperty = ownerProperty;
	}

	/**
	 * Retrieves the return descriptor for the owner of this fetch.
	 *
	 * @return The owner
	 */
	public NonScalarReturn getOwner() {
		return owner;
	}

	/**
	 * The name of the property on the owner which represents this association.
	 *
	 * @return The property name.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}
}
