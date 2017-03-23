/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.collection.internal.CollectionElementBasicImpl;
import org.hibernate.persister.collection.internal.CollectionElementEntityImpl;
import org.hibernate.persister.collection.internal.CollectionIndexBasicImpl;
import org.hibernate.persister.collection.spi.CollectionElementBasic;
import org.hibernate.persister.collection.spi.CollectionElementEmbedded;
import org.hibernate.persister.collection.spi.CollectionElementEntity;
import org.hibernate.persister.collection.spi.CollectionIndexBasic;
import org.hibernate.persister.collection.spi.CollectionIndexEmbedded;
import org.hibernate.persister.collection.spi.CollectionIndexEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.SingularPersistentAttributeBasic;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.RowIdDescriptor;
import org.hibernate.persister.entity.spi.VersionDescriptor;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;

/**
 * Visitation strategy for walking Hibernate's Navigable graph trees.  Following visitor pattern
 * this contract would serve the role of visitor which each node accepts.
 *
 *
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
public interface NavigableVisitationStrategy {
	// todo (6.0) : use listener approach or visitor approach here?
	//		currently this contract leverages the "listener" approach to graph walking, whereas in all other places we
	//		leverage visitors.  A listener has certain usefulness here, but considering we so need the capability to
	//		sometimes skip the processing of children - I think visitor may still be the best choice here

	// todo (6.0) : many methods here deal with internal types - we need to develop API/SPI counterparts to these
	//		^^ API if we want to allow applications to use this - maybe something like:
	//		Session/SessionFactory#visit(String entityName, NavigableVisitationStrategy visitor)
	//		Session/SessionFactory#visit(Class entityJavaType, NavigableVisitationStrategy visitor)

	@Deprecated
	default void start() {
		prepareForVisitation();
	}

	/**
	 * Notification we are preparing to start visitation.
	 */
	void prepareForVisitation();

	@Deprecated
	default void finish() {
		visitationComplete();
	}

	/**
	 * Notification we are finished visitation.
	 */
	void visitationComplete();

	/**
	 * Should only happen as a root
	 */
	<J> void visitNoInheritanceEntity(EntityPersister<J> persister);

	/**
	 * Should only happen as a root
	 */
	<J> void visitDiscriminatedInheritanceEntity(EntityPersister<J> persister);

	/**
	 * Should only happen as a root
	 */
	<J> void visitJoinedInheritanceEntity(EntityPersister<J> persister);

	/**
	 * Should only happen as a root
	 */
	<J> void visitUnionInheritanceEntity(EntityPersister<J> persister);

	<J> void visitSimpleIdentifier(EntityHierarchy hierarchy, SingularPersistentAttribute<?,J> idAttribute);

	<J> void visitNonAggregatedCompositeIdentifier(EntityHierarchy hierarchy, EmbeddedPersister<J> embeddedPersister);

	<J> void visitAggregatedCompositeIdentifier(EntityHierarchy hierarchy, EmbeddedPersister<J> embeddedPersister);

	<O, J> void visitVersion(VersionDescriptor versionDescriptor);

	<O, J> void visitRowId(RowIdDescriptor<O, J> rowIdDescriptor);


	<O,J> void visitSingularAttributeBasic(SingularPersistentAttributeBasic<O,J> attribute);
	<O,J> void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded<O,J> attribute);
	<O,J> void visitSingularAttributeEntity(SingularPersistentAttributeEntity<O,J> attribute);

	// todo (6.0) : differentiate between many-to-one, one-to-one and element-collection?
	//		the difficulty with that is that associations are actually relative
	//		to element and index, not the collection itself, even though JPA and Hibernate (historically)
	//		represented it that way.
	<O,C,E> void visitPluralAttribute(CollectionPersister<O,C,E> pluralAttribute);

	<J> void visitCollectionElementBasic(CollectionElementBasic<J> element);
	<J> void visitCollectionElementEmbedded(CollectionElementEmbedded<J> element);
	<J> void visitCollectionElementEntity(CollectionElementEntity<J> element);

	<J> void visitCollectionIndexBasic(CollectionIndexBasic<J> index);
	<J> void visitCollectionIndexEmbedded(CollectionIndexEmbedded<J> index);
	<J> void visitCollectionIndexEntity(CollectionIndexEntity<J> index);



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
