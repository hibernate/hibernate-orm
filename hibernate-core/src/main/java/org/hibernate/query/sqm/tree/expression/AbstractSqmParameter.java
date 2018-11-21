/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * Common support for SqmParameter impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmParameter extends AbstractInferableTypeSqmExpression implements SqmParameter {
	private final boolean canBeMultiValued;

	public AbstractSqmParameter(boolean canBeMultiValued, AllowableParameterType inherentType) {
		super( inherentType );
		this.canBeMultiValued = canBeMultiValued;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	@Override
	public AllowableParameterType getExpressableType() {
		return (AllowableParameterType) super.getExpressableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Supplier<? extends AllowableParameterType> getInferableType() {
		return (Supplier<? extends AllowableParameterType>) super.getInferableType();
	}

	@Override
	public AllowableParameterType getAnticipatedType() {
		return getExpressableType();
	}
}
