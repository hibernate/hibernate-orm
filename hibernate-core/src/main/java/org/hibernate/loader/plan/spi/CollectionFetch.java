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
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class CollectionFetch extends AbstractFetch implements CollectionReference {
	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	private final CollectionPersister persister;

	public CollectionFetch(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			AbstractFetchOwner owner,
			FetchStrategy fetchStrategy,
			String ownerProperty,
			CollectionAliases collectionAliases,
			EntityAliases elementEntityAliases) {
		super( sessionFactory, alias, lockMode, owner, ownerProperty, fetchStrategy );
		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;

		final String role = owner.retrieveFetchSourcePersister().getEntityName() + '.' + getOwnerPropertyName();
		this.persister = sessionFactory.getCollectionPersister( role );
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
	public EntityPersister retrieveFetchSourcePersister() {
		return ( (QueryableCollection) getCollectionPersister() ).getElementPersister();
	}
}
