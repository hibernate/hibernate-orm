/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;

/**
 * Common support for SqmParameter impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmParameter extends AbstractSqmExpression implements SqmParameter {
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
	public AllowableParameterType<?> getExpressableType() {
		return (AllowableParameterType) super.getExpressableType();
	}

	@Override
	public AllowableParameterType getAnticipatedType() {
		return getExpressableType();
	}
}
