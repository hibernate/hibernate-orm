/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedEntityJoin<T, S extends T> extends SqmEntityJoin<S> implements SqmTreatedPath<T,S> {
	private final SqmEntityJoin<T> wrapped;
	private final EntityTypeDescriptor<S> treatTarget;

	public SqmTreatedEntityJoin(
			SqmEntityJoin<T> wrapped,
			EntityTypeDescriptor<S> treatTarget,
			String alias,
			SqmJoinType joinType) {
		super(
				treatTarget,
				alias,
				joinType,
				wrapped.getRoot()
		);
		this.wrapped = wrapped;
		this.treatTarget = treatTarget;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrapped;
	}

	@Override
	public EntityTypeDescriptor<S> getReferencedNavigable() {
		return super.getReferencedNavigable();
	}
}
