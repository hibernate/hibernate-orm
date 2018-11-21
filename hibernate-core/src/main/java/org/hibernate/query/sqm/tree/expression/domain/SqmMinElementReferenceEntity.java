/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMinElementReferenceEntity
		extends AbstractSpecificSqmElementReference
		implements SqmMinElementReference, SqmEntityTypedReference {
	private SqmFrom exportedFromElement;

	public SqmMinElementReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionElementEntity getExpressableType() {
		return (CollectionElementEntity) super.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends CollectionElementEntity> getInferableType() {
		return (Supplier<? extends CollectionElementEntity>) super.getInferableType();
	}

	@Override
	public CollectionElementEntity getReferencedNavigable() {
		return (CollectionElementEntity) getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeElementBinding( this );
	}
}
