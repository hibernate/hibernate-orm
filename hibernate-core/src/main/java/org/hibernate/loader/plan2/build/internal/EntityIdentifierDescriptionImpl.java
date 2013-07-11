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
package org.hibernate.loader.plan2.build.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan2.build.spi.ExpandingQuerySpace;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.internal.EntityFetchImpl;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.Fetch;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierDescriptionImpl implements ExpandingEntityIdentifierDescription {
	private final EntityReference entityReference;
	private final PropertyPath propertyPath;

	private List<Fetch> fetches;

	public EntityIdentifierDescriptionImpl(EntityReference entityReference) {
		this.entityReference = entityReference;
		this.propertyPath = entityReference.getPropertyPath().append( "<id>" );
	}

	// IdentifierDescription impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public Fetch[] getFetches() {
		return fetches == null ? NO_FETCHES : fetches.toArray( new Fetch[ fetches.size() ] );
	}


	// FetchContainer impl ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
		if ( attributeDefinition.getType().isCollectionType() ) {
			throw new WalkingException(
					"Encountered collection attribute in identifier fetches: " + attributeDefinition.getSource() +
							"." + attributeDefinition.getName()
			);
		}
		// todo : allow bi-directional key-many-to-one fetches?
		//		those do cause problems in Loader; question is whether those are indicative of that situation or
		// 		of Loaders ability to handle it.
	}

	@Override
	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		// we have a key-many-to-one
		//
		// IMPL NOTE: we pass ourselves as the FetchOwner which will route the fetch back through our #addFetch
		// 		impl.  We collect them there and later build the IdentifierDescription

		// if `this` is a fetch and its owner is "the same" (bi-directionality) as the attribute to be join fetched
		// we should wrap our FetchOwner as an EntityFetch.  That should solve everything except for the alias
		// context lookups because of the different instances (because of wrapping).  So somehow the consumer of this
		// needs to be able to unwrap it to do the alias lookup, and would have to know to do that.
		//
		//
		// we are processing the EntityReference(Address) identifier.  we come across its key-many-to-one reference
		// to Person.  Now, if EntityReference(Address) is an instance of EntityFetch(Address) there is a strong
		// likelihood that we have a bi-directionality and need to handle that specially.
		//
		// how to best (a) find the bi-directionality and (b) represent that?

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

		if ( EntityFetch.class.isInstance( entityReference ) ) {
//			// we just confirmed that EntityReference(Address) is an instance of EntityFetch(Address),
//			final EntityFetch entityFetch = (EntityFetch) entityReference;
//			final FetchSource entityFetchSource = entityFetch.getSource();
//			// so at this point we need to see if entityFetchSource and attributeDefinition refer to the
//			// "same thing".  "same thing" == "same type" && "same column(s)"?
//			//
//			// i make assumptions here that that the attribute type is the EntityType, is that always valid?
//
//			final boolean sameType = fetchedPersister.getEntityName().equals(
//					entityFetchSource.retrieveFetchSourcePersister().getEntityName()
//			);
//
//			if ( sameType ) {
//				// check same columns as well?
//
//				return new KeyManyToOneBidirectionalEntityFetch(
//						sessionFactory(),
//						//ugh
//						LockMode.READ,
//						this,
//						attributeDefinition,
//						(EntityReference) entityFetchSource,
//						fetchStrategy
//				);
//			}
		}

		final ExpandingQuerySpace leftHandSide = (ExpandingQuerySpace) loadPlanBuildingContext.getQuerySpaces().findQuerySpaceByUid(
				entityReference.getQuerySpaceUid()
		);
		Join join = leftHandSide.addEntityJoin(
				attributeDefinition,
				fetchedPersister,
				loadPlanBuildingContext.getQuerySpaces().generateImplicitUid(),
				attributeDefinition.isNullable()
		);
		final EntityFetch fetch = new EntityFetchImpl(
				this,
				attributeDefinition,
				fetchedPersister,
				fetchStrategy,
				join
		);
		addFetch( fetch );

//		this.sqlSelectFragmentResolver = new EntityPersisterBasedSqlSelectFragmentResolver( (Queryable) persister );

		return fetch;
	}

	private void addFetch(EntityFetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<Fetch>();
		}
		fetches.add( fetch );
	}

	@Override
	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		throw new WalkingException( "Entity identifier cannot contain persistent collections" );
	}

}
