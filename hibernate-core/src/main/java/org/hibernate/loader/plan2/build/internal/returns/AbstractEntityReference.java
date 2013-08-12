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
package org.hibernate.loader.plan2.build.internal.returns;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityIdentifierDescription;
import org.hibernate.loader.plan2.spi.EntityQuerySpace;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityReference implements EntityReference, ExpandingFetchSource {
	private final EntityQuerySpace entityQuerySpace;
	private final PropertyPath propertyPath;

	private final EntityIdentifierDescription identifierDescription;

	private List<Fetch> fetches;

	public AbstractEntityReference(
			EntityQuerySpace entityQuerySpace,
			PropertyPath propertyPath) {
		this.entityQuerySpace = entityQuerySpace;
		this.propertyPath = propertyPath;
		this.identifierDescription = buildIdentifierDescription();
	}


	/**
	 * Builds just the first level of identifier description.  This will be either a simple id descriptor (String,
	 * Long, etc) or some form of composite id (either encapsulated or not).
	 *
	 * @return the descriptor for the identifier
	 */
	private EntityIdentifierDescription buildIdentifierDescription() {
		final EntityPersister persister = entityQuerySpace.getEntityPersister();
		final EntityIdentifierDefinition identifierDefinition = persister.getEntityKeyDefinition();

		if ( identifierDefinition.isEncapsulated() ) {
			final EncapsulatedEntityIdentifierDefinition encapsulatedIdentifierDefinition = (EncapsulatedEntityIdentifierDefinition) identifierDefinition;
			final Type idAttributeType = encapsulatedIdentifierDefinition.getAttributeDefinition().getType();
			if ( ! CompositeType.class.isInstance( idAttributeType ) ) {
				return new SimpleEntityIdentifierDescriptionImpl();
			}
		}

		// if we get here, we know we have a composite identifier...
		final Join join = ( (ExpandingEntityQuerySpace) entityQuerySpace ).makeCompositeIdentifierJoin();
		return identifierDefinition.isEncapsulated()
				? buildEncapsulatedCompositeIdentifierDescription( join )
				: buildNonEncapsulatedCompositeIdentifierDescription( join );
	}

	private NonEncapsulatedEntityIdentifierDescription buildNonEncapsulatedCompositeIdentifierDescription(Join compositeJoin) {
		return new NonEncapsulatedEntityIdentifierDescription(
				this,
				(CompositeQuerySpace) compositeJoin.getRightHandSide(),
				(CompositeType) entityQuerySpace.getEntityPersister().getIdentifierType(),
				propertyPath.append( "id" )
		);
	}

	private EncapsulatedEntityIdentifierDescription buildEncapsulatedCompositeIdentifierDescription(Join compositeJoin) {
		return new EncapsulatedEntityIdentifierDescription(
				this,
				(CompositeQuerySpace) compositeJoin.getRightHandSide(),
				(CompositeType) entityQuerySpace.getEntityPersister().getIdentifierType(),
				propertyPath.append( "id" )
		);
	}

	@Override
	public EntityReference resolveEntityReference() {
		return this;
	}

	protected EntityQuerySpace getEntityQuerySpace() {
		return entityQuerySpace;
	}

	@Override
	public String getQuerySpaceUid() {
		return getEntityQuerySpace().getUid();
	}

	@Override
	public EntityPersister getEntityPersister() {
		return entityQuerySpace.getEntityPersister();
	}

	@Override
	public EntityIdentifierDescription getIdentifierDescription() {
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
		final EntityType fetchedType = (EntityType) attributeDefinition.getType();
		final EntityPersister fetchedPersister = loadPlanBuildingContext.getSessionFactory().getEntityPersister(
				fetchedType.getAssociatedEntityName()
		);

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate EntityPersister [%s] for fetch [%s]",
							fetchedType.getAssociatedEntityName(),
							attributeDefinition.getName()
					)
			);
		}

		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) entityQuerySpace;
		final Join join = leftHandSide.addEntityJoin(
				attributeDefinition,
				fetchedPersister,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid(),
				attributeDefinition.isNullable()
		);
		final EntityFetch fetch = new EntityFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy,
				join
		);
		addFetch( fetch );
		return fetch;
	}

	private void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}
		fetches.add( fetch );
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) entityQuerySpace;
		final Join join = leftHandSide.addCompositeJoin(
				attributeDefinition,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid()
		);

		final CompositeFetchImpl fetch = new CompositeFetchImpl(
				this,
				attributeDefinition.getType(),
				(CompositeQuerySpace) join.getRightHandSide(),
				true,
				getPropertyPath()
		);
		addFetch( fetch );
		return fetch;
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {

		// general question here wrt Joins and collection fetches...  do we create multiple Joins for many-to-many,
		// for example, or do we allow the Collection QuerySpace to handle that?

		final CollectionType fetchedType = (CollectionType) attributeDefinition.getType();
		final CollectionPersister fetchedPersister = loadPlanBuildingContext.getSessionFactory().getCollectionPersister(
				fetchedType.getRole()
		);

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate CollectionPersister [%s] for fetch [%s]",
							fetchedType.getRole(),
							attributeDefinition.getName()
					)
			);
		}
		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) loadPlanBuildingContext.getQuerySpaces().getQuerySpaceByUid(
				getQuerySpaceUid()
		);
		final Join join = leftHandSide.addCollectionJoin(
				attributeDefinition,
				fetchedPersister,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid()
		);
		final CollectionFetch fetch = new CollectionFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy,
				join,
				loadPlanBuildingContext
		);
		addFetch( fetch );
		return fetch;
	}
}
