/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

/**
 * @author Steve Ebersole
 */
public class SqmEntityJoin
		extends AbstractSqmJoin
		implements SqmQualifiedJoin {
	private SqmPredicate onClausePredicate;

	public SqmEntityJoin(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			EntityValuedExpressableType joinedEntityDescriptor,
			SqmJoinType joinType) {
		super(
				fromElementSpace,
				uid,
				alias,
				new SqmEntityReference( joinedEntityDescriptor ),
				joinedEntityDescriptor,
				joinType
		);
		getEntityBinding().injectExportedFromElement( this );
	}

	@Override
	public SqmEntityReference getBinding() {
		return getEntityBinding();
	}

	public SqmEntityReference getEntityBinding() {
		return (SqmEntityReference) super.getBinding();
	}

	public String getEntityName() {
		return getEntityBinding().getReferencedNavigable().getEntityName();
	}

	@Override
	public SqmPredicate getOnClausePredicate() {
		return onClausePredicate;
	}

	public void setOnClausePredicate(SqmPredicate predicate) {
		this.onClausePredicate = predicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitQualifiedEntityJoinFromElement( this );
	}
}
