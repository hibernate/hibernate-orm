/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmDmlStatement<E>
		extends AbstractSqmStatement<E>
		implements SqmDmlStatement<E> {
	private SqmRoot<E> target;

	public AbstractSqmDmlStatement(NodeBuilder nodeBuilder) {
		super( nodeBuilder );
	}

	public AbstractSqmDmlStatement(SqmRoot<E> target, NodeBuilder nodeBuilder) {
		this( nodeBuilder );
		this.target = target;
	}

	@Override
	public SqmRoot<E> getTarget() {
		return target;
	}

	@Override
	public void setTarget(SqmRoot<E> root) {
		this.target = root;
	}

	@Override
	public <U> SqmSubQuery<U> subquery(Class<U> type) {
		return new SqmSubQuery<>( this, type, nodeBuilder() );
	}
}
