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
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Gail Badner
 */
public abstract class AbstractExpandingFetchSource implements ExpandingFetchSource {
	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final Fetch[] NO_FETCHES = new Fetch[0];

	/**
	 * Convenient constant for returning no fetches from {@link #getFetches()}
	 */
	private static final BidirectionalEntityReference[] NO_BIDIRECTIONAL_ENTITY_REFERENCES =
			new BidirectionalEntityReference[0];

	private final ExpandingQuerySpace querySpace;
	private final PropertyPath propertyPath;
	private List<Fetch> fetches;
	private List<BidirectionalEntityReference> bidirectionalEntityReferences;

	public AbstractExpandingFetchSource(ExpandingQuerySpace querySpace, PropertyPath propertyPath) {
		this.querySpace = querySpace;
		this.propertyPath = propertyPath;
	}

	@Override
	public final String getQuerySpaceUid() {
		return querySpace.getUid();
	}

	protected final ExpandingQuerySpace expandingQuerySpace() {
		return querySpace;
	}

	@Override
	public final PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}

	private void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}
		fetches.add( fetch );
	}

	@Override
	public BidirectionalEntityReference[] getBidirectionalEntityReferences() {
		return bidirectionalEntityReferences == null ?
				NO_BIDIRECTIONAL_ENTITY_REFERENCES :
				bidirectionalEntityReferences.toArray(
						new BidirectionalEntityReference[ bidirectionalEntityReferences.size() ]
				);
	}

	private void addBidirectionalEntityReference(BidirectionalEntityReference bidirectionalEntityReference) {
		if ( bidirectionalEntityReferences == null ) {
			bidirectionalEntityReferences = new ArrayList<BidirectionalEntityReference>();
		}
		bidirectionalEntityReferences.add( bidirectionalEntityReference );
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

		final Join join = querySpace.addEntityJoin(
				attributeDefinition,
				fetchedPersister,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid(),
				attributeDefinition.isNullable()
		);
		final EntityFetch fetch = new EntityFetchImpl( this, attributeDefinition, fetchStrategy, join );
		addFetch( fetch );
		return fetch;
	}

	@Override
	public BidirectionalEntityReference buildBidirectionalEntityReference(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			EntityReference targetEntityReference,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		final EntityType fetchedType = (EntityType) attributeDefinition.getType();
		final EntityPersister fetchedPersister = loadPlanBuildingContext.getSessionFactory().getEntityPersister(
				fetchedType.getAssociatedEntityName()
		);

		if ( fetchedPersister == null ) {
			throw new WalkingException(
					String.format(
							"Unable to locate EntityPersister [%s] for bidirectional entity reference [%s]",
							fetchedType.getAssociatedEntityName(),
							attributeDefinition.getName()
					)
			);
		}

		final BidirectionalEntityReference bidirectionalEntityReference =
				new BidirectionalEntityReferenceImpl( this, attributeDefinition, targetEntityReference );
		addBidirectionalEntityReference( bidirectionalEntityReference );
		return bidirectionalEntityReference;
	}

	protected abstract CompositeFetch createCompositeFetch(
			CompositeType compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace);

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		final ExpandingQuerySpace leftHandSide = expandingQuerySpace();
		final Join join = leftHandSide.addCompositeJoin(
				attributeDefinition,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid()
		);
		final CompositeFetch fetch = createCompositeFetch(
				attributeDefinition.getType(),
				(ExpandingCompositeQuerySpace) join.getRightHandSide()
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
		final Join join = querySpace.addCollectionJoin(
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
