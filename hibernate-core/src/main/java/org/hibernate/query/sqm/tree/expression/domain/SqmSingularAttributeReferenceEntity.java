/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEntity extends AbstractSqmSingularAttributeReference implements
		SqmEntityTypedReference {
	public SqmSingularAttributeReferenceEntity(
			SqmNavigableContainerReference domainReferenceBinding,
			SingularPersistentAttributeEntity boundNavigable) {
		super( domainReferenceBinding, boundNavigable );
	}

	public SqmSingularAttributeReferenceEntity(SqmAttributeJoin fromElement) {
		super( fromElement );
	}

	@Override
	public SingularPersistentAttributeEntity getReferencedNavigable() {
		return (SingularPersistentAttributeEntity) super.getExpressionType();
	}

	@Override
	public EntityValuedExpressableType getExpressionType() {
		return (EntityValuedExpressableType) super.getExpressionType();
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityValuedSingularAttribute( this );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return super.getExpressionType().getPersistenceType();
	}
}
