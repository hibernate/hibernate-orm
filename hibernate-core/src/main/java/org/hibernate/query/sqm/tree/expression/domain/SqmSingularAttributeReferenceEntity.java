/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEntity extends AbstractSqmSingularAttributeReference implements
		SqmEntityTypedReference {
	public SqmSingularAttributeReferenceEntity(
			SqmNavigableSourceReference domainReferenceBinding,
			SingularPersistentAttributeEntity boundNavigable) {
		super( domainReferenceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceEntity(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public SingularPersistentAttributeEntity getReferencedNavigable() {
		return (SingularPersistentAttributeEntity) super.getReferencedNavigable();
	}

	@Override
	public SqmExpressableTypeEntity getExpressionType() {
		return (SqmExpressableTypeEntity) super.getExpressionType();
	}

	@Override
	public SqmExpressableTypeEntity getInferableType() {
		return getExpressionType();
	}
}
