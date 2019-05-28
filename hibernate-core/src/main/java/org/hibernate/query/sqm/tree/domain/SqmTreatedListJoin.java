/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedListJoin<O,T, S extends T> extends SqmListJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmListJoin<O,T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedListJoin(
			SqmListJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				(ListPersistentAttribute) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmListJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public ListPersistentAttribute<O, S> getModel() {
		return (ListPersistentAttribute<O, S>) super.getModel();
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		return getWrappedPath().resolveIndexedAccess( selector, isTerminal, creationState );
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedListJoin( wrappedPath, treatTarget, getAlias() );
	}
}
