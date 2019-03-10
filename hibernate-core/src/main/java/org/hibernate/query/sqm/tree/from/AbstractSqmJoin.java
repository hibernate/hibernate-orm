/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.SqmJoinType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmJoin extends AbstractSqmFrom implements SqmJoin {
	private final SqmJoinType joinType;

	public AbstractSqmJoin(
			String uid,
			NavigablePath navigablePath,
			NavigableContainer referencedNavigable,
			String alias,
			SqmJoinType joinType) {
		super( uid, navigablePath, referencedNavigable, alias );
		this.joinType = joinType;
	}

	@Override
	public SqmJoinType getJoinType() {
		return joinType;
	}
}
