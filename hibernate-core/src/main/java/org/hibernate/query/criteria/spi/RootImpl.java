/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public class RootImpl<T> extends AbstractFrom<T,T> implements RootImplementor<T> {
	private final EntityTypeDescriptor<T> entityTypeDescriptor;

	public RootImpl(
			EntityTypeDescriptor<T> entityTypeDescriptor,
			CriteriaNodeBuilder criteriaBuilder) {
		super( entityTypeDescriptor, null, criteriaBuilder );
		this.entityTypeDescriptor = entityTypeDescriptor;
	}

	public EntityTypeDescriptor<T> getEntityTypeDescriptor() {
		return entityTypeDescriptor;
	}

	@Override
	public EntityTypeDescriptor<T> getManagedType() {
		return getNavigable();
	}

	@Override
	public EntityTypeDescriptor<T> getNavigable() {
		return (EntityTypeDescriptor<T>) super.getNavigable();
	}

	@Override
	public PathSourceImplementor<T> asPathSource(String subPathName) throws PathException {
		return this;
	}

	@Override
	public EntityTypeDescriptor<T> getModel() {
		return getEntityTypeDescriptor();
	}

	@Override
	public RootImplementor<T> correlateTo(JpaSubQuery<T> subquery) {
		return new CorrelationDelegateImpl<T>( this, nodeBuilder() );
	}

	@Override
	public <S extends T> RootImplementor<S> treatAs(Class<S> treatJavaType) throws PathException {
		return (RootImplementor<S>) super.treatAs( treatJavaType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitRoot( this );
	}

	private static class CorrelationDelegateImpl<D>
			extends AbstractCorrelationDelegate<D,D>
			implements RootImplementor<D> {

		public CorrelationDelegateImpl(RootImplementor<D> correlationParent, CriteriaNodeBuilder nodeBuilder) {
			super( correlationParent, nodeBuilder );
		}

		@Override
		public RootImplementor<D> correlateTo(JpaSubQuery<D> subquery) {
			return new CorrelationDelegateImpl<>( this, nodeBuilder() );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R> R accept(CriteriaVisitor visitor) {
			return (R) visitor.visitCorrelationDelegate( this );
		}

		@Override
		public PathSourceImplementor<D> asPathSource(String subPathName) throws PathException {
			return this;
		}

		@Override
		public RootImplementor<D> getCorrelationParent() {
			return (RootImplementor<D>) super.getCorrelationParent();
		}

		@Override
		public EntityTypeDescriptor<D> getModel() {
			return getCorrelationParent().getModel();
		}

		@Override
		public EntityTypeDescriptor<D> getManagedType() {
			return getCorrelationParent().getManagedType();
		}

		@Override
		public <S extends D> RootImplementor<S> treatAs(Class<S> treatJavaType) throws PathException {
			return (RootImplementor<S>) super.treatAs( treatJavaType );
		}
	}
}
