/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.plan.spi.AnyAttributeFetch;
import org.hibernate.loader.plan.spi.BidirectionalEntityReference;
import org.hibernate.loader.plan.spi.CollectionAttributeFetch;
import org.hibernate.loader.plan.spi.CompositeAttributeFetch;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;

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
	 * @param fetchStrategy The type of fetch to validate.
	 * @param attributeDefinition The attribute to be fetched.
	 */
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition);

	/**
	 * Builds a fetch for an entity attribute.
	 *
	 * @param attributeDefinition The entity attribute.
	 * @param fetchStrategy The fetch strategy for the attribute.
	 * @return The entity fetch.
	 */
	public EntityFetch buildEntityAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy);

	/**
	 * Builds a bidirectional entity reference for an entity attribute.
	 *
	 * @param attributeDefinition The attribute definition.
	 * @param fetchStrategy The fetch strategy for the attribute.
	 * @param targetEntityReference The associated (target) entity reference.
	 * @return The bidirectional entity reference.
	 */
	public BidirectionalEntityReference buildBidirectionalEntityReference(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy,
			EntityReference targetEntityReference);

	/**
	 * Builds a fetch for a composite attribute.
	 *
	 * @param attributeDefinition The attribute definition.
	 * @return The composite attribute fetch.
	 */
	public CompositeAttributeFetch buildCompositeAttributeFetch(
			AttributeDefinition attributeDefinition);

	/**
	 * Builds a fetch for a collection attribute.
	 *
	 * @param attributeDefinition The attribute definition.
	 * @param fetchStrategy The fetch strategy for the attribute.
	 * @return The collection attribute fetch.
	 */
	public CollectionAttributeFetch buildCollectionAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy);

	/**
	 * Builds a fetch for an "any" attribute.
	 *
	 * @param attributeDefinition The attribute definition.
	 * @param fetchStrategy The fetch strategy for the attibute.
	 * @return The "any" attribute fetch.
	 */
	public AnyAttributeFetch buildAnyAttributeFetch(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy);
}
