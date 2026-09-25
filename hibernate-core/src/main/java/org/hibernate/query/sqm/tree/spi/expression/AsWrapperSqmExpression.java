/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.type.BasicType;

import java.util.Objects;


public class AsWrapperSqmExpression<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> expression;

	AsWrapperSqmExpression(@Nullable SqmBindableType<T> type, @Nonnull Class<T> clazz, @Nonnull SqmExpression<?> expression) {
		super( null, expression.nodeBuilder() );
		this.expression = expression;
		setExpressibleType(
				type,
				type == null
						? nodeBuilder().getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( clazz )
						: null
		);
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitAsWrapperExpression( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "wrap(" );
		expression.appendHqlString( hql, context );
		hql.append( " as " );
		final BasicType<T> nodeType = getNodeType();
		if ( nodeType == null ) {
			hql.append( getJavaType().getCanonicalName() );
		}
		else {
			hql.append( nodeType.getReturnedClassName() );
		}
		hql.append( ")" );
	}

	@Nonnull
	@Override
	public <X> SqmExpression<X> as(@Nonnull Class<X> type) {
		return expression.as( type );
	}

	@Nonnull
	@Override
	public SqmExpression<T> copy(@Nonnull SqmCopyContext context) {
		return new AsWrapperSqmExpression<>( getNodeType(), getJavaType(), expression.copy( context ) );
	}

	@Nonnull
	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public @Nullable BasicType<T> getNodeType() {
		return (BasicType<T>) super.getNodeType();
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof AsWrapperSqmExpression<?> that
			&& this.expression.equals( that.expression )
			&& Objects.equals( this.getNodeType(), that.getNodeType() )
			&& Objects.equals( this.getJavaTypeDescriptor(), that.getJavaTypeDescriptor() );
	}

	@Override
	public int hashCode() {
		return expression.hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof AsWrapperSqmExpression<?> that
			&& this.expression.isCompatible( that.expression )
			&& Objects.equals( this.getNodeType(), that.getNodeType() )
			&& Objects.equals( this.getJavaTypeDescriptor(), that.getJavaTypeDescriptor() );
	}

	@Override
	public int cacheHashCode() {
		return expression.cacheHashCode();
	}
}
