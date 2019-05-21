/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedCrossJoin<T,S extends T> extends SqmCrossJoin<S> implements SqmTreatedPath<T,S> {
	private final SqmCrossJoin<T> wrappedPath;
	private final EntityTypeDescriptor<S> treatTarget;

	public SqmTreatedCrossJoin(
			SqmCrossJoin<T> wrappedPath,
			String alias,
			EntityTypeDescriptor<S> treatTarget) {
		super(
				(EntityTypeDescriptor) wrappedPath.getReferencedPathSource(),
				alias,
				wrappedPath.getRoot()
		);
		this.wrappedPath = wrappedPath;
		this.treatTarget = treatTarget;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public EntityTypeDescriptor<S> getModel() {
		return getTreatTarget();
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SqmPathSource<?, S> getReferencedPathSource() {
		return (EntityTypeDescriptor<S>) wrappedPath.getReferencedPathSource();
	}
}
