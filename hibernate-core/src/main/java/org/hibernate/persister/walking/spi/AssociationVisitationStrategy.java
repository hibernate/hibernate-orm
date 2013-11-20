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
package org.hibernate.persister.walking.spi;

import org.hibernate.loader.plan.spi.FetchSource;

/**
 * Strategy for walking associations as defined by the Hibernate metamodel.  Is essentially a callback listener for
 * interesting events while walking a metamodel graph
 * <p/>
 * {@link #start()} and {@link #finish()} are called at the start and at the finish of the process.
 * <p/>
 * Walking might start with an entity or a collection depending on where the walker is asked to start.  When starting
 * with an entity, {@link #startingEntity}/{@link #finishingEntity} ()} will be the outer set of calls.  When starting
 * with a collection, {@link #startingCollection}/{@link #finishingCollection} will be the outer set of calls.
 *
 * @author Steve Ebersole
 */
public interface AssociationVisitationStrategy {
	/**
	 * Notification we are preparing to start visitation.
	 */
	public void start();

	/**
	 * Notification we are finished visitation.
	 */
	public void finish();

	/**
	 * Notification we are starting to walk an entity.
	 *
	 * @param entityDefinition The entity we are preparing to walk
	 */
	public void startingEntity(EntityDefinition entityDefinition);

	/**
	 * Notification we are finishing walking an entity.
	 *
	 * @param entityDefinition The entity we are finishing walking.
	 */
	public void finishingEntity(EntityDefinition entityDefinition);

	/**
	 * Notification we are starting to walk the identifier of an entity.
	 *
	 * @param entityIdentifierDefinition The identifier we are preparing to walk
	 */
	public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition);

	/**
	 * Notification we are finishing walking an entity.
	 *
	 * @param entityIdentifierDefinition The identifier we are finishing walking.
	 */
	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition);

	/**
	 * Notification that we are starting to walk a collection
	 *
	 * @param collectionDefinition The collection we are preparing to walk
	 */
	public void startingCollection(CollectionDefinition collectionDefinition);

	/**
	 * Notification that we are finishing walking a collection
	 *
	 * @param collectionDefinition The collection we are finishing
	 */
	public void finishingCollection(CollectionDefinition collectionDefinition);

	/**
	 * Notification that we are starting to walk the index of a collection (List/Map).  In the case of a Map,
	 * if the indices (the keys) are entities this will be followed up by a call to {@link #startingEntity}
	 *
	 * @param collectionIndexDefinition The collection index we are preparing to walk.
	 */
	public void startingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition);

	/**
	 * Notification that we are finishing walking the index of a collection (List/Map).
	 *
	 * @param collectionIndexDefinition The collection index we are finishing
	 */
	public void finishingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition);

	/**
	 * Notification that we are starting to look at the element definition for the collection.  If the collection
	 * elements are entities this will be followed up by a call to {@link #startingEntity}
	 *
	 * @param elementDefinition The collection element we are preparing to walk..
	 */
	public void startingCollectionElements(CollectionElementDefinition elementDefinition);

	/**
	 * Notification that we are finishing walking the elements of a collection (List/Map).
	 *
	 * @param elementDefinition The collection element we are finishing
	 */
	public void finishingCollectionElements(CollectionElementDefinition elementDefinition);

	/**
	 * Notification that we are preparing to walk a composite.  This is called only for:<ul>
	 *     <li>
	 *         top-level composites for entity attributes. composite entity identifiers do not route through here, see
	 *         {@link #startingEntityIdentifier} if you need to hook into walking the top-level cid composite.
	 *     </li>
	 *     <li>
	 *         All forms of nested composite paths
	 *     </li>
	 * </ul>
	 *
	 * @param compositionDefinition The composite we are preparing to walk.
	 */
	public void startingComposite(CompositionDefinition compositionDefinition);

	/**
	 * Notification that we are done walking a composite.  Called on the back-end of the situations listed
	 * on {@link #startingComposite}
	 *
	 * @param compositionDefinition The composite we are finishing
	 */
	public void finishingComposite(CompositionDefinition compositionDefinition);

	// get rid of these ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	public void startingCompositeCollectionElement(CompositeCollectionElementDefinition compositionElementDefinition);
//	public void finishingCompositeCollectionElement(CompositeCollectionElementDefinition compositionElementDefinition);
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Notification that we are preparing to walk an attribute.  May be followed by calls to {@link #startingEntity}
	 * (one-to-one, many-to-one), {@link #startingComposite}, or {@link #startingCollection}.
	 *
	 * @param attributeDefinition The attribute we are preparing to walk.
	 *
	 * @return {@code true} if the walking should continue; {@code false} if walking should stop.
	 */
	public boolean startingAttribute(AttributeDefinition attributeDefinition);

	/**
	 * Notification that we are finishing walking an attribute.
	 *
	 * @param attributeDefinition The attribute we are done walking
	 */
	public void finishingAttribute(AttributeDefinition attributeDefinition);

	public void foundAny(AnyMappingDefinition anyDefinition);

	public void associationKeyRegistered(AssociationKey associationKey);
	public FetchSource registeredFetchSource(AssociationKey associationKey);
	public void foundCircularAssociation(AssociationAttributeDefinition attributeDefinition);
	public boolean isDuplicateAssociationKey(AssociationKey associationKey);

}
