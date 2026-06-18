/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;
import org.hibernate.type.descriptor.java.JavaType;


import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;

/**
 * @author Steve Ebersole
 */
public class SqmFieldLiteral<T> extends AbstractSqmExpression<T>
		implements SqmExpression<T>, SqmBindableType<T>, SqmSelectableNode<T>, SemanticPathPart {
	private final @Nullable T value;
	private final JavaType<T> fieldJavaType;
	private final String fieldName;

	public SqmFieldLiteral(
			@Nullable T value,
			JavaType<T> fieldJavaType,
			String fieldName,
			NodeBuilder nodeBuilder) {
		super( null, nodeBuilder );
		this.value = value;
		this.fieldJavaType = fieldJavaType;
		this.fieldName = fieldName;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	public SqmFieldLiteral<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmFieldLiteral<>(
						value,
						fieldJavaType,
						fieldName,
						nodeBuilder()
				)
		);
	}

	public @Nullable T getValue() {
		return value;
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public @Nonnull SqmBindableType<T> getNodeType() {
		return this;
	}

	@Override
	public void applyInferableType(@Nullable SqmBindableType<?> type) {
	}

	@Override
	public @Nonnull JavaType<T> getExpressibleJavaType() {
		return fieldJavaType;
	}

	@Override
	public @Nonnull JavaType<T> getJavaTypeDescriptor() {
		return getExpressibleJavaType();
	}

	@Override
	public @Nonnull Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFieldLiteral( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		SqmLiteral.appendHqlString( hql, getJavaTypeDescriptor(), getValue() );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldJavaType.getTypeName(),
						fieldName
				)
		);
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						Locale.ROOT,
						"Static field reference [%s#%s] cannot be de-referenced",
						fieldJavaType.getTypeName(),
						fieldName
				)
		);
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return null;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmFieldLiteral<?> that
			&& Objects.equals( value, that.value );
	}

	@Override
	public int hashCode() {
		return Objects.hash( value );
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
