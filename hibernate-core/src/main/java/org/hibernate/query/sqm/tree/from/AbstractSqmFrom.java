/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

import org.jboss.logging.Logger;

/**
 * Convenience base class for FromElement implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmFrom implements SqmFrom {
	private static final Logger log = Logger.getLogger( AbstractSqmFrom.class );

	private final SqmFromElementSpace fromElementSpace;
	private final String uid;
	private final String alias;
	private final SqmNavigableReference binding;
	private final EntityValuedExpressableType subclassIndicator;

	protected AbstractSqmFrom(
			SqmFromElementSpace fromElementSpace,
			String uid,
			String alias,
			SqmNavigableReference binding,
			EntityValuedExpressableType subclassIndicator) {
		this.fromElementSpace = fromElementSpace;
		this.uid = uid;
		this.alias = alias;
		this.binding = binding;
		this.subclassIndicator = subclassIndicator;
	}

	@Override
	public SqmNavigableReference getBinding() {
		return binding;
	}

//	@Override
//	public SqmNavigableSource getReferencedNavigable() {
//		return binding.getReferencedNavigable();
//	}

	@Override
	public SqmFromElementSpace getContainingSpace() {
		return fromElementSpace;
	}

	@Override
	public String getUniqueIdentifier() {
		return uid;
	}

	@Override
	public String getIdentificationVariable() {
		return alias;
	}

	@Override
	public EntityValuedExpressableType getIntrinsicSubclassIndicator() {
		return subclassIndicator;
	}
}
