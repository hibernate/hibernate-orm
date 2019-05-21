/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.EntityValuedNavigable;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class SqmTreatedSimplePath<T, S extends T>
		extends SqmEntityValuedSimplePath<S>
		implements SqmNavigableReference<S>, SqmTreatedPath<T,S> {

	private final EntityTypeDescriptor<S> treatTarget;
	private final SqmPath<T> wrappedPath;

	public SqmTreatedSimplePath(
			SqmEntityValuedSimplePath<T> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			NodeBuilder nodeBuilder) {
		//noinspection unchecked
		super(
				wrappedPath.getNavigablePath(),
				(EntityValuedNavigable<S>) wrappedPath.getReferencedPathSource(),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@SuppressWarnings("unchecked")
	public SqmTreatedSimplePath(
			SqmPluralValuedSimplePath<T> wrappedPath,
			EntityTypeDescriptor<S> treatTarget,
			NodeBuilder nodeBuilder) {
		super(
				wrappedPath.getNavigablePath(),
				wrappedPath.sqmAs( EntityValuedNavigable.class ),
				wrappedPath.getLhs(),
				nodeBuilder
		);
		this.treatTarget = treatTarget;
		this.wrappedPath = wrappedPath;
	}

	@Override
	public EntityTypeDescriptor<S> getTreatTarget() {
		return treatTarget;
	}

	@Override
	public SqmPath<T> getWrappedPath() {
		return wrappedPath;
	}

	@Override
	public <S1 extends S> SqmTreatedSimplePath<S,S1> treatAs(Class<S1> treatJavaType) throws PathException {
		return (SqmTreatedSimplePath<S, S1>) super.treatAs( treatJavaType );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitTreatedPath( this );
	}
}
