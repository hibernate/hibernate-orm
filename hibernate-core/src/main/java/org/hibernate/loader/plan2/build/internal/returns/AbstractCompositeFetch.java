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
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeFetch implements CompositeFetch, ExpandingFetchSource {
	private static final FetchStrategy FETCH_STRATEGY = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	private final CompositeType compositeType;
	private final CompositeQuerySpace compositeQuerySpace;
	private final PropertyPath propertyPath;
	private final boolean allowCollectionFetches;

	private List<Fetch> fetches;

	protected AbstractCompositeFetch(
			CompositeType compositeType,
			CompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches, PropertyPath propertyPath) {
		this.compositeType = compositeType;
		this.compositeQuerySpace = compositeQuerySpace;
		this.allowCollectionFetches = allowCollectionFetches;
		this.propertyPath = propertyPath;
	}

	@SuppressWarnings("UnusedParameters")
	protected CompositeQuerySpace resolveCompositeQuerySpace(LoadPlanBuildingContext loadPlanBuildingContext) {
		return compositeQuerySpace;
	}

	@Override
	public EntityReference resolveEntityReference() {
		return resolveFetchSourceEntityReference( this );
	}

	private static EntityReference resolveFetchSourceEntityReference(CompositeFetch fetch) {
		final FetchSource fetchSource = fetch.getSource();

		if ( EntityReference.class.isInstance( fetchSource ) ) {
			return (EntityReference) fetchSource;
		}
		else if ( CompositeFetch.class.isInstance( fetchSource ) ) {
			return resolveFetchSourceEntityReference( (CompositeFetch) fetchSource );
		}
		throw new IllegalStateException(
				String.format(
						"Cannot resolve FetchOwner [%s] of Fetch [%s (%s)] to an EntityReference",
						fetchSource,
						fetch,
						fetch.getPropertyPath()
				)
		);
	}

	@Override
	public String getQuerySpaceUid() {
		return compositeQuerySpace.getUid();
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		// anything to do here?
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

		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) resolveCompositeQuerySpace(
				loadPlanBuildingContext
		);
		final Join join = leftHandSide.addEntityJoin(
				attributeDefinition,
				fetchedPersister,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid(),
				attributeDefinition.isNullable()
		);
		final EntityFetch fetch = createEntityFetch( attributeDefinition, fetchStrategy, join, loadPlanBuildingContext );
		addFetch( fetch );
		return fetch;
	}

	protected EntityFetch createEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			Join fetchedJoin,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return new EntityFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy,
				fetchedJoin
		);

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
		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) resolveCompositeQuerySpace( loadPlanBuildingContext );
		final Join join = leftHandSide.addCompositeJoin(
				attributeDefinition,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid()
		);

		final NestedCompositeFetchImpl fetch = new NestedCompositeFetchImpl(
				this,
				attributeDefinition.getType(),
				(CompositeQuerySpace) join.getRightHandSide(),
				allowCollectionFetches,
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
		if ( !allowCollectionFetches ) {
			throw new WalkingException(
					String.format(
							"This composite path [%s] does not allow collection fetches (composite id or composite collection index/element",
							propertyPath.getFullPath()
					)
			);
		}

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

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return FETCH_STRATEGY;
	}

	@Override
	public Type getFetchedType() {
		return compositeType;
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public String getAdditionalJoinConditions() {
		return null;
	}

	@Override
	public Fetch[] getFetches() {
		return (fetches == null) ? NO_FETCHES : fetches.toArray( new Fetch[fetches.size()] );
	}


	// this is being removed to be more ogm/search friendly
	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}
}
