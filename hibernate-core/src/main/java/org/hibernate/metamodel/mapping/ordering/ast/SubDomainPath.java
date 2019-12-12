/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingTypedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;

/**
 * @author Steve Ebersole
 */
public class SubDomainPath implements DomainPath {
	private final DomainPath lhs;
	private final ModelPart referencedModelPart;

	public SubDomainPath(DomainPath lhs, ModelPart referencedModelPart) {
		this.lhs = lhs;
		this.referencedModelPart = referencedModelPart;
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
			boolean isTerminal,
			TranslationContext translationContext) {
		if ( referencedModelPart.getPartMappingType() instanceof ManagedMappingType ) {
			final ManagedMappingType partMappingType = (ManagedMappingType) referencedModelPart.getPartMappingType();
			final ModelPart subPart = partMappingType.findSubPart( name, null );
			if ( subPart == null ) {
				throw new UnexpectedTokenException(
						"Could not resolve path token : " +
								referencedModelPart + " -> " + name
				);
			}

			return new SubDomainPath( this, subPart );
		}

		throw new UnexpectedTokenException(
				"Domain path of type `" +  referencedModelPart.getPartMappingType() +
						"` -> `" + name + "`"
		);
	}
}
