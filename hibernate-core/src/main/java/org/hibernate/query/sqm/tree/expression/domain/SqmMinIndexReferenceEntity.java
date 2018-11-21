/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmMinIndexReferenceEntity
		extends AbstractSpecificSqmCollectionIndexReference
		implements SqmMinIndexReference, SqmEntityTypedReference {
	private SqmFrom exportedFromElement;

	public SqmMinIndexReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public EntityValuedNavigable getReferencedNavigable() {
		return (EntityValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public EntityValuedNavigable getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends CollectionIndexEntity> getInferableType() {
		return (Supplier<? extends CollectionIndexEntity>) super.getInferableType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}
}
