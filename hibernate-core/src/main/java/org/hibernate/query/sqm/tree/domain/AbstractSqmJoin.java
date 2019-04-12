/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmJoin<O,T> extends AbstractSqmFrom<O,T> implements SqmJoin<O,T> {
	private final SqmJoinType joinType;

	public AbstractSqmJoin(
			NavigablePath navigablePath,
			NavigableContainer referencedNavigable,
			SqmFrom lhs,
			String alias,
			SqmJoinType joinType,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, alias, nodeBuilder );
		this.joinType = joinType;
	}

	@Override
	public SqmJoinType getSqmJoinType() {
		return joinType;
	}
}
