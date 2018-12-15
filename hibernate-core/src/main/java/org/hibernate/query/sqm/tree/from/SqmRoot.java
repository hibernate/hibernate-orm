/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom {
	private final SqmEntityReference entityReference;

	public SqmRoot(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			EntityValuedExpressableType<E> entityReference) {
		super( fromElementSpace, uid, alias );
		this.entityReference = new SqmEntityReference( entityReference.getEntityDescriptor(), this );
	}

	@Override
	public SqmEntityReference getNavigableReference() {
		return entityReference;
	}

	public String getEntityName() {
		return getNavigableReference().getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityTypeDescriptor<E> getIntrinsicSubclassEntityMetadata() {
		// a root FromElement cannot indicate a subclass intrinsically (as part of its declaration)
		return null;
	}

	@Override
	public String toString() {
		return getEntityName() + " as " + getIdentificationVariable();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitRootEntityFromElement( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getNavigableReference().getJavaTypeDescriptor();
	}
}
