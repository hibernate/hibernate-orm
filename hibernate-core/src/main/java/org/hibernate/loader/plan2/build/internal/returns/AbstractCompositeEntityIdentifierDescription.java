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

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingEntityIdentifierDescription;
import org.hibernate.loader.plan2.build.spi.LoadPlanBuildingContext;
import org.hibernate.loader.plan2.spi.CompositeQuerySpace;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeEntityIdentifierDescription
		extends AbstractCompositeFetch
		implements FetchSource, ExpandingEntityIdentifierDescription {

	private final EntityReference entityReference;

	protected AbstractCompositeEntityIdentifierDescription(
			EntityReference entityReference,
			CompositeQuerySpace compositeQuerySpace,
			CompositeType identifierType,
			PropertyPath propertyPath) {
		super( identifierType, compositeQuerySpace, false, propertyPath );
		this.entityReference = entityReference;
	}

	@Override
	public boolean hasFetches() {
		return getFetches().length > 0;
	}

	@Override
	public FetchSource getSource() {
		// the source for this (as a Fetch) is the entity reference
		return entityReference;
	}

	@Override
	protected EntityFetch createEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			Join fetchedJoin,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		// we have a key-many-to-one
		//
		// IMPL NOTE: we pass ourselves as the ExpandingFetchSource to collect fetches and later build
		// the IdentifierDescription

		// if `this` is a fetch and its owner is "the same" (bi-directionality) as the attribute to be join fetched
		// we should wrap our FetchSource as an EntityFetch.  That should solve everything except for the alias
		// context lookups because of the different instances (because of wrapping).  So somehow the consumer of this
		// needs to be able to unwrap it to do the alias lookup, and would have to know to do that.
		//
		//
		// we are processing the EntityReference(Address) identifier.  we come across its key-many-to-one reference
		// to Person.  Now, if EntityReference(Address) is an instance of EntityFetch(Address) there is a strong
		// likelihood that we have a bi-directionality and need to handle that specially.
		//
		// how to best (a) find the bi-directionality and (b) represent that?

		final FetchSource registeredFetchSource = loadPlanBuildingContext.registeredFetchSource(
				attributeDefinition.getAssociationKey()
		);
		if ( isKeyManyToOneBidirectionalAttributeDefinition( attributeDefinition, loadPlanBuildingContext ) ) {
			return new KeyManyToOneBidirectionalEntityFetchImpl(
					this,
					attributeDefinition,
					fetchStrategy,
					fetchedJoin,
					registeredFetchSource.resolveEntityReference()
			);
		}
		else {
			return super.createEntityFetch( attributeDefinition, fetchStrategy, fetchedJoin, loadPlanBuildingContext );
		}
	}

	private boolean isKeyManyToOneBidirectionalAttributeDefinition(
			AssociationAttributeDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext) {
		final FetchSource registeredFetchSource = loadPlanBuildingContext.registeredFetchSource(
				attributeDefinition.getAssociationKey()
		);
		return registeredFetchSource != null && registeredFetchSource != getSource();
	}
}
