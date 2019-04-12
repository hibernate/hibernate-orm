/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedBagJoin<O,T, S extends T> extends SqmBagJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmBagJoin<O, T> wrappedPath;
	private final EntityTypeDescriptor<S> treatTarget;

	@SuppressWarnings("unchecked")
	public SqmTreatedBagJoin(
			SqmBagJoin<O,T> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			String alias) {
		super(
				wrappedPath.getLhs(),
				(BagPersistentAttribute<O, S>) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmBagJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BagPersistentAttribute getReferencedNavigable() {
		return super.getReferencedNavigable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public BagPersistentAttribute getModel() {
		return getReferencedNavigable();
	}
}
