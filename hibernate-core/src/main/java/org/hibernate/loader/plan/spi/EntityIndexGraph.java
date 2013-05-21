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
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AssociationType;

/**
 *  Represents the {@link FetchOwner} for a collection index that is an entity.
 *
 * @author Steve Ebersole
 */
public class EntityIndexGraph extends AbstractFetchOwner implements FetchableCollectionIndex, EntityReference {
	private final CollectionReference collectionReference;
	private final CollectionPersister collectionPersister;
	private final AssociationType indexType;
	private final EntityPersister indexPersister;
	private final PropertyPath propertyPath;
	private final FetchOwnerDelegate fetchOwnerDelegate;

	private IdentifierDescription identifierDescription;

	/**
	 * Constructs an {@link EntityIndexGraph}.
	 *
	 * @param sessionFactory - the session factory.
	 * @param collectionReference - the collection reference.
	 * @param collectionPath - the {@link PropertyPath} for the collection.
	 */
	public EntityIndexGraph(
			SessionFactoryImplementor sessionFactory,
			CollectionReference collectionReference,
			PropertyPath collectionPath) {
		super( sessionFactory );
		this.collectionReference = collectionReference;
		this.collectionPersister = collectionReference.getCollectionPersister();
		this.indexType = (AssociationType) collectionPersister.getIndexType();
		this.indexPersister = (EntityPersister) this.indexType.getAssociatedJoinable( sessionFactory() );
		this.propertyPath = collectionPath.append( "<index>" ); // todo : do we want the <index> part?
		this.fetchOwnerDelegate = new EntityFetchOwnerDelegate( indexPersister );
	}

	public EntityIndexGraph(EntityIndexGraph original, CopyContext copyContext) {
		super( original, copyContext );
		this.collectionReference = original.collectionReference;
		this.collectionPersister = original.collectionReference.getCollectionPersister();
		this.indexType = original.indexType;
		this.indexPersister = original.indexPersister;
		this.propertyPath = original.propertyPath;
		this.fetchOwnerDelegate = original.fetchOwnerDelegate;
	}

	/**
	 * TODO: Does lock mode apply to a collection index that is an entity?
	 */
	@Override
	public LockMode getLockMode() {
		return null;
	}

	@Override
	public EntityReference getEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return indexPersister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return indexPersister;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public EntityIndexGraph makeCopy(CopyContext copyContext) {
		return new EntityIndexGraph( this, copyContext );
	}

	@Override
	public CollectionReference getCollectionReference() {
		return collectionReference;
	}

	@Override
	protected FetchOwnerDelegate getFetchOwnerDelegate() {
		return fetchOwnerDelegate;
	}
}
