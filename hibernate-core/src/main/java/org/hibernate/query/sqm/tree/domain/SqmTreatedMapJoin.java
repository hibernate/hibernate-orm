/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedMapJoin<O,K,V, S extends V> extends SqmMapJoin<O,K,S> implements SqmTreatedPath<V,S> {
	private final SqmMapJoin<O,K,V> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	public SqmTreatedMapJoin(
			SqmMapJoin<O,K,V> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				( (SqmMapJoin) wrappedPath ).getModel(),
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
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public JavaTypeDescriptor<S> getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		return new SqmTreatedMapJoin(
				wrappedPath,
				treatTarget,
				getAlias()
		);
	}
}
