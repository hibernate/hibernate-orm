/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;

/**
 * SequencePart implementation used to translate the root of a path
 *
 * @see PluralAttributePath
 *
 * @author Steve Ebersole
 */
public class RootSequencePart implements SequencePart {
	private final PluralAttributePath pluralAttributePath;

	public RootSequencePart(PluralAttributeMapping pluralAttributeMapping) {
		this.pluralAttributePath = new PluralAttributePath( pluralAttributeMapping );
	}

	@Override
	public SequencePart resolvePathPart(
			String name,
			String identifier,
			boolean isTerminal,
			TranslationContext translationContext) {
		// could be a column-reference (isTerminal would have to be true) or a domain-path

		final DomainPath subDomainPath = pluralAttributePath.resolvePathPart(
				name,
				identifier,
				isTerminal,
				translationContext
		);
		if ( subDomainPath != null ) {
			return subDomainPath;
		}

		if ( isTerminal ) {
			// assume a column-reference
			return new ColumnReference(
					translationContext.getFactory()
							.getJdbcServices()
							.getDialect()
							.quote( identifier ),
					false
			);
		}

		throw new PathResolutionException(
				"Could not resolve order-by path : " +
						pluralAttributePath.getReferenceModelPart().getCollectionDescriptor().getRole() + " -> " + name
		);
	}
}
