/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionIndexEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMaxIndexReferenceEmbedded
		extends AbstractSpecificSqmCollectionIndexReference
		implements SqmMaxIndexReference, SqmEmbeddableTypedReference {
	private SqmFrom exportedFromElement;

	public SqmMaxIndexReferenceEmbedded(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public EmbeddedValuedNavigable getReferencedNavigable() {
		return (EmbeddedValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends CollectionIndexEmbedded> getInferableType() {
		return (Supplier<? extends CollectionIndexEmbedded>) super.getInferableType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}
}
