/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.SetPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSetJoin<O,T, S extends T> extends SqmSetJoin<O,S> implements SqmTreatedPath<T,S> {
	private final SqmSetJoin<O,T> wrappedPath;
	private final EntityDomainType<S> treatTarget;

	@SuppressWarnings("WeakerAccess")
	public SqmTreatedSetJoin(
			SqmSetJoin<O,T> wrappedPath,
			EntityDomainType<S> treatTarget,
			String alias) {
		//noinspection unchecked
		super(
				wrappedPath.getLhs(),
				(SetPersistentAttribute) wrappedPath.getAttribute(),
				alias,
				wrappedPath.getSqmJoinType(),
				wrappedPath.isFetched(),
				wrappedPath.nodeBuilder()
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public SqmSetJoin<O,T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public SetPersistentAttribute<O,S> getModel() {
		return super.getModel();
	}

	@Override
	public EntityDomainType<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SetPersistentAttribute<O,S> getReferencedPathSource() {
		return super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<S> getJavaTypeDescriptor() {
		//noinspection unchecked
		return (JavaTypeDescriptor) wrappedPath.getJavaTypeDescriptor();
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		//noinspection unchecked
		return new SqmTreatedSetJoin( wrappedPath, treatTarget, getAlias() );
	}
}
