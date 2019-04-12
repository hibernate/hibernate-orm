/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmCorrelatedFrom<O,T>
		extends AbstractSqmFrom<O,T>
		implements SqmPathWrapper<T,T>, SqmFrom<O,T> {
	private SqmFrom<O,T> correlationParent;

	public AbstractSqmCorrelatedFrom(
			SqmFrom<O,T> correlationParent,
			NodeBuilder criteriaBuilder) {
		super(
				correlationParent.getNavigablePath(),
				correlationParent.getReferencedNavigable(),
				(SqmFrom) correlationParent.getLhs(),
				null,
				criteriaBuilder
		);
		this.correlationParent = correlationParent;
	}

	@Override
	public SqmFrom<O, T> getCorrelationParent() {
		return correlationParent;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return getCorrelationParent();
	}

	@Override
	public boolean isCorrelated() {
		return true;
	}

	@Override
	public NavigableContainer<T> getReferencedNavigable() {
		return correlationParent.getReferencedNavigable();
	}


}
