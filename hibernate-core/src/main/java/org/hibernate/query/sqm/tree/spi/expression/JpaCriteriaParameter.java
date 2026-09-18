/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.procedure.spi.NamedCallableQueryMemento;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.type.BindableType;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

import java.util.Objects;

/**
 * {@link JpaParameterExpression} created via JPA {@link jakarta.persistence.criteria.CriteriaBuilder}.
 * <p>
 * Each occurrence of a {@code JpaParameterExpression} results in a unique {@link SqmParameter}.
 *
 * @see ParameterMetadata
 * @see NodeBuilder#parameter
 *
 * @author Steve Ebersole
 */
public class JpaCriteriaParameter<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T>, QueryParameterImplementor<T> {

	private final @Nullable String name;
	private final @Nullable Class<T> declaredJavaType;
	private boolean allowsMultiValuedBinding;

	public JpaCriteriaParameter(
			@Nullable String name,
			@Nullable BindableType<? super T> type,
			boolean allowsMultiValuedBinding,
			@Nonnull NodeBuilder nodeBuilder) {
		this( name, type, null, allowsMultiValuedBinding, nodeBuilder );
	}

	public JpaCriteriaParameter(
			@Nullable String name,
			@Nullable BindableType<? super T> type,
			@Nullable Class<T> declaredJavaType,
			boolean allowsMultiValuedBinding,
			@Nonnull NodeBuilder nodeBuilder) {
		super( nodeBuilder.resolveExpressible( type ), nodeBuilder );
		this.name = name;
		this.declaredJavaType = declaredJavaType;
		this.allowsMultiValuedBinding = allowsMultiValuedBinding;
	}

	protected JpaCriteriaParameter(@Nonnull JpaCriteriaParameter<T> original) {
		super( original.getNodeType(), original.nodeBuilder() );
		this.name = original.name;
		this.declaredJavaType = original.declaredJavaType;
		this.allowsMultiValuedBinding = original.allowsMultiValuedBinding;
	}

	@Nonnull
	@Override
	public JpaCriteriaParameter<T> copy(@Nonnull SqmCopyContext context) {
		// Don't create a copy of regular parameters because identity is important here
		return this;
	}

	@Override
	public @Nullable String getName() {
		return name;
	}

	public @Nullable T getValue() {
		return null;
	}

	@Override
	public @Nullable Integer getPosition() {
		// for criteria anyway, these cannot be positional
		return null;
	}

	@Override
	public @Nullable Integer getTupleLength() {
		// TODO: we should be able to do much better than this!
		return null;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		return allowsMultiValuedBinding;
	}

	@Override
	public void disallowMultiValuedBinding() {
		allowsMultiValuedBinding = false;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return allowsMultiValuedBinding();
	}

	@Override
	public @Nullable BindableType<T> getAnticipatedType() {
		return getHibernateType();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void applyAnticipatedType(@Nullable BindableType type) {
		super.internalApplyInferableType( nodeBuilder().resolveExpressible( type ) );
	}

	@Nonnull
	@Override
	public SqmParameter<T> copy() {
		return new JpaCriteriaParameter<>( this );
	}

	@Override
	public @Nullable BindableType<T> getHibernateType() {
		return getNodeType();
	}

	@Override
	public @Nonnull Class<T> getParameterType() {
		final var javaType = getJavaTypeIfKnown();
		if ( javaType == null ) {
			throw new IllegalStateException( "Could not determine the Java type of Criteria parameter"
					+ (name == null ? "" : " '" + name + "'") );
		}
		return javaType;
	}

	@Override
	public @Nullable Class<T> getJavaTypeIfKnown() {
		if ( declaredJavaType != null ) {
			return declaredJavaType;
		}
		else {
			final var nodeType = getNodeType();
			final var javaType = nodeType == null ? null : nodeType.getExpressibleJavaType();
			return javaType == null ? null : javaType.getJavaTypeClass();
		}
	}

	@Override
	protected void internalApplyInferableType(@Nullable SqmBindableType<?> newType) {
		super.internalApplyInferableType( newType );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitJpaCriteriaParameter( this );
	}

	@Override
	@Nonnull
	public NamedCallableQueryMemento.ParameterMemento toMemento() {
		throw new UnsupportedOperationException( "ParameterMemento cannot be extracted from Criteria query parameter" );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( ':' ).append( name( context ) );
	}

	@Nonnull
	private String name(@Nonnull SqmRenderContext context) {
		return name == null ? context.resolveParameterName( this ) : name;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return this == object
			|| object instanceof JpaCriteriaParameter<?> that
				&& name != null
				&& Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return name == null ? super.hashCode() : name.hashCode();
	}

	// For caching, we can consider two parameters to be compatible if they are unnamed, or they have the same name

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object != null && getClass() == object.getClass()
			&& Objects.equals( name, ((JpaCriteriaParameter<?>) object).name );
	}

	@Override
	public int cacheHashCode() {
		return name == null ? 0 : name.hashCode();
	}
}
