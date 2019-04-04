/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom {
	public SqmRoot(EntityTypeDescriptor entityTypeDescriptor, String alias) {
		super(
				alias == null
						? new NavigablePath( entityTypeDescriptor.getEntityName() )
						: new NavigablePath( entityTypeDescriptor.getEntityName() + '(' + alias + ')' ),
				entityTypeDescriptor,
				alias
		);
	}

	@Override
	public SqmPath getLhs() {
		// a root has no LHS
		return null;
	}

	@Override
	public SqmRoot findRoot() {
		return this;
	}

	@Override
	public EntityTypeDescriptor<?> getReferencedNavigable() {
		return (EntityTypeDescriptor<?>) super.getReferencedNavigable();
	}

	public String getEntityName() {
		return getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public String toString() {
		return getExplicitAlias() == null
				? getEntityName()
				: getEntityName() + " as " + getExplicitAlias();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitRootPath( this );
	}
}
