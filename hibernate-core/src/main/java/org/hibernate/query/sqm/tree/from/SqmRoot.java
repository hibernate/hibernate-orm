/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom {
	public SqmRoot(
			String uid,
			String alias,
			EntityTypeDescriptor entityTypeDescriptor) {
		super(
				uid,
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
	public EntityTypeDescriptor<?> getReferencedNavigable() {
		return (EntityTypeDescriptor<?>) super.getReferencedNavigable();
	}

	@Override
	public EntityTypeDescriptor getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends EntityTypeDescriptor> getInferableType() {
		return this::getReferencedNavigable;
	}

	public String getEntityName() {
		return getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityTypeDescriptor<E> getIntrinsicSubclassEntityMetadata() {
		// a root FromElement cannot indicate a subclass intrinsically (as part of its declaration)
		return null;
	}

	@Override
	public EntityJavaDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public String toString() {
		return getEntityName() + " as " + getIdentificationVariable();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitRootEntityFromElement( this );
	}
}
