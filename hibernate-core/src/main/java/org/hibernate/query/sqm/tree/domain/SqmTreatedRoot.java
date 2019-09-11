/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedRoot<T, S extends T> extends SqmRoot<S> implements SqmTreatedPath<T,S> {
	private final SqmRoot<T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedRoot(
			SqmRoot<T> wrappedPath,
			EntityDomainType<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				(EntityDomainType) wrappedPath.getReferencedPathSource(),
				null,
				nodeBuilder
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityDomainType<S> getManagedType() {
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

	@Override
	public SqmPath getLhs() {
		return wrappedPath.getLhs();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}
}
