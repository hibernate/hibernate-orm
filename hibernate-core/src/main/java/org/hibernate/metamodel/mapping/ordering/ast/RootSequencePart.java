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
 * PathPart implementation used to translate the root of a path
 *
 * @author Steve Ebersole
 */
public class RootSequencePart implements SequencePart {
	private final PluralAttributeMapping pluralAttributeMapping;

	public RootSequencePart(PluralAttributeMapping pluralAttributeMapping) {
		this.pluralAttributeMapping = pluralAttributeMapping;
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			boolean isTerminal,
			TranslationContext translationContext) {
		// could be a column-reference (isTerminal would have to be true) or a domain-path

		final ModelPart subPart = pluralAttributeMapping.findSubPart( name, null );

		if ( subPart != null ) {
			return new CollectionSubPath( pluralAttributeMapping, subPart );
		}

		if ( isTerminal ) {
			// assume a column-reference
			return new ColumnReference( name );
		}

		throw new UnexpectedTokenException(
				"Could not resolve order-by token : " +
						pluralAttributeMapping.getCollectionDescriptor().getRole() + " -> " + name
		);
	}
}
