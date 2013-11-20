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
package org.hibernate.loader.plan.build.internal.returns;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.internal.spaces.QuerySpaceHelper;
import org.hibernate.loader.plan.build.spi.ExpandingCollectionQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingEntityQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingFetchSource;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan.build.spi.ExpandingQuerySpaces;
import org.hibernate.loader.plan.spi.AnyAttributeFetch;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
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
	public EntityFetch buildEntityAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {

		final ExpandingEntityQuerySpace entityQuerySpace = QuerySpaceHelper.INSTANCE.makeEntityQuerySpace(
				expandingQuerySpace(),
				attributeDefinition,
				getQuerySpaces().generateImplicitUid(),
				fetchStrategy
		);
		final EntityFetch fetch = new EntityAttributeFetchImpl( this, attributeDefinition, fetchStrategy, entityQuerySpace );
		addFetch( fetch );
		return fetch;
	}

	@Override
	public BidirectionalEntityReference buildBidirectionalEntityReference(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			EntityReference targetEntityReference) {
		final EntityType fetchedType = (EntityType) attributeDefinition.getType();
		final EntityPersister fetchedPersister = attributeDefinition.toEntityDefinition().getEntityPersister();

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

	protected abstract CompositeAttributeFetch createCompositeAttributeFetch(
			AttributeDefinition compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace);

	protected ExpandingQuerySpaces getQuerySpaces() {
		return querySpace.getExpandingQuerySpaces();
	}

	@Override
	public CompositeAttributeFetch buildCompositeAttributeFetch(
			AttributeDefinition attributeDefinition) {
		final ExpandingCompositeQuerySpace compositeQuerySpace = QuerySpaceHelper.INSTANCE.makeCompositeQuerySpace(
				expandingQuerySpace(),
				attributeDefinition,
				getQuerySpaces().generateImplicitUid(),
				true
		);

		final CompositeAttributeFetch fetch = createCompositeAttributeFetch( attributeDefinition, compositeQuerySpace );
		addFetch( fetch );
		return fetch;
	}

	@Override
	public CollectionAttributeFetch buildCollectionAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {

		final ExpandingCollectionQuerySpace collectionQuerySpace = QuerySpaceHelper.INSTANCE.makeCollectionQuerySpace(
				querySpace,
				attributeDefinition,
				getQuerySpaces().generateImplicitUid(),
				fetchStrategy
		);

		final CollectionAttributeFetch fetch = new CollectionAttributeFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy,
				collectionQuerySpace
		);
		addFetch( fetch );
		return fetch;
	}

	@Override
	public AnyAttributeFetch buildAnyAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {

		final AnyAttributeFetch fetch = new AnyAttributeFetchImpl(
				this,
				attributeDefinition,
				fetchStrategy
		);
		addFetch( fetch );
		return fetch;
	}
}
