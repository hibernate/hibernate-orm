/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.AbstractDomainPath;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.spi.NavigablePath;

/**
 * A path relative to either a CollectionPartPath (element/index DomainPath) or another DomainPathContinuation
 *
 * @author Steve Ebersole
 */
public class DomainPathContinuation extends AbstractDomainPath {
	protected final NavigablePath navigablePath;
	protected final ModelPart referencedModelPart;

	private final DomainPath lhs;

	public DomainPathContinuation(NavigablePath navigablePath, DomainPath lhs, ModelPart referencedModelPart) {
		this.navigablePath = navigablePath;
		this.lhs = lhs;
		this.referencedModelPart = referencedModelPart;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public DomainPath getLhs() {
		return lhs;
	}

	@Override
	public ModelPart getReferenceModelPart() {
		return referencedModelPart;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		if ( referencedModelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			final var embeddableMappingType = embeddableValuedModelPart.getEmbeddableTypeDescriptor();
			final ModelPart subPart = embeddableMappingType.findSubPart( name, null );
			if ( subPart == null ) {
				throw new PathResolutionException( name );
			}

			return new DomainPathContinuation(
					navigablePath.append( name ),
					this,
					subPart
			);
		}

		throw new PathResolutionException( name );
	}
}
