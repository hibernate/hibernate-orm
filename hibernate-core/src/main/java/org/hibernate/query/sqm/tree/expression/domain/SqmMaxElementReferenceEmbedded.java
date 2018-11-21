/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMaxElementReferenceEmbedded
		extends AbstractSpecificSqmElementReference
		implements SqmRestrictedCollectionElementReferenceEmbedded, SqmMaxElementReference {

	private SqmFrom exportedFromElement;

	public SqmMaxElementReferenceEmbedded(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionElementEmbedded getReferencedNavigable() {
		return (CollectionElementEmbedded) super.getReferencedNavigable();
	}

	@Override
	public CollectionElementEmbedded getExpressableType() {
		return (CollectionElementEmbedded) super.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends CollectionElementEmbedded> getInferableType() {
		return (Supplier<? extends CollectionElementEmbedded>) super.getInferableType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}
}
