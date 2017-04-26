/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmJoin extends AbstractSqmFrom implements SqmJoin {
	private final SqmJoinType joinType;

	public AbstractSqmJoin(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			SqmNavigableReference navigableBinding,
			EntityValuedExpressableType intrinsicSubclassIndicator,
			SqmJoinType joinType) {
		super( fromElementSpace, uid, alias, navigableBinding, intrinsicSubclassIndicator );
		this.joinType = joinType;
	}

	@Override
	public SqmJoinType getJoinType() {
		return joinType;
	}
}
