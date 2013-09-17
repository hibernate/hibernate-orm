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
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;

/**
 * Specifically a fetch return that refers to a collection association.
 *
 * @author Steve Ebersole
 */
public class CollectionFetchReturn extends FetchReturn {
	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	public CollectionFetchReturn(
			String alias,
			NonScalarReturn owner,
			String ownerProperty,
			CollectionAliases collectionAliases,
	        EntityAliases elementEntityAliases,
			LockMode lockMode) {
		super( owner, ownerProperty, alias, lockMode );
		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;
	}

	public CollectionAliases getCollectionAliases() {
		return collectionAliases;
	}

	public EntityAliases getElementEntityAliases() {
		return elementEntityAliases;
	}
}
