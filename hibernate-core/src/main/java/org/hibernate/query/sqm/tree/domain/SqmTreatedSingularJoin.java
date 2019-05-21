/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.mapping.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSingularJoin<O,T, S extends T> extends SqmSingularJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmSingularJoin<O,T> wrappedPath;
	private final EntityTypeDescriptor<S> treatTarget;


	public SqmTreatedSingularJoin(
			SqmSingularJoin<O,T> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				(SingularPersistentAttributeEntity) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmSingularJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPathSource<?, S> getReferencedPathSource() {
		return (SingularPersistentAttributeEntity) super.getReferencedPathSource();
	}
}
