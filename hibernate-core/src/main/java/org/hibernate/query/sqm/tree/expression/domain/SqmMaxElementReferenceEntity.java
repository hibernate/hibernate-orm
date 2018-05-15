/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMaxElementReferenceEntity
		extends AbstractSpecificSqmElementReference
		implements SqmRestrictedCollectionElementReferenceEntity, SqmMaxElementReference {
	private SqmFrom exportedFromElement;

	public SqmMaxElementReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionElementEntity getReferencedNavigable() {
		return (CollectionElementEntity) super.getReferencedNavigable();
	}

	@Override
	public CollectionElementEntity getExpressableType() {
		return (CollectionElementEntity) getPluralAttributeReference().getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public CollectionElementEntity getInferableType() {
		return getExpressableType();
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
