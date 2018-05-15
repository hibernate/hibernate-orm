/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmCrossJoin extends AbstractSqmFrom implements SqmJoin {
	private final SqmEntityReference joinedEntiytReference;

	public SqmCrossJoin(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			EntityTypeDescriptor entityDescriptor,
			SqmCreationContext creationContext) {
		super(
				fromElementSpace,
				uid,
				alias
		);
		this.joinedEntiytReference = new SqmEntityReference( entityDescriptor, this, creationContext );
	}

	@Override
	public SqmEntityReference getNavigableReference() {
		return joinedEntiytReference;
	}

	public String getEntityName() {
		return getNavigableReference().getReferencedNavigable().getEntityName();
	}

	@Override
	public SqmJoinType getJoinType() {
		return SqmJoinType.CROSS;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitCrossJoinedFromElement( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getNavigableReference().getJavaTypeDescriptor();
	}
}
