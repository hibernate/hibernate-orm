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

import org.jboss.logging.Logger;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Provides model graph visitation based on the defined metadata (as opposed to based on the incoming graph
 * as we see in cascade processing).  In layman terms, we are walking the graph of the users model as defined by
 * mapped associations.
 * <p/>
 * Re-implementation of the legacy {@link org.hibernate.loader.JoinWalker} contract to leverage load plans.
 *
 * @author Steve Ebersole
 */
public class MetadataDrivenModelGraphVisitor {
	private static final Logger log = Logger.getLogger( MetadataDrivenModelGraphVisitor.class );

	public static void visitEntity(AssociationVisitationStrategy strategy, EntityPersister persister) {
		strategy.start();
		try {
			new MetadataDrivenModelGraphVisitor( strategy, persister.getFactory() )
					.visitEntityDefinition( persister );
		}
		finally {
			strategy.finish();
		}
	}

	public static void visitCollection(AssociationVisitationStrategy strategy, CollectionPersister persister) {
		strategy.start();
		try {
			new MetadataDrivenModelGraphVisitor( strategy, persister.getFactory() )
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

	public MetadataDrivenModelGraphVisitor(AssociationVisitationStrategy strategy, SessionFactoryImplementor factory) {
		this.strategy = strategy;
		this.factory = factory;
	}

	private void visitEntityDefinition(EntityDefinition entityDefinition) {
		strategy.startingEntity( entityDefinition );
		try {
			visitAttributes( entityDefinition );
			optionallyVisitEmbeddedCompositeIdentifier( entityDefinition );
		}
		finally {
			strategy.finishingEntity( entityDefinition );
		}
	}

	private void optionallyVisitEmbeddedCompositeIdentifier(EntityDefinition entityDefinition) {
		// if the entity has a composite identifier, see if we need to handle its sub-properties separately
		final Iterable<AttributeDefinition> embeddedCompositeIdentifierAttributes =
				entityDefinition.getEmbeddedCompositeIdentifierAttributes();
		if ( embeddedCompositeIdentifierAttributes == null ) {
			return;
		}

		for ( AttributeDefinition attributeDefinition : embeddedCompositeIdentifierAttributes ) {
			visitAttributeDefinition( attributeDefinition );
		}
	}

	private void visitAttributes(AttributeSource attributeSource) {
		for ( AttributeDefinition attributeDefinition : attributeSource.getAttributes() ) {
			visitAttributeDefinition( attributeDefinition );
		}
	}

	private void visitAttributeDefinition(AttributeDefinition attributeDefinition) {
		final PropertyPath subPath = currentPropertyPath.append( attributeDefinition.getName() );
		log.debug( "Visiting attribute path : " + subPath.getFullPath() );

		final boolean continueWalk = strategy.startingAttribute( attributeDefinition );
		if ( continueWalk ) {
			final PropertyPath old = currentPropertyPath;
			currentPropertyPath = subPath;
			try {
				if ( attributeDefinition.getType().isAssociationType() ) {
					visitAssociation( (AssociationAttributeDefinition) attributeDefinition );
				}
				else if ( attributeDefinition.getType().isComponentType() ) {
					visitCompositeDefinition( (CompositeDefinition) attributeDefinition );
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

		if ( isDuplicateAssociation( attribute.getAssociationKey() ) ) {
			log.debug( "Property path deemed to be circular : " + currentPropertyPath.getFullPath() );
			return;
		}

		if ( attribute.isCollection() ) {
			visitCollectionDefinition( attribute.toCollectionDefinition() );
		}
		else {
			visitEntityDefinition( attribute.toEntityDefinition() );
		}
	}

	private void visitCompositeDefinition(CompositeDefinition compositeDefinition) {
		strategy.startingComposite( compositeDefinition );
		try {
			visitAttributes( compositeDefinition );
		}
		finally {
			strategy.finishingComposite( compositeDefinition );
		}
	}

	private void visitCollectionDefinition(CollectionDefinition collectionDefinition) {
		strategy.startingCollection( collectionDefinition );

		try {
			visitCollectionIndex( collectionDefinition.getIndexDefinition() );

			final CollectionElementDefinition elementDefinition = collectionDefinition.getElementDefinition();
			if ( elementDefinition.getType().isComponentType() ) {
				visitCompositeDefinition( elementDefinition.toCompositeDefinition() );
			}
			else {
				visitEntityDefinition( elementDefinition.toEntityDefinition() );
			}
		}
		finally {
			strategy.finishingCollection( collectionDefinition );
		}
	}

	private void visitCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		if ( collectionIndexDefinition == null ) {
			return;
		}

		log.debug( "Visiting collection index :  " + currentPropertyPath.getFullPath() );
		currentPropertyPath = currentPropertyPath.append( "<key>" );
		try {
			final Type collectionIndexType = collectionIndexDefinition.getType();
			if ( collectionIndexType.isComponentType() ) {
				visitCompositeDefinition( collectionIndexDefinition.toCompositeDefinition() );
			}
			else if ( collectionIndexType.isAssociationType() ) {
				visitEntityDefinition( collectionIndexDefinition.toEntityDefinition() );
			}
		}
		finally {
			currentPropertyPath = currentPropertyPath.getParent();
		}
	}


	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<AssociationKey>();

	protected boolean isDuplicateAssociation(AssociationKey associationKey) {
		return !visitedAssociationKeys.add( associationKey );
	}

}
