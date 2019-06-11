/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.produce.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedBagJoin<O,T, S extends T> extends SqmBagJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmBagJoin<O, T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	@SuppressWarnings("WeakerAccess")
	public SqmTreatedBagJoin(
			SqmBagJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				(BagPersistentAttribute) wrappedPath.getAttribute(),
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
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		//noinspection unchecked
		return new SqmTreatedBagJoin( wrappedPath, treatTarget, getAlias() );
	}
}
