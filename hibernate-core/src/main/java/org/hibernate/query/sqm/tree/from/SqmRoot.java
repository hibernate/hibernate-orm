/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;

/**
 * @author Steve Ebersole
 */
public class SqmRoot extends AbstractSqmFrom {
	public SqmRoot(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			EntityTypeImplementor entityReference) {
		super(
				fromElementSpace,
				uid,
				alias,
				new SqmEntityReference( entityReference ),
				entityReference
		);

		getNavigableReference().injectExportedFromElement( this );
	}

	@Override
	public SqmEntityReference getNavigableReference() {
		return (SqmEntityReference) super.getNavigableReference();
	}

	public String getEntityName() {
		return getNavigableReference().getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityTypeImplementor getIntrinsicSubclassEntityMetadata() {
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
}
