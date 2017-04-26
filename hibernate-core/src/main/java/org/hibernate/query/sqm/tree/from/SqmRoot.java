/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
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
			EntityValuedExpressableType entityReference) {
		super(
				fromElementSpace,
				uid,
				alias,
				new SqmEntityReference( entityReference ),
				entityReference
		);

		getBinding().injectExportedFromElement( this );
	}

	@Override
	public SqmEntityReference getBinding() {
		return (SqmEntityReference) super.getBinding();
	}

	public String getEntityName() {
		return getBinding().getReferencedNavigable().getEntityName();
	}

	@Override
	public EntityValuedExpressableType getIntrinsicSubclassIndicator() {
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
