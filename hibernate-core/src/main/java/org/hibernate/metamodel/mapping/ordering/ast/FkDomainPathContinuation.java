/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.query.spi.NavigablePath;

public class FkDomainPathContinuation extends DomainPathContinuation {

	private final Set<String> possiblePaths;

	public FkDomainPathContinuation(
			NavigablePath navigablePath, DomainPath lhs,
			ToOneAttributeMapping referencedModelPart) {
		super( navigablePath, lhs, referencedModelPart );
		this.possiblePaths = referencedModelPart.getTargetKeyPropertyNames();
	}

	public FkDomainPathContinuation(
			NavigablePath navigablePath, DomainPath lhs,
			ModelPart referencedModelPart, Set<String> possiblePaths) {
		super( navigablePath, lhs, referencedModelPart );
		this.possiblePaths = possiblePaths;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		HashSet<String> furtherPaths = new LinkedHashSet<>( possiblePaths.size() );
		for ( String path : possiblePaths ) {
			if ( !path.startsWith( name ) ) {
				return new DomainPathContinuation(
						navigablePath.append( name ),
						this,
						// unfortunately at this stage the foreign key descriptor could not be set
						// on the attribute mapping yet, so we need to defer the sub part extraction later
						referencedModelPart
				);
			}

			furtherPaths.add( path.substring( name.length() + 1 ) );
		}

		if ( furtherPaths.isEmpty() ) {
			throw new PathResolutionException( "Domain path of type `" + referencedModelPart.getPartMappingType() + "` -> `" + name + "`" );
		}

		return new FkDomainPathContinuation(
				navigablePath.append( name ),
				this,
				( (ModelPartContainer) referencedModelPart ).findSubPart( name, null ),
				furtherPaths
		);
	}
}
