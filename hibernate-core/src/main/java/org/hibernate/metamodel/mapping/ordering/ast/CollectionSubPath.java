/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;

/**
 * @author Steve Ebersole
 */
public class CollectionSubPath implements DomainPath {
	private final PluralAttributeMapping pluralAttribute;
	private final ModelPart referenceModelPart;

	public CollectionSubPath(
			PluralAttributeMapping pluralAttribute,
			ModelPart referenceModelPart) {
		this.pluralAttribute = pluralAttribute;
		this.referenceModelPart = referenceModelPart;
	}

	@Override
	public PluralAttributeMapping getPluralAttribute() {
		return pluralAttribute;
	}

	@Override
	public DomainPath getLhs() {
		return null;
	}

	@Override
	public ModelPart getReferenceModelPart() {
		return referenceModelPart;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			boolean isTerminal,
			TranslationContext translationContext) {
		return null;
	}
}
