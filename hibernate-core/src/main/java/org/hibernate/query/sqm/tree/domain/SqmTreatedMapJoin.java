/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.MapPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedMapJoin<O,K,V, S extends V> extends SqmMapJoin<O,K,S> implements SqmTreatedPath<V,S> {
	private final SqmMapJoin<O,K,V> wrappedPath;
	private final EntityTypeDescriptor<S> treatTarget;

	public SqmTreatedMapJoin(
			SqmMapJoin<O,K,V> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				(MapPersistentAttribute) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmMapJoin<O,K,V> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPathSource<?, S> getReferencedPathSource() {
		return super.getReferencedPathSource();
	}
}
