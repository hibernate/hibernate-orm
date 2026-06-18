/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.type.BindableType;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmExpressible;

import jakarta.annotation.Nullable;

/**
 * Common support for SqmParameter impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmParameter<T> extends AbstractSqmExpression<T> implements SqmParameter<T> {
	private boolean canBeMultiValued;

	public AbstractSqmParameter(
			boolean canBeMultiValued,
			@Nullable SqmBindableType<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.canBeMultiValued = canBeMultiValued;
	}

	@Override
	public void applyInferableType(@Nullable SqmBindableType<?> type) {
		if ( type != null ) {
			final SqmPathSource<?> pathSource;
			if ( type instanceof PluralPersistentAttribute<?, ?, ?> pluralPersistentAttribute ) {
				internalApplyInferableType( (SqmBindableType<?>) pluralPersistentAttribute.getElementType() );
			}
			else if ( type instanceof AnonymousTupleType<?> tupleType
				&& (pathSource = tupleType.findSubPathSource( CollectionPart.Nature.ELEMENT.getName() ) ) != null ) {
				internalApplyInferableType( pathSource.getExpressible() );
			}
			else {
				internalApplyInferableType( type );
			}
		}
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	@Override
	public @Nullable Integer getPosition() {
		return null;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	public void disallowMultiValuedBinding() {
		canBeMultiValued = false;
	}

	@Override
	public @Nullable BindableType<T> getAnticipatedType() {
		return getNodeType();
	}

	@Override
	public @Nullable Class<T> getParameterType() {
		final SqmExpressible<T> nodeType = getNodeType();
		return nodeType == null ? null : nodeType.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public @Nullable Integer getTupleLength() {
		return null;
	}
}
