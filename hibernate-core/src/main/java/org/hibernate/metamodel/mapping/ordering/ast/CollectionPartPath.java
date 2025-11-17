/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.AbstractDomainPath;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.spi.NavigablePath;

/**
 * Represents a part of a `CollectionPart` (element or index descriptor) as a DomainPath
 *
 * @author Steve Ebersole
 */
public class CollectionPartPath extends AbstractDomainPath {
	private final NavigablePath navigablePath;
	private final PluralAttributePath lhs;
	private final CollectionPart referencedPart;

	CollectionPartPath(
			PluralAttributePath lhs,
			CollectionPart referencedPart) {
		this.lhs = lhs;
		this.referencedPart = referencedPart;

		this.navigablePath = lhs.getNavigablePath().append( referencedPart.getPartName() );
	}

	@Override
	public PluralAttributeMapping getPluralAttribute() {
		return lhs.getPluralAttribute();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public PluralAttributePath getLhs() {
		return lhs;
	}

	@Override
	public CollectionPart getReferenceModelPart() {
		return referencedPart;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		if ( referencedPart instanceof ModelPartContainer modelPartContainer ) {
			final ModelPart subPart = modelPartContainer.findSubPart( name, null );
			return new DomainPathContinuation( navigablePath.append( name ), this, subPart );
		}
		else {
			throw new PathResolutionException( name );
		}
	}
}
