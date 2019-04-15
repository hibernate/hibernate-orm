/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedRoot<T, S extends T> extends SqmRoot<S> implements SqmTreatedPath<T,S> {
	private final SqmRoot<T> wrappedPath;
	private final EntityTypeDescriptor<S> treatTarget;

	public SqmTreatedRoot(
			SqmRoot<T> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				( (EntityValuedNavigable) wrappedPath.getReferencedNavigable() ).getEntityDescriptor(),
				null,
				nodeBuilder
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityTypeDescriptor<S> getManagedType() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityTypeDescriptor<S> getReferencedNavigable() {
		return (EntityTypeDescriptor<S>) wrappedPath.getReferencedNavigable();
	}

	@Override
	public SqmPath getLhs() {
		return wrappedPath.getLhs();
	}

}
