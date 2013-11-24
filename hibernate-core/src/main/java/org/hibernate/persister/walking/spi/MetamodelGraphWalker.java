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

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Implements metamodel graph walking.  In layman terms, we are walking the graph of the users domain model as
 * defined/understood by mapped associations.
 * <p/>
 * Initially grew as a part of the re-implementation of the legacy JoinWalker functionality to instead build LoadPlans.
 * But this is really quite simple walking.  Interesting events are handled by calling out to
 * implementations of {@link AssociationVisitationStrategy} which really provide the real functionality of what we do
 * as we walk.
 * <p/>
 * The visitor will walk the entire metamodel graph (the parts reachable from the given root)!!!  It is up to the
 * provided AssociationVisitationStrategy to tell it when to stop.  The walker provides the walking; the strategy
 * provides the semantics of what happens at certain points.  Its really very similar to parsers and how parsing is
 * generally split between syntax and semantics.  Walker walks the syntax (associations, identifiers, etc) and when it
 * calls out to the strategy the strategy then decides the semantics (literally, the meaning).
 * <p/>
 * The visitor will, however, stop if it sees a "duplicate" AssociationKey.  In such a case, the walker would call
 * {@link AssociationVisitationStrategy#foundCircularAssociation} and stop walking any further down that graph any
 * further.
 *
 * @author Steve Ebersole
 */
public class MetamodelGraphWalker {
	private static final Logger log = Logger.getLogger( MetamodelGraphWalker.class );

	/**
	 * Entry point into walking the model graph of an entity according to its defined metamodel.
	 *
	 * @param strategy The semantics strategy
	 * @param persister The persister describing the entity to start walking from
	 */
	public static void visitEntity(AssociationVisitationStrategy strategy, EntityPersister persister) {
		strategy.start();
		try {
			new MetamodelGraphWalker( strategy, persister.getFactory() )
					.visitEntityDefinition( persister );
		}
		finally {
			strategy.finish();
		}
	}

	/**
	 * Entry point into walking the model graph of a collection according to its defined metamodel.
	 *
	 * @param strategy The semantics strategy
	 * @param persister The persister describing the collection to start walking from
	 */
	public static void visitCollection(AssociationVisitationStrategy strategy, CollectionPersister persister) {
		strategy.start();
		try {
			new MetamodelGraphWalker( strategy, persister.getFactory() )
					.visitCollectionDefinition( persister );
		}
		finally {
			strategy.finish();
		}
	}

	private final AssociationVisitationStrategy strategy;
	private final SessionFactoryImplementor factory;

	// todo : add a getDepth() method to PropertyPath
	private PropertyPath currentPropertyPath = new PropertyPath();

	public MetamodelGraphWalker(AssociationVisitationStrategy strategy, SessionFactoryImplementor factory) {
		this.strategy = strategy;
		this.factory = factory;
	}

	private void visitEntityDefinition(EntityDefinition entityDefinition) {
		strategy.startingEntity( entityDefinition );

		visitIdentifierDefinition( entityDefinition.getEntityKeyDefinition() );
		visitAttributes( entityDefinition );

		strategy.finishingEntity( entityDefinition );
	}

	private void visitIdentifierDefinition(EntityIdentifierDefinition identifierDefinition) {
		strategy.startingEntityIdentifier( identifierDefinition );

		// to make encapsulated and non-encapsulated composite identifiers work the same here, we "cheat" here a
		// little bit and simply walk the attributes of the composite id in both cases.

		// this works because the LoadPlans already build the top-level composite for composite ids

		if ( identifierDefinition.isEncapsulated() ) {
			// in the encapsulated composite id case that means we have a little bit of duplication between here and
			// visitCompositeDefinition, but in the spirit of consistently handling composite ids, that is much better
			// solution...
			final EncapsulatedEntityIdentifierDefinition idAsEncapsulated = (EncapsulatedEntityIdentifierDefinition) identifierDefinition;
			final AttributeDefinition idAttr = idAsEncapsulated.getAttributeDefinition();
			if ( CompositionDefinition.class.isInstance( idAttr ) ) {
				visitCompositeDefinition( (CompositionDefinition) idAttr );
			}
		}
		else {
			// NonEncapsulatedEntityIdentifierDefinition itself is defined as a CompositionDefinition
			visitCompositeDefinition( (NonEncapsulatedEntityIdentifierDefinition) identifierDefinition );
		}

		strategy.finishingEntityIdentifier( identifierDefinition );
	}

	private void visitAttributes(AttributeSource attributeSource) {
		final Iterable<AttributeDefinition> attributeDefinitions = attributeSource.getAttributes();
		if ( attributeDefinitions == null ) {
			return;
		}
		for ( AttributeDefinition attributeDefinition : attributeSource.getAttributes() ) {
			visitAttributeDefinition( attributeDefinition );
		}
	}

	private void visitAttributeDefinition(AttributeDefinition attributeDefinition) {
		final PropertyPath subPath = currentPropertyPath.append( attributeDefinition.getName() );
		log.debug( "Visiting attribute path : " + subPath.getFullPath() );


		if ( attributeDefinition.getType().isAssociationType() ) {
			final AssociationAttributeDefinition associationAttributeDefinition =
					(AssociationAttributeDefinition) attributeDefinition;
			final AssociationKey associationKey = associationAttributeDefinition.getAssociationKey();
			if ( isDuplicateAssociationKey( associationKey ) ) {
				log.debug( "Property path deemed to be circular : " + subPath.getFullPath() );
				strategy.foundCircularAssociation( associationAttributeDefinition );
				// EARLY EXIT!!!
				return;
			}
		}


		boolean continueWalk = strategy.startingAttribute( attributeDefinition );
		if ( continueWalk ) {
			final PropertyPath old = currentPropertyPath;
			currentPropertyPath = subPath;
			try {
				final Type attributeType = attributeDefinition.getType();
				if ( attributeType.isAssociationType() ) {
					visitAssociation( (AssociationAttributeDefinition) attributeDefinition );
				}
				else if ( attributeType.isComponentType() ) {
					visitCompositeDefinition( (CompositionDefinition) attributeDefinition );
				}
			}
			finally {
				currentPropertyPath = old;
			}
		}
		strategy.finishingAttribute( attributeDefinition );
	}

	private void visitAssociation(AssociationAttributeDefinition attribute) {
		// todo : do "too deep" checks; but see note about adding depth to PropertyPath
		//
		// may also need to better account for "composite fetches" in terms of "depth".

		addAssociationKey( attribute.getAssociationKey() );

		final AssociationAttributeDefinition.AssociationNature nature = attribute.getAssociationNature();
		if ( nature == AssociationAttributeDefinition.AssociationNature.ANY ) {
			visitAnyDefinition( attribute.toAnyDefinition() );
		}
		else if ( nature == AssociationAttributeDefinition.AssociationNature.COLLECTION ) {
			visitCollectionDefinition( attribute.toCollectionDefinition() );
		}
		else {
			visitEntityDefinition( attribute.toEntityDefinition() );
		}
	}

	private void visitAnyDefinition(AnyMappingDefinition anyDefinition) {
		strategy.foundAny( anyDefinition );
	}

	private void visitCompositeDefinition(CompositionDefinition compositionDefinition) {
		strategy.startingComposite( compositionDefinition );

		visitAttributes( compositionDefinition );

		strategy.finishingComposite( compositionDefinition );
	}

	private void visitCollectionDefinition(CollectionDefinition collectionDefinition) {
		strategy.startingCollection( collectionDefinition );

		visitCollectionIndex( collectionDefinition );
		visitCollectionElements( collectionDefinition );

		strategy.finishingCollection( collectionDefinition );
	}

	private void visitCollectionIndex(CollectionDefinition collectionDefinition) {
		final CollectionIndexDefinition collectionIndexDefinition = collectionDefinition.getIndexDefinition();
		if ( collectionIndexDefinition == null ) {
			return;
		}

		strategy.startingCollectionIndex( collectionIndexDefinition );

		log.debug( "Visiting index for collection :  " + currentPropertyPath.getFullPath() );
		currentPropertyPath = currentPropertyPath.append( "<index>" );

		try {
			final Type collectionIndexType = collectionIndexDefinition.getType();
			if ( collectionIndexType.isAnyType() ) {
				visitAnyDefinition( collectionIndexDefinition.toAnyMappingDefinition() );
			}
			else if ( collectionIndexType.isComponentType() ) {
				visitCompositeDefinition( collectionIndexDefinition.toCompositeDefinition() );
			}
			else if ( collectionIndexType.isAssociationType() ) {
				visitEntityDefinition( collectionIndexDefinition.toEntityDefinition() );
			}
		}
		finally {
			currentPropertyPath = currentPropertyPath.getParent();
		}

		strategy.finishingCollectionIndex( collectionIndexDefinition );
	}

	private void visitCollectionElements(CollectionDefinition collectionDefinition) {
		final CollectionElementDefinition elementDefinition = collectionDefinition.getElementDefinition();
		strategy.startingCollectionElements( elementDefinition );

		final Type collectionElementType = elementDefinition.getType();
		if ( collectionElementType.isAnyType() ) {
			visitAnyDefinition( elementDefinition.toAnyMappingDefinition() );
		}
		else if ( collectionElementType.isComponentType() ) {
			visitCompositeDefinition( elementDefinition.toCompositeElementDefinition() );
		}
		else if ( collectionElementType.isEntityType() ) {
			if ( ! collectionDefinition.getCollectionPersister().isOneToMany() ) {
				final QueryableCollection queryableCollection = (QueryableCollection) collectionDefinition.getCollectionPersister();
				addAssociationKey(
						new AssociationKey(
								queryableCollection.getTableName(),
								queryableCollection.getElementColumnNames()
						)
				);
			}
			visitEntityDefinition( elementDefinition.toEntityDefinition() );
		}

		strategy.finishingCollectionElements( elementDefinition );
	}

	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<AssociationKey>();

	/**
	 * Add association key to indicate the association is being visited.
	 * @param associationKey - the association key.
	 * @throws WalkingException if the association with the specified association key
	 *                          has already been visited.
	 */
	protected void addAssociationKey(AssociationKey associationKey) {
		if ( ! visitedAssociationKeys.add( associationKey ) ) {
			throw new WalkingException(
					String.format( "Association has already been visited: %s", associationKey )
			);
		}
		strategy.associationKeyRegistered( associationKey );
	}

	/**
	 * Has an association with the specified key been visited already?
	 * @param associationKey - the association key.
	 * @return true, if the association with the specified association key has already been visited;
	 *         false, otherwise.
	 */
	protected boolean isDuplicateAssociationKey(AssociationKey associationKey) {
		return visitedAssociationKeys.contains( associationKey ) || strategy.isDuplicateAssociationKey( associationKey );
	}
}
