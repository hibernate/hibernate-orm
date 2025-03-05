/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.AbstractDomainPath;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.spi.NavigablePath;

/**
 * Represents the collection as a DomainPath
 *
 * @author Steve Ebersole
 * @see RootSequencePart
 */
public class PluralAttributePath extends AbstractDomainPath {
	private final NavigablePath navigablePath;
	private final PluralAttributeMapping pluralAttributeMapping;

	PluralAttributePath(PluralAttributeMapping pluralAttributeMapping) {
		this.navigablePath = new NavigablePath( pluralAttributeMapping.getRootPathName() );
		this.pluralAttributeMapping = pluralAttributeMapping;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public DomainPath getLhs() {
		return null;
	}

	@Override
	public PluralAttributeMapping getReferenceModelPart() {
		return pluralAttributeMapping;
	}

	@Override
	public DomainPath resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		final ModelPart subPart = pluralAttributeMapping.findSubPart( name, null );

		if ( subPart != null ) {
			if ( subPart instanceof CollectionPart ) {
				return new CollectionPartPath( this, (CollectionPart) subPart );
			}
			if ( subPart instanceof EmbeddableValuedModelPart ) {
				return new DomainPathContinuation( navigablePath.append( name ), this, subPart );
			}
			if ( subPart instanceof ToOneAttributeMapping ) {
				return new FkDomainPathContinuation(
						navigablePath.append( name ),
						this,
						(ToOneAttributeMapping) subPart
				);
			}

			// leaf case:
			final CollectionPartPath elementPath = new CollectionPartPath(
					this,
					pluralAttributeMapping.getElementDescriptor()
			);

			return (DomainPath) elementPath.resolvePathPart( name, identifier, isTerminal, translationContext);
		}

		// the above checks for explicit element or index descriptor references
		// 		try also as an implicit element or index sub-part reference...

		if ( pluralAttributeMapping.getElementDescriptor() instanceof EmbeddableValuedModelPart elementDescriptor ) {
			final ModelPart elementSubPart = elementDescriptor.findSubPart( name, null );
			if ( elementSubPart != null ) {
				// create the CollectionSubPath to use as the `lhs` for the element sub-path
				final CollectionPartPath elementPath = new CollectionPartPath(
						this,
						(CollectionPart) elementDescriptor
				);

				return new DomainPathContinuation(
						elementPath.getNavigablePath().append( name ),
						this,
						elementSubPart
				);
			}
		}

		if ( pluralAttributeMapping.getIndexDescriptor() instanceof EmbeddableValuedModelPart indexDescriptor ) {
			final ModelPart indexSubPart = indexDescriptor.findSubPart( name, null );
			if ( indexSubPart != null ) {
				// create the CollectionSubPath to use as the `lhs` for the element sub-path
				final CollectionPartPath indexPath = new CollectionPartPath(
						this,
						(CollectionPart) indexDescriptor
				);
				return new DomainPathContinuation(
						indexPath.getNavigablePath().append( name ),
						this,
						indexSubPart
				);
			}
		}

		return null;
	}
}
