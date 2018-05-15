/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmCollectionElementReferenceEmbedded
		extends AbstractSqmCollectionElementReference
		implements SqmCollectionElementReference, SqmEmbeddableTypedReference {
	public SqmCollectionElementReferenceEmbedded(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionElementEmbedded getReferencedNavigable() {
		return (CollectionElementEmbedded) super.getReferencedNavigable();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return getPluralAttributeReference().getExportedFromElement();
	}
}
