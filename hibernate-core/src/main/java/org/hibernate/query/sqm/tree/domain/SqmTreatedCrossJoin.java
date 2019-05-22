/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedCrossJoin<T,S extends T> extends SqmCrossJoin<S> implements SqmTreatedPath<T,S> {
	private final SqmCrossJoin<T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedCrossJoin(
			SqmCrossJoin<T> wrappedPath,
			String alias,
			EntityDomainType<S> treatTarget) {
		//noinspection unchecked
		super(
				(EntityDomainType) wrappedPath.getReferencedPathSource().getSqmPathType(),
				alias,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getModel() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityDomainType<S> getReferencedPathSource() {
		//noinspection unchecked
		return (EntityDomainType) wrappedPath.getReferencedPathSource();
	}
}
