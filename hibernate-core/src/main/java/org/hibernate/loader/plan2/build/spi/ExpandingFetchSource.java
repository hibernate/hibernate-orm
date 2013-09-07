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
package org.hibernate.loader.plan2.build.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan2.spi.BidirectionalEntityFetch;
import org.hibernate.loader.plan2.spi.CollectionFetch;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.EntityFetch;
import org.hibernate.loader.plan2.spi.EntityReference;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;

/**
 * Describes the internal contract for things which can contain fetches.  Used to request building
 * the different types of fetches.
 *
 * @author Steve Ebersole
 */
public interface ExpandingFetchSource extends FetchSource {
	/**
	 * Is the asserted plan valid from this owner to a fetch?
	 *
	 * @param fetchStrategy The type of fetch to validate
	 * @param attributeDefinition The attribute to be fetched
	 */
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition);

	public EntityFetch buildEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext);

	public BidirectionalEntityFetch buildBidirectionalEntityFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			EntityReference targetEntityReference,
			LoadPlanBuildingContext loadPlanBuildingContext);

	public CompositeFetch buildCompositeFetch(
			CompositionDefinition attributeDefinition,
			LoadPlanBuildingContext loadPlanBuildingContext);

	public CollectionFetch buildCollectionFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			LoadPlanBuildingContext loadPlanBuildingContext);

}
