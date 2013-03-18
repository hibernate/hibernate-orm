/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionReference extends AbstractPlanNode implements CollectionReference {
	private final String alias;
	private final LockMode lockMode;
	private final CollectionPersister collectionPersister;
	private final PropertyPath propertyPath;

	private final CollectionAliases collectionAliases;
	private final EntityAliases elementEntityAliases;

	private final FetchOwner indexGraph;
	private final FetchOwner elementGraph;

	protected AbstractCollectionReference(
			SessionFactoryImplementor sessionFactory,
			String alias,
			LockMode lockMode,
			CollectionPersister collectionPersister,
			PropertyPath propertyPath,
			CollectionAliases collectionAliases,
			EntityAliases elementEntityAliases) {
		super( sessionFactory );
		this.alias = alias;
		this.lockMode = lockMode;
		this.collectionPersister = collectionPersister;
		this.propertyPath = propertyPath;

		this.collectionAliases = collectionAliases;
		this.elementEntityAliases = elementEntityAliases;

		this.indexGraph = buildIndexGraph( getCollectionPersister() );
		this.elementGraph = buildElementGraph( getCollectionPersister() );
	}

	private FetchOwner buildIndexGraph(CollectionPersister persister) {
		if ( persister.hasIndex() ) {
			final Type type = persister.getIndexType();
			if ( type.isAssociationType() ) {
				if ( type.isEntityType() ) {
					return new EntityIndexGraph( sessionFactory(), this, getPropertyPath() );
				}
			}
			else if ( type.isComponentType() ) {
				return new CompositeIndexGraph( sessionFactory(), this, getPropertyPath() );
			}
		}

		return null;
	}

	private FetchOwner buildElementGraph(CollectionPersister persister) {
		final Type type = persister.getElementType();
		if ( type.isAssociationType() ) {
			if ( type.isEntityType() ) {
				return new EntityElementGraph( sessionFactory(), this, getPropertyPath() );
			}
		}
		else if ( type.isComponentType() ) {
			return new CompositeElementGraph( sessionFactory(), this, getPropertyPath() );
		}

		return null;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
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
		return collectionPersister;
	}

	@Override
	public FetchOwner getIndexGraph() {
		return indexGraph;
	}

	@Override
	public FetchOwner getElementGraph() {
		return elementGraph;
	}

	@Override
	public boolean hasEntityElements() {
		return getCollectionPersister().isOneToMany() || getCollectionPersister().isManyToMany();
	}
}
