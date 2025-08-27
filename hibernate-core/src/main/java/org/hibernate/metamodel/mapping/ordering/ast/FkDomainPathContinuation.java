/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.spi.NavigablePath;

public class FkDomainPathContinuation extends DomainPathContinuation {

	private final Set<String> possiblePaths;

	public FkDomainPathContinuation(
			NavigablePath navigablePath,
			DomainPath lhs,
			ToOneAttributeMapping referencedModelPart) {
		super( navigablePath, lhs, referencedModelPart );
		this.possiblePaths = referencedModelPart.getTargetKeyPropertyNames();
	}

	public FkDomainPathContinuation(
			NavigablePath navigablePath,
			DomainPath lhs,
			ModelPart referencedModelPart,
			Set<String> possiblePaths) {
		super( navigablePath, lhs, referencedModelPart );
		this.possiblePaths = possiblePaths;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		if ( !possiblePaths.contains( name ) ) {
			throw new PathResolutionException( name );
		}

		final HashSet<String> furtherPaths = new HashSet<>();
		for ( String possiblePath : possiblePaths ) {
			if ( possiblePath.startsWith( name ) && possiblePath.length() > name.length()
					&& possiblePath.charAt( name.length() ) == '.' ) {
				furtherPaths.add( possiblePath.substring( name.length() + 2 ) );
			}
		}
		return new FkDomainPathContinuation(
				navigablePath.append( name ),
				this,
				( (ModelPartContainer) referencedModelPart ).findSubPart( name, null ),
				furtherPaths
		);
	}
}
