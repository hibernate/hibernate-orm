/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class CollectionReturn extends AbstractFetchOwner implements Return, FetchOwner, CollectionReference {
	private final String ownerEntityName;
	private final String ownerProperty;
	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	private final CollectionPersister persister;

	private final PropertyPath propertyPath = new PropertyPath(); // its a root

	public CollectionReturn(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			String ownerEntityName,
			String ownerProperty,
			CollectionAliases collectionAliases,
			EntityAliases elementEntityAliases) {
		super( sessionFactory, alias, lockMode );
		this.ownerEntityName = ownerEntityName;
		this.ownerProperty = ownerProperty;
		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;

		final String role = ownerEntityName + '.' + ownerProperty;
		this.persister = sessionFactory.getCollectionPersister( role );
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}

	@Override
	public CollectionAliases getCollectionAliases() {
		return collectionAliases;
	}

	@Override
	public EntityAliases getElementEntityAliases() {
		return elementEntityAliases;
	}

	@Override
	public CollectionPersister getCollectionPersister() {
		return persister;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return ( (QueryableCollection) persister ).getElementPersister();
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}
}
