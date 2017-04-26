/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEmbedded;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeEmbeddable;
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
	public Navigable getReferencedNavigable() {
		return (SqmExpressableTypeEmbedded) super.getReferencedNavigable();
	}

	@Override
	public SqmDomainTypeEmbeddable getExportedDomainType() {
		return (SqmDomainTypeEmbeddable) super.getExportedDomainType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return getPluralAttributeBinding().getExportedFromElement();
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		throw new NotYetImplementedException( "Cannot inject SqmFrom element into a CompositeBinding" );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}
}
