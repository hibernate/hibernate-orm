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
package org.hibernate.loader.plan2.internal;

import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.internal.EntityIdentifierDescriptionImpl;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityReference implements EntityReference, ExpandingFetchSource {
	private final EntityPersister entityPersister;
	private final PropertyPath propertyPath;

	private ExpandingEntityIdentifierDescription identifierDescription;

	private List<Fetch> fetches;

	public AbstractEntityReference(EntityPersister entityPersister, PropertyPath propertyPath) {
		this.entityPersister = entityPersister;
		this.propertyPath = propertyPath;

		this.identifierDescription = new EntityIdentifierDescriptionImpl( this );
	}

	protected abstract EntityQuerySpace getEntityQuerySpace();

	@Override
	public String getQuerySpaceUid() {
		return getEntityQuerySpace().getUid();
	}

	@Override
	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	@Override
	public ExpandingEntityIdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition, LoadPlanBuildingContext loadPlanBuildingContext) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
