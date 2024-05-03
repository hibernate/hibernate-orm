/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.BindableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common support for SqmParameter impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmParameter<T> extends AbstractSqmExpression<T> implements SqmParameter<T> {
	private boolean canBeMultiValued;

	public AbstractSqmParameter(
			boolean canBeMultiValued,
			SqmExpressible<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.canBeMultiValued = canBeMultiValued;
	}

	@Override
	public void applyInferableType(@Nullable SqmExpressible<?> type) {
		if ( type != null ) {
			if ( type instanceof PluralPersistentAttribute ) {
				final PluralPersistentAttribute<?, ?, ?> pluralPersistentAttribute =
						(PluralPersistentAttribute<?, ?, ?>) type;
				internalApplyInferableType( pluralPersistentAttribute.getElementType() );
			}
			else {
				internalApplyInferableType( type );
			}
		}
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

	public void disallowMultiValuedBinding() {
		this.canBeMultiValued = false;
	}

	@Override
	public BindableType<T> getAnticipatedType() {
		return this.getNodeType();
	}

	@Override
	public Class<T> getParameterType() {
		return this.getNodeType().getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public Integer getTupleLength() {
		return null;
	}
}
