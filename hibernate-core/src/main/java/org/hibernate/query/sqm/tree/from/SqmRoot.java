/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedRoot;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom<E,E> implements JpaRoot<E> {
	public SqmRoot(
			EntityTypeDescriptor<E> entityTypeDescriptor,
			String alias,
			NodeBuilder nodeBuilder) {
		super( entityTypeDescriptor, alias, nodeBuilder );
	}

	@Override
	public SqmPath<?> getLhs() {
		// a root has no LHS
		return null;
	}

	@Override
	public SqmRoot findRoot() {
		return this;
	}

	@Override
	public EntityTypeDescriptor<E> getReferencedNavigable() {
		return (EntityTypeDescriptor<E>) super.getReferencedNavigable();
	}

	public String getEntityName() {
		return getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityJavaDescriptor<E> getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public String toString() {
		return getExplicitAlias() == null
				? getEntityName()
				: getEntityName() + " as " + getExplicitAlias();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootPath( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public EntityTypeDescriptor<E> getManagedType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityTypeDescriptor<E> getModel() {
		return getReferencedNavigable();
	}

	@Override
	public <S extends E> SqmTreatedRoot<E, S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityTypeDescriptor<S> typeDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedRoot<>( this, typeDescriptor, nodeBuilder() );
	}
}
